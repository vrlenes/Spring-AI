package tr.gov.karatay.asistan.chat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
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

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ChatService(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public ChatResponse yanitla(ChatRequest istek) {
        String conversationId = conversationIdCoz(istek.conversationId());

        ChatClientResponse yanit = chatClient.prompt()
                .user(istek.mesaj())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .chatClientResponse();

        String cevap = metniCikar(yanit);
        List<Kaynak> kaynaklar = kaynaklariCikar(yanit.context());

        return new ChatResponse(conversationId, cevap, kaynaklar);
    }

    public Flux<ServerSentEvent<String>> akisliYanitla(ChatRequest istek) {
        String conversationId = conversationIdCoz(istek.conversationId());

        Flux<ServerSentEvent<String>> conversationIdOlayi = Flux.just(
                ServerSentEvent.builder(conversationId).event("conversationId").build());

        AtomicReference<List<Kaynak>> kaynaklarRef = new AtomicReference<>(List.of());

        Flux<ServerSentEvent<String>> tokenOlaylari = chatClient.prompt()
                .user(istek.mesaj())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .chatClientResponse()
                .doOnNext(yanit -> {
                    List<Kaynak> bulunan = kaynaklariCikar(yanit.context());
                    if (!bulunan.isEmpty()) {
                        kaynaklarRef.set(bulunan);
                    }
                })
                .mapNotNull(yanit -> {
                    String parca = metniCikar(yanit);
                    return (parca == null || parca.isEmpty())
                            ? null
                            : ServerSentEvent.builder(parca).event("token").build();
                });

        Flux<ServerSentEvent<String>> kaynakOlayi = Flux.defer(() -> {
            List<Kaynak> kaynaklar = kaynaklarRef.get();
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

    private String metniCikar(ChatClientResponse yanit) {
        var cevap = yanit.chatResponse();
        if (cevap == null || cevap.getResults().isEmpty()) {
            return null;
        }
        return cevap.getResult().getOutput().getText();
    }

    private List<Kaynak> kaynaklariCikar(Map<String, Object> baglam) {
        if (!(baglam.get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS) instanceof List<?> belgeler)) {
            return List.of();
        }
        return belgeler.stream()
                .filter(Document.class::isInstance)
                .map(Document.class::cast)
                .map(belge -> new Kaynak(
                        String.valueOf(belge.getMetadata().getOrDefault("baslik", "Bilinmeyen belge")),
                        belge.getMetadata().get("chunkIndex") instanceof Number sayi ? sayi.intValue() : 0,
                        belge.getScore()))
                .toList();
    }

    private String conversationIdCoz(String istekteki) {
        return (istekteki == null || istekteki.isBlank()) ? UUID.randomUUID().toString() : istekteki;
    }
}
