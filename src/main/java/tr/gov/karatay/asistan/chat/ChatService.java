package tr.gov.karatay.asistan.chat;

import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import tr.gov.karatay.asistan.chat.dto.ChatRequest;
import tr.gov.karatay.asistan.chat.dto.ChatResponse;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResponse yanitla(ChatRequest istek) {
        String conversationId = (istek.conversationId() == null || istek.conversationId().isBlank())
                ? UUID.randomUUID().toString()
                : istek.conversationId();

        String cevap = chatClient.prompt()
                .user(istek.mesaj())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        return new ChatResponse(conversationId, cevap);
    }
}
