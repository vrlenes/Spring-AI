package tr.gov.karatay.asistan.chat;

import java.util.List;
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

@Service
public class ChatService {

    // Sistem promptundaki "asla uydurma" kurali tek basina yetmiyor: kucuk yerel
    // modelde (qwen2.5:7b) genel bilgisine guvendigi konularda (orn. "belediye
    // meclisi kac uyeden olusur") halusinasyon gordugumuz icin, hicbir belge
    // eslesmediginde bunu mesaja acikca (ve o an icin) ekliyoruz - uzun sistem
    // promptunun icinde gomulu bir kural olarak degil, dogrudan ve guncel bir
    // "sistem notu" olarak, modelin gormezden gelmesi cok daha zor.
    private static final String BELGE_BULUNAMADI_NOTU = """


            (Sistem notu: Bu soruyla ilgili yüklü belgelerde hiçbir eşleşme bulunamadı. \
            Bu bir mevzuat/prosedür sorusuysa KESİNLİKLE kendi genel bilgini kullanma, \
            sadece "Bu konuda yüklenmiş belgelerde bilgi bulamadım" de. Genel bir \
            sohbet/selamlama ise normal cevap ver.)""";

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final ObjectMapper objectMapper;
    private final int topK;
    private final double benzerlikEsigi;

    public ChatService(
            ChatClient chatClient,
            VectorStore vectorStore,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            ObjectMapper objectMapper,
            @Value("${asistan.rag.top-k}") int topK,
            @Value("${asistan.rag.similarity-threshold}") double benzerlikEsigi) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.questionAnswerAdvisor = questionAnswerAdvisor;
        this.objectMapper = objectMapper;
        this.topK = topK;
        this.benzerlikEsigi = benzerlikEsigi;
    }

    public ChatResponse yanitla(ChatRequest istek) {
        String conversationId = conversationIdCoz(istek.conversationId());
        List<Kaynak> kaynaklar = ilgiliKaynaklariBul(istek.mesaj());

        ChatClientResponse yanit = chatClient.prompt()
                .user(kullaniciMesajiHazirla(istek.mesaj(), kaynaklar))
                .advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, conversationId);
                    if (!kaynaklar.isEmpty()) {
                        a.advisors(questionAnswerAdvisor);
                    }
                })
                .call()
                .chatClientResponse();

        return new ChatResponse(conversationId, metniCikar(yanit), kaynaklar);
    }

    public Flux<ServerSentEvent<String>> akisliYanitla(ChatRequest istek) {
        String conversationId = conversationIdCoz(istek.conversationId());
        List<Kaynak> kaynaklar = ilgiliKaynaklariBul(istek.mesaj());

        Flux<ServerSentEvent<String>> conversationIdOlayi = Flux.just(
                ServerSentEvent.builder(conversationId).event("conversationId").build());

        Flux<ServerSentEvent<String>> tokenOlaylari = chatClient.prompt()
                .user(kullaniciMesajiHazirla(istek.mesaj(), kaynaklar))
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

        return Flux.concat(conversationIdOlayi, tokenOlaylari, kaynakOlayi);
    }

    private String kullaniciMesajiHazirla(String mesaj, List<Kaynak> kaynaklar) {
        return kaynaklar.isEmpty() ? mesaj + BELGE_BULUNAMADI_NOTU : mesaj;
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
