(function () {
    "use strict";

    let conversationId = null;

    const mesajlarEl = document.getElementById("mesajlar");
    const formEl = document.getElementById("mesajForm");
    const inputEl = document.getElementById("mesajInput");
    const gonderButonEl = document.getElementById("gonderButon");
    const yeniKonusmaEl = document.getElementById("yeniKonusma");

    function baloEkle(kimden) {
        const balo = document.createElement("div");
        balo.className = kimden === "kullanici"
            ? "ml-auto max-w-[75%] bg-slate-800 text-white rounded-lg px-3 py-2 whitespace-pre-wrap"
            : "mr-auto max-w-[75%] bg-white border border-slate-200 rounded-lg px-3 py-2 whitespace-pre-wrap";
        mesajlarEl.appendChild(balo);
        mesajlarEl.scrollTop = mesajlarEl.scrollHeight;
        return balo;
    }

    function sseOlaylariniIsle(metin, tamponRef, handler) {
        tamponRef.deger += metin;
        const parcalar = tamponRef.deger.split("\n\n");
        tamponRef.deger = parcalar.pop();

        for (const parca of parcalar) {
            if (!parca.trim()) continue;
            let olayAdi = "message";
            const dataSatirlari = [];
            for (const satir of parca.split("\n")) {
                if (satir.startsWith("event:")) {
                    olayAdi = satir.slice(6).trim();
                } else if (satir.startsWith("data:")) {
                    dataSatirlari.push(satir.slice(5));
                }
            }
            handler(olayAdi, dataSatirlari.join("\n"));
        }
    }

    async function mesajGonder(mesaj) {
        baloEkle("kullanici").textContent = mesaj;
        const asistanBalo = baloEkle("asistan");
        asistanBalo.textContent = "";

        gonderButonEl.disabled = true;

        try {
            const yanit = await fetch("/api/chat/stream", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ conversationId, mesaj })
            });

            if (!yanit.ok || !yanit.body) {
                asistanBalo.textContent = "Bir hata oluştu, lütfen tekrar deneyin.";
                return;
            }

            const okuyucu = yanit.body.getReader();
            const cozucu = new TextDecoder("utf-8");
            const tamponRef = { deger: "" };

            while (true) {
                const { value, done } = await okuyucu.read();
                if (done) break;

                sseOlaylariniIsle(cozucu.decode(value, { stream: true }), tamponRef, (olayAdi, veri) => {
                    if (olayAdi === "conversationId") {
                        conversationId = veri;
                    } else if (olayAdi === "token") {
                        asistanBalo.textContent += veri;
                        mesajlarEl.scrollTop = mesajlarEl.scrollHeight;
                    }
                });
            }
        } catch (hata) {
            asistanBalo.textContent = "Bağlantı hatası: " + hata.message;
        } finally {
            gonderButonEl.disabled = false;
        }
    }

    formEl.addEventListener("submit", function (e) {
        e.preventDefault();
        const mesaj = inputEl.value.trim();
        if (!mesaj) return;
        inputEl.value = "";
        mesajGonder(mesaj);
    });

    yeniKonusmaEl.addEventListener("click", function () {
        conversationId = null;
        mesajlarEl.innerHTML = "";
    });
})();
