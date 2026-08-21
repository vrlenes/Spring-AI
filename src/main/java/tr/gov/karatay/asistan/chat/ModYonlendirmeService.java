package tr.gov.karatay.asistan.chat;

import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import tr.gov.karatay.asistan.common.LlmEsZamanliSinirlayici;
import tr.gov.karatay.asistan.common.enums.SohbetModu;

// "Otomatik" mod secildiginde her mesaji GENEL/TALEP/IMAR/RUHSAT'tan birine
// yonlendirir - kullanicinin mod kartlarindan elle secim yapmasina gerek
// birakmadan. Bilincli olarak DUSUK RISKLI tutuldu: bu siniflandirmanin
// yanlis cikmasi hicbir veriyi bozmaz (yazma araclari zaten PendingAction
// onayindan geciyor - bkz. CLAUDE.md), sadece o turda yanlis sistem
// promptu/RAG kapsaminin kullanilmasina yol acar. Bu yuzden herhangi bir
// belirsizlik/hata GENEL'e (en az yan etkili mod) duser, asla istisna
// firlatip kullanicinin mesajini yarida birakmaz.
@Service
public class ModYonlendirmeService {

    private static final Logger log = LoggerFactory.getLogger(ModYonlendirmeService.class);

    private static final Set<String> GECERLI_MODLAR = Set.of("GENEL", "TALEP", "IMAR", "RUHSAT");

    private final ChatClient modYonlendirmeChatClient;
    private final LlmEsZamanliSinirlayici llmSinirlayici;

    public ModYonlendirmeService(
            @Qualifier("modYonlendirmeChatClient") ChatClient modYonlendirmeChatClient,
            LlmEsZamanliSinirlayici llmSinirlayici) {
        this.modYonlendirmeChatClient = modYonlendirmeChatClient;
        this.llmSinirlayici = llmSinirlayici;
    }

    public SohbetModu yonlendir(String mesaj) {
        try {
            String cevap = llmSinirlayici.sinirliCagir(() ->
                    modYonlendirmeChatClient.prompt().user(mesaj == null ? "" : mesaj).call().content());
            return dogrula(cevap);
        } catch (Exception e) {
            log.warn("Mod yönlendirme başarısız, GENEL moduna düşülüyor: {}", e.getMessage());
            return SohbetModu.GENEL;
        }
    }

    private SohbetModu dogrula(String cevap) {
        if (cevap == null) {
            return SohbetModu.GENEL;
        }
        String temiz = cevap.trim().toUpperCase(Locale.ROOT);
        return GECERLI_MODLAR.contains(temiz) ? SohbetModu.valueOf(temiz) : SohbetModu.GENEL;
    }
}
