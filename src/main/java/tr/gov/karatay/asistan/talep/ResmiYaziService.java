package tr.gov.karatay.asistan.talep;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import tr.gov.karatay.asistan.common.LlmEsZamanliSinirlayici;
import tr.gov.karatay.asistan.talep.dto.ResmiYaziTaslagi;
import tr.gov.karatay.asistan.talep.dto.TalepDetay;

// Faz 4: resmi yazi taslagi uretme. Faz 2'de yuklenen "Resmi Yazismalar
// Yonetmeligi" belgesinden (varsa) format kurallarini RAG ile cekip taslaga
// katiyor - boylece model kendi bildigi genel Turkce resmi yazi formatina
// degil, GERCEK yonetmelige dayanmis oluyor (CLAUDE.md'nin "kaynak koddan
// uretilir" ilkesiyle ayni ruh, ama burada kaynak dogrudan gosterilmiyor,
// sadece taslagi zenginlestirmek icin kullaniliyor). Sonuc HICBIR ZAMAN
// otomatik kaydedilmez/gonderilmez - sadece kopyalanip duzenlenecek bir
// metin dondurur.
@Service
public class ResmiYaziService {

    private static final String FORMAT_SORGUSU = "resmi yazışma formatı hitap imza yetkisi üst yazı";

    private final ChatClient resmiYaziChatClient;
    private final VectorStore vectorStore;
    private final TalepService talepService;
    private final LlmEsZamanliSinirlayici llmSinirlayici;

    public ResmiYaziService(
            @Qualifier("resmiYaziChatClient") ChatClient resmiYaziChatClient,
            VectorStore vectorStore,
            TalepService talepService,
            LlmEsZamanliSinirlayici llmSinirlayici) {
        this.resmiYaziChatClient = resmiYaziChatClient;
        this.vectorStore = vectorStore;
        this.talepService = talepService;
        this.llmSinirlayici = llmSinirlayici;
    }

    public ResmiYaziTaslagi taslakOlustur(String takipNo) {
        TalepDetay talep = talepService
                .talepDetayGetir(takipNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "\"%s\" takip numaralı talep bulunamadı.".formatted(takipNo)));

        String kullaniciMesaji = """
                Takip No: %s
                Konu: %s
                Vatandaş: %s
                Mahalle: %s
                Müdürlük: %s
                Öncelik: %s

                Resmi yazışma format kuralları (varsa):
                %s
                """
                .formatted(
                        talep.takipNo(),
                        talep.konuMetni(),
                        talep.vatandasAd() == null ? "belirtilmemiş" : talep.vatandasAd(),
                        talep.mahalle() == null ? "belirtilmemiş" : talep.mahalle(),
                        talep.mudurlukAdi() == null ? "henüz atanmamış" : talep.mudurlukAdi(),
                        talep.oncelik(),
                        formatBaglamiBul());

        String taslak = llmSinirlayici.sinirliCagir(
                () -> resmiYaziChatClient.prompt().user(kullaniciMesaji).call().content());
        return new ResmiYaziTaslagi(takipNo, taslak);
    }

    private String formatBaglamiBul() {
        List<Document> belgeler = vectorStore.similaritySearch(
                SearchRequest.builder().query(FORMAT_SORGUSU).topK(3).similarityThreshold(0.3).build());
        if (belgeler.isEmpty()) {
            return "(Yüklenmiş belgelerde resmi yazışma formatına dair bir bölüm bulunamadı - genel Türkçe resmi yazışma kurallarını kullan.)";
        }
        return belgeler.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
    }
}
