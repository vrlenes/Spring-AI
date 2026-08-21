package tr.gov.karatay.asistan.rag;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import tr.gov.karatay.asistan.chat.dto.KaynakDogrulamaSonucu;
import tr.gov.karatay.asistan.common.LlmEsZamanliSinirlayici;

// RAG cevabinin, kullanilan kaynak metinlerine gercekten sadik kalip
// kalmadigini kontrol eden bagimsiz bir "gerceklik kontrolu" adimi -
// modelin kendi cevabini degil, AYRI ve KOR bir ikinci model cagrisini
// kullanir (bkz. CLAUDE.md "kaynak gosterimi koddan uretilir" ilkesiyle
// ayni ruh: modele guvenmek yerine dogrulanabilir bir mekanizma). Herhangi
// bir hata/belirsizlik durumunda null doner - rozet o turda hic
// gosterilmez, YANLIS bir "dogrulanamadi" iddiasi da uretilmez.
@Service
public class KaynakDogrulamaService {

    private static final Logger log = LoggerFactory.getLogger(KaynakDogrulamaService.class);

    private final ChatClient kaynakDogrulamaChatClient;
    private final LlmEsZamanliSinirlayici llmSinirlayici;

    public KaynakDogrulamaService(
            @Qualifier("kaynakDogrulamaChatClient") ChatClient kaynakDogrulamaChatClient,
            LlmEsZamanliSinirlayici llmSinirlayici) {
        this.kaynakDogrulamaChatClient = kaynakDogrulamaChatClient;
        this.llmSinirlayici = llmSinirlayici;
    }

    public KaynakDogrulamaSonucu dogrula(String cevapMetni, List<String> kaynakMetinleri) {
        if (cevapMetni == null || cevapMetni.isBlank() || kaynakMetinleri == null || kaynakMetinleri.isEmpty()) {
            return null;
        }
        String kullaniciMesaji = """
                CEVAP METNİ:
                %s

                KAYNAK METİNLERİ:
                %s
                """
                .formatted(cevapMetni, String.join("\n---\n", kaynakMetinleri));
        try {
            return llmSinirlayici.sinirliCagir(() -> kaynakDogrulamaChatClient
                    .prompt()
                    .user(kullaniciMesaji)
                    .call()
                    .entity(KaynakDogrulamaSonucu.class));
        } catch (Exception e) {
            log.warn("Kaynak dogrulama basarisiz, rozet gosterilmeyecek: {}", e.getMessage());
            return null;
        }
    }
}
