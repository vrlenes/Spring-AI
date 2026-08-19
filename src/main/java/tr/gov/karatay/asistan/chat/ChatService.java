package tr.gov.karatay.asistan.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import tr.gov.karatay.asistan.common.CokFazlaIstekException;
import tr.gov.karatay.asistan.common.LlmEsZamanliSinirlayici;
import tr.gov.karatay.asistan.talep.PendingActionService;
import tr.gov.karatay.asistan.talep.TalepTools;
import tr.gov.karatay.asistan.talep.dto.PendingActionOzeti;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    // Onceden, kucuk yerel modelin (qwen2.5:7b) belgesiz mevzuat sorularinda
    // hafizasindan halusinasyon uretmesini engellemek icin mesaja zorla bir
    // "sadece 'bulamadim' de" talimati ekleniyordu (BELGE_BULUNAMADI_ONEKI) -
    // ve bunun talep isteklerine karismamasi icin ayri bir anahtar-kelime
    // yonlendirmesi (talepIstegiGibi) gerekiyordu. Bu mekanizma kaldirildi:
    // artik model belgede olmayan bir soruya kendi genel bilgisiyle cevap
    // VEREBILIR (sistem promptu bunu "genel bilgiye dayaniyorum" diye acikca
    // belirtmesini istiyor), ama cevabin belgeye mi yoksa genel bilgiye mi
    // dayandigi frontend'de KODDAN turetiliyor: kaynaklar ve araclar listesi
    // ikisi de bossa, arayuz bunu "genel bilgi" olarak isaretliyor - modelin
    // kendi ifadesine guvenmeden, ayni "kaynak gosterimi koddan uretilir"
    // prensibiyle (bkz. CLAUDE.md).
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final PendingActionService pendingActionService;
    private final ObjectMapper objectMapper;
    private final LlmEsZamanliSinirlayici llmSinirlayici;
    private final int topK;
    private final double benzerlikEsigi;

    public ChatService(
            ChatClient chatClient,
            VectorStore vectorStore,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            PendingActionService pendingActionService,
            ObjectMapper objectMapper,
            LlmEsZamanliSinirlayici llmSinirlayici,
            @Value("${asistan.rag.top-k}") int topK,
            @Value("${asistan.rag.similarity-threshold}") double benzerlikEsigi) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.questionAnswerAdvisor = questionAnswerAdvisor;
        this.pendingActionService = pendingActionService;
        this.objectMapper = objectMapper;
        this.llmSinirlayici = llmSinirlayici;
        this.topK = topK;
        this.benzerlikEsigi = benzerlikEsigi;
    }

    public ChatResponse yanitla(ChatRequest istek) {
        String conversationId = conversationIdCoz(istek.conversationId());
        List<Kaynak> kaynaklar = ilgiliKaynaklariBul(istek.mesaj());
        List<String> bekleyenIslemIdleri = new ArrayList<>();
        List<String> kullanilanAraclar = new ArrayList<>();

        ChatClientResponse yanit = llmSinirlayici.sinirliCagir(() -> chatClient.prompt()
                .user(istek.mesaj())
                .toolContext(Map.of(
                        TalepTools.PENDING_ACTION_ID_SINK, bekleyenIslemIdleri,
                        TalepTools.KULLANILAN_ARAC_SINK, kullanilanAraclar))
                .advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, conversationId);
                    if (!kaynaklar.isEmpty()) {
                        a.advisors(questionAnswerAdvisor);
                    }
                })
                .call()
                .chatClientResponse());

        log.info(
                "Sohbet yaniti uretildi: conversationId={}, kaynakSayisi={}, kullanilanArac={}",
                conversationId,
                kaynaklar.size(),
                benzersiz(kullanilanAraclar));

        return new ChatResponse(
                conversationId,
                metniCikar(yanit),
                kaynaklar,
                bekleyenIslemBul(bekleyenIslemIdleri),
                benzersiz(kullanilanAraclar));
    }

    public Flux<ServerSentEvent<String>> akisliYanitla(ChatRequest istek) {
        // Izin, Flux insa edilmeden ONCE senkron olarak alinir: Flux'lar tembel
        // (lazy) kuruldugu icin, izinAl() burada degil de bir Flux operatoru
        // icinde cagrilsaydi, hemen firlatilan bir CokFazlaIstekException yerine
        // reaktif zincirin bir yerinde sinyallenen bir hata elde ederdik - bunun
        // @RestControllerAdvice tarafindan guvenilir sekilde yakalanip
        // yakalanmayacagi (WebFlux olmayan bu uygulamada) belirsiz. Senkron
        // firlatma, controller metodu cagrilirken atildigi icin kesin yakalanir.
        if (!llmSinirlayici.izinAl()) {
            throw new CokFazlaIstekException(
                    "Sistem şu anda başka isteklerle meşgul, lütfen birkaç saniye sonra tekrar deneyin.");
        }

        String conversationId = conversationIdCoz(istek.conversationId());
        List<Kaynak> kaynaklar = ilgiliKaynaklariBul(istek.mesaj());
        List<String> bekleyenIslemIdleri = new ArrayList<>();
        List<String> kullanilanAraclar = new ArrayList<>();

        Flux<ServerSentEvent<String>> conversationIdOlayi = Flux.just(
                ServerSentEvent.builder(conversationId).event("conversationId").build());

        Flux<ServerSentEvent<String>> tokenOlaylari = chatClient.prompt()
                .user(istek.mesaj())
                .toolContext(Map.of(
                        TalepTools.PENDING_ACTION_ID_SINK, bekleyenIslemIdleri,
                        TalepTools.KULLANILAN_ARAC_SINK, kullanilanAraclar))
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

        Flux<ServerSentEvent<String>> araclarOlayi = Flux.defer(() -> {
            List<String> araclar = benzersiz(kullanilanAraclar);
            if (araclar.isEmpty()) {
                return Flux.empty();
            }
            try {
                String json = objectMapper.writeValueAsString(araclar);
                return Flux.just(ServerSentEvent.builder(json).event("araclar").build());
            } catch (JsonProcessingException e) {
                return Flux.empty();
            }
        });

        return Flux.concat(conversationIdOlayi, tokenOlaylari, kaynakOlayi, bekleyenIslemOlayi, araclarOlayi)
                .doFinally(sinyal -> {
                    llmSinirlayici.izinBirak();
                    log.info(
                            "Akisli sohbet yaniti tamamlandi: conversationId={}, kaynakSayisi={}, kullanilanArac={}, sinyal={}",
                            conversationId,
                            kaynaklar.size(),
                            benzersiz(kullanilanAraclar),
                            sinyal);
                });
    }

    private List<String> benzersiz(List<String> liste) {
        return liste.stream().distinct().toList();
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
