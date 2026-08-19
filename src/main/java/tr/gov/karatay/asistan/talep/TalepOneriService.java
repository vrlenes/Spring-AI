package tr.gov.karatay.asistan.talep;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import tr.gov.karatay.asistan.mudurluk.MudurlukService;
import tr.gov.karatay.asistan.mudurluk.dto.MudurlukOzeti;
import tr.gov.karatay.asistan.talep.dto.SiniflandirmaOnerisi;
import tr.gov.karatay.asistan.talep.dto.TalepDetay;
import tr.gov.karatay.asistan.talep.dto.TalepOzeti;
import tr.gov.karatay.asistan.talep.dto.TopluSiniflandirmaOnerisi;

// LLM sadece bir ONERI uretir - hicbir veriyi degistirmez. Kullanici oneriyi
// "Uygula" derse, TalepController uzerinden zaten var olan (LLM'den bagimsiz)
// talebiMudurlugeAta REST cagrisi tetiklenir. Bu, sohbetteki yazma araclariyla
// ayni ilke: model karar onerir, gercek mutasyon koda/kullaniciya baglidir.
@Service
public class TalepOneriService {

    private final ChatClient siniflandirmaChatClient;
    private final TalepService talepService;
    private final MudurlukService mudurlukService;

    public TalepOneriService(
            @Qualifier("siniflandirmaChatClient") ChatClient siniflandirmaChatClient,
            TalepService talepService,
            MudurlukService mudurlukService) {
        this.siniflandirmaChatClient = siniflandirmaChatClient;
        this.talepService = talepService;
        this.mudurlukService = mudurlukService;
    }

    private static final int TOPLU_ONERI_SERT_LIMIT = 20;

    public SiniflandirmaOnerisi oneriOlustur(String takipNo) {
        TalepDetay talep = talepService
                .talepDetayGetir(takipNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "\"%s\" takip numaralı talep bulunamadı.".formatted(takipNo)));

        return oneriUret(talep, mudurlukService.mudurlukleriListele());
    }

    // Atanmamis (mudurluk_id NULL) talepleri tek tek LLM'e gonderip oneri
    // uretir. Hicbir sey uygulanmaz - PendingAction ile ayni ilke: sonuc sadece
    // bir liste, kullanici arayuzde tek tek veya topluca "Uygula" demeden
    // veritabaninda hicbir sey degismez. Ollama'nin tek istemcili yerel
    // calisma sekli nedeniyle cagrilar bilerek SIRAYLA yapiliyor (paralel
    // degil) - hem yuku dagitmak hem de gercek uygulama davranisini yansitmak
    // icin.
    public List<TopluSiniflandirmaOnerisi> topluOneriOlustur(Integer limit) {
        int gercekLimit = limit == null || limit <= 0 ? TOPLU_ONERI_SERT_LIMIT : Math.min(limit, TOPLU_ONERI_SERT_LIMIT);

        List<TalepOzeti> atanmamislar = talepService.talepleriGetir(null, null, null, null, null, true, gercekLimit);
        List<MudurlukOzeti> mudurlukler = mudurlukService.mudurlukleriListele();

        return atanmamislar.stream()
                .map(t -> {
                    TalepDetay detay = talepService.talepDetayGetir(t.takipNo()).orElseThrow();
                    return new TopluSiniflandirmaOnerisi(t.takipNo(), t.konuMetni(), oneriUret(detay, mudurlukler));
                })
                .toList();
    }

    private SiniflandirmaOnerisi oneriUret(TalepDetay talep, List<MudurlukOzeti> mudurlukler) {
        String mudurlukListesi = mudurlukler.stream()
                .map(m -> "- %s: %s".formatted(m.ad(), m.sorumlulukAlani()))
                .collect(Collectors.joining("\n"));

        String kullaniciMesaji = """
                Talep konusu: %s
                Mahalle: %s
                Mevcut kategori: %s

                Müdürlükler:
                %s
                """
                .formatted(
                        talep.konuMetni(),
                        talep.mahalle() == null ? "belirtilmemiş" : talep.mahalle(),
                        talep.kategori() == null ? "sınıflandırılmamış" : talep.kategori(),
                        mudurlukListesi);

        SiniflandirmaOnerisi oneri = siniflandirmaChatClient
                .prompt()
                .user(kullaniciMesaji)
                .call()
                .entity(SiniflandirmaOnerisi.class);

        return dogrula(oneri, mudurlukler);
    }

    // LLM'in mudurlukAdi olarak dondurdugu deger, gercekten var olan bir
    // mudurlukle (buyuk/kucuk harf duyarsiz) esleşmiyorsa null'a cevrilir -
    // frontend bu durumda "Uygula" butonunu devre disi birakir.
    private SiniflandirmaOnerisi dogrula(SiniflandirmaOnerisi oneri, List<MudurlukOzeti> mudurlukler) {
        if (oneri == null) {
            return new SiniflandirmaOnerisi(null, null, "AI önerisi alınamadı.");
        }
        String eslesenAd = mudurlukler.stream()
                .map(MudurlukOzeti::ad)
                .filter(ad -> ad.equalsIgnoreCase(oneri.mudurlukAdi()))
                .findFirst()
                .orElse(null);
        return new SiniflandirmaOnerisi(eslesenAd, oneri.kategori(), oneri.gerekce());
    }
}
