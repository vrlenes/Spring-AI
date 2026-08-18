package tr.gov.karatay.asistan.chat;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import tr.gov.karatay.asistan.chat.dto.ChatRequest;
import tr.gov.karatay.asistan.chat.dto.ChatResponse;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest istek) {
        return chatService.yanitla(istek);
    }
}
