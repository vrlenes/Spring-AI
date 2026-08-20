package tr.gov.karatay.asistan.chat;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import tr.gov.karatay.asistan.chat.dto.ChatRequest;
import tr.gov.karatay.asistan.chat.dto.ChatResponse;
import tr.gov.karatay.asistan.personel.PersonelDetails;
import tr.gov.karatay.asistan.sohbet.SohbetService;
import tr.gov.karatay.asistan.sohbet.dto.SohbetMesajOzeti;
import tr.gov.karatay.asistan.sohbet.dto.SohbetOzeti;

@RestController
public class ChatController {

    private final ChatService chatService;
    private final SohbetService sohbetService;

    public ChatController(ChatService chatService, SohbetService sohbetService) {
        this.chatService = chatService;
        this.sohbetService = sohbetService;
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest istek, @AuthenticationPrincipal PersonelDetails personel) {
        return chatService.yanitla(istek, personel.getPersonel().getId());
    }

    @PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @Valid @RequestBody ChatRequest istek, @AuthenticationPrincipal PersonelDetails personel) {
        return chatService.akisliYanitla(istek, personel.getPersonel().getId());
    }

    @GetMapping("/api/sohbetler")
    public List<SohbetOzeti> sohbetleriGetir(@AuthenticationPrincipal PersonelDetails personel) {
        return sohbetService.sohbetleriGetir(personel.getPersonel().getId());
    }

    @GetMapping("/api/sohbetler/{id}/mesajlar")
    public List<SohbetMesajOzeti> mesajlariGetir(@PathVariable String id, @AuthenticationPrincipal PersonelDetails personel) {
        return sohbetService.mesajlariGetir(id, personel.getPersonel().getId());
    }
}
