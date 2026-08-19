package tr.gov.karatay.asistan.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import tr.gov.karatay.asistan.chat.dto.ChatRequest;
import tr.gov.karatay.asistan.chat.dto.ChatResponse;
import tr.gov.karatay.asistan.chat.dto.Kaynak;
import tr.gov.karatay.asistan.talep.PendingActionService;
import tr.gov.karatay.asistan.talep.TalepTools;
import tr.gov.karatay.asistan.talep.dto.PendingActionOzeti;

@Service
public class ChatService {

    // Sistem promptundaki "asla uydurma" kurali tek basina yetmiyor: kucuk yerel
    // modelde (qwen2.5:7b) genel bilgisine guvendigi konularda (orn. "belediye
    // meclisi kac uyeden olusur", "meclis ve encumen farki") halusinasyon
    // gorduk - hatta mesaja EKLENEN ilk (daha yumusak) uyari da her zaman
    // yetmedi. Bunun yerine, dogrudan komut niteliginde, mesajin BASINA
    // (sonuna degil - deneyerek daha guvenilir oldugunu gorduk) eklenen, genel
    // sohbet icin acik istisnasi olan sert bir "sistem uyarisi" kullaniyoruz.
    private static final String BELGE_BULUNAMADI_ONEKI = """
            SİSTEM UYARISI - BU TALİMATI ATLAMA: Şu an vektör veritabanında \
            kullanıcının sorusuyla eşleşen HİÇBİR belge parçası bulunamadı (0 sonuç). \
            Kuralların gereği, bu durumda eğer aşağıdaki mesaj bir mevzuat/prosedür \
            sorusuysa ASLA kendi bilgini, tahminini veya genel kültürünü kullanarak \
            cevap YAZAMAZSIN - başka hiçbir açıklama eklemeden TAM OLARAK şu cümleyi \
            yaz: "Bu konuda yüklenmiş belgelerde bilgi bulamadım." ANCAK bu kural \
            SADECE mevzuat/prosedür sorularında geçerlidir. Aşağıdaki mesaj genel \
            bir sohbet, selamlama, senin ne yapabildiğinle ilgiliyse VEYA vatandaş \
            talepleriyle ilgili bir istekse (listeleme, arama, atama, durum/öncelik \
            güncelleme, not ekleme, istatistik, müdürlük bilgisi gibi - bu tür \
            isteklerde belge aranmaz, sana verilen ilgili aracı kullanarak cevap \
            ver) bu kuralı yoksay ve normal, yardımcı bir şekilde cevap ver.

            Şimdi kullanıcının mesajı:

            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final PendingActionService pendingActionService;
    private final ObjectMapper objectMapper;
    private final int topK;
    private final double benzerlikEsigi;

    public ChatService(
            ChatClient chatClient,
            VectorStore vectorStore,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            PendingActionService pendingActionService,
            ObjectMapper objectMapper,
            @Value("${asistan.rag.top-k}") int topK,
            @Value("${asistan.rag.similarity-threshold}") double benzerlikEsigi) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.questionAnswerAdvisor = questionAnswerAdvisor;
        this.pendingActionService = pendingActionService;
        this.objectMapper = objectMapper;
        this.topK = topK;
        this.benzerlikEsigi = benzerlikEsigi;
    }

    public ChatResponse yanitla(ChatRequest istek) {
        String conversationId = conversationIdCoz(istek.conversationId());
        List<Kaynak> kaynaklar = ilgiliKaynaklariBul(istek.mesaj());
        List<String> bekleyenIslemIdleri = new ArrayList<>();

        ChatClientResponse yanit = chatClient.prompt()
                .user(kullaniciMesajiHazirla(istek.mesaj(), kaynaklar))
                .toolContext(Map.of(TalepTools.PENDING_ACTION_ID_SINK, bekleyenIslemIdleri))
                .advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, conversationId);
                    if (!kaynaklar.isEmpty()) {
                        a.advisors(questionAnswerAdvisor);
                    }
                })
                .call()
                .chatClientResponse();

        return new ChatResponse(conversationId, metniCikar(yanit), kaynaklar, bekleyenIslemBul(bekleyenIslemIdleri));
    }

    public Flux<ServerSentEvent<String>> akisliYanitla(ChatRequest istek) {
        String conversationId = conversationIdCoz(istek.conversationId());
        List<Kaynak> kaynaklar = ilgiliKaynaklariBul(istek.mesaj());
        List<String> bekleyenIslemIdleri = new ArrayList<>();

        Flux<ServerSentEvent<String>> conversationIdOlayi = Flux.just(
                ServerSentEvent.builder(conversationId).event("conversationId").build());

        Flux<ServerSentEvent<String>> tokenOlaylari = chatClient.prompt()
                .user(kullaniciMesajiHazirla(istek.mesaj(), kaynaklar))
                .toolContext(Map.of(TalepTools.PENDING_ACTION_ID_SINK, bekleyenIslemIdleri))
                .advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, conversationId);
                    if (!kaynaklar.isEmpty()) {
                        a.advisors(questionAnswerAdvisor);
                    }
                })
                .stream()
                .chatClientResponse()
                .mapNotNull(yanit -> {
                    String parca = metniCikar(yanit);
                    return (parca == null || parca.isEmpty())
                            ? null
                            : ServerSentEvent.builder(parca).event("token").build();
                });

        Flux<ServerSentEvent<String>> kaynakOlayi = Flux.defer(() -> {
            if (kaynaklar.isEmpty()) {
                return Flux.empty();
            }
            try {
                String json = objectMapper.writeValueAsString(kaynaklar);
                return Flux.just(ServerSentEvent.builder(json).event("kaynaklar").build());
            } catch (JsonProcessingException e) {
                return Flux.empty();
            }
        });

        Flux<ServerSentEvent<String>> bekleyenIslemOlayi = Flux.defer(() -> {
            PendingActionOzeti bekleyenIslem = bekleyenIslemBul(bekleyenIslemIdleri);
            if (bekleyenIslem == null) {
                return Flux.empty();
            }
            try {
                String json = objectMapper.writeValueAsString(bekleyenIslem);
                return Flux.just(ServerSentEvent.builder(json).event("bekleyenIslem").build());
            } catch (JsonProcessingException e) {
                return Flux.empty();
            }
        });

        return Flux.concat(conversationIdOlayi, tokenOlaylari, kaynakOlayi, bekleyenIslemOlayi);
    }

    private PendingActionOzeti bekleyenIslemBul(List<String> bekleyenIslemIdleri) {
        if (bekleyenIslemIdleri.isEmpty()) {
            return null;
        }
        return pendingActionService
                .getir(bekleyenIslemIdleri.get(0))
                .map(a -> new PendingActionOzeti(a.id(), a.tur(), a.takipNo(), a.aciklama()))
                .orElse(null);
    }

    // Kucuk yerel model, BELGE_BULUNAMADI_ONEKI icindeki "ama talep yonetimi
    // istegiyse yoksay" istisnasini guvenilir sekilde ayirt edemiyor (denendi,
    // "Acik talepleri listele" gibi acik bir tool komutunu bile mevzuat sorusu
    // sanip "bulamadim" diyebiliyor). Bu yuzden ayirim modelin yorumuna degil,
    // koddaki bu anahtar kelime kontrolune birakiliyor - ayni "kaynak gosterimi
    // LLM'e degil koda dayanir" prensibi (bkz. CLAUDE.md).
    private static final List<String> TALEP_ANAHTAR_KELIMELERI = List.of(
            "talep", "tlp-", "müdürlük", "sikayet", "şikayet", "öncelik", "oncelik",
            "durum güncelle", "durum guncelle", "takip no", "not ekle", "istatistik",
            "mahalle", "vatandaş", "vatandas");

    private boolean talepIstegiGibi(String mesaj) {
        String kucukMesaj = mesaj.toLowerCase(Locale.of("tr"));
        return TALEP_ANAHTAR_KELIMELERI.stream().anyMatch(kucukMesaj::contains);
    }

    private String kullaniciMesajiHazirla(String mesaj, List<Kaynak> kaynaklar) {
        if (!kaynaklar.isEmpty() || talepIstegiGibi(mesaj)) {
            return mesaj;
        }
        return BELGE_BULUNAMADI_ONEKI + mesaj;
    }

    private List<Kaynak> ilgiliKaynaklariBul(String mesaj) {
        SearchRequest aramaIstegi = SearchRequest.builder()
                .query(mesaj)
                .topK(topK)
                .similarityThreshold(benzerlikEsigi)
                .build();

        List<Document> belgeler = vectorStore.similaritySearch(aramaIstegi);
        return belgeler.stream()
                .map(belge -> new Kaynak(
                        String.valueOf(belge.getMetadata().getOrDefault("baslik", "Bilinmeyen belge")),
                        belge.getMetadata().get("chunkIndex") instanceof Number sayi ? sayi.intValue() : 0,
                        belge.getScore()))
                .toList();
    }

    private String metniCikar(ChatClientResponse yanit) {
        var cevap = yanit.chatResponse();
        if (cevap == null || cevap.getResults().isEmpty()) {
            return null;
        }
        return cevap.getResult().getOutput().getText();
    }

    private String conversationIdCoz(String istekteki) {
        return (istekteki == null || istekteki.isBlank()) ? UUID.randomUUID().toString() : istekteki;
    }
}
