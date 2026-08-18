package tr.gov.karatay.asistan.chat;

import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import tr.gov.karatay.asistan.chat.dto.ChatRequest;
import tr.gov.karatay.asistan.chat.dto.ChatResponse;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResponse yanitla(ChatRequest istek) {
        String conversationId = conversationIdCoz(istek.conversationId());

        String cevap = chatClient.prompt()
                .user(istek.mesaj())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        return new ChatResponse(conversationId, cevap);
    }

    public Flux<ServerSentEvent<String>> akisliYanitla(ChatRequest istek) {
        String conversationId = conversationIdCoz(istek.conversationId());

        Flux<ServerSentEvent<String>> conversationIdOlayi = Flux.just(
                ServerSentEvent.builder(conversationId).event("conversationId").build());

        Flux<ServerSentEvent<String>> tokenOlaylari = chatClient.prompt()
                .user(istek.mesaj())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .map(parca -> ServerSentEvent.builder(parca).event("token").build());

        return Flux.concat(conversationIdOlayi, tokenOlaylari);
    }

    private String conversationIdCoz(String istekteki) {
        return (istekteki == null || istekteki.isBlank()) ? UUID.randomUUID().toString() : istekteki;
    }
}
