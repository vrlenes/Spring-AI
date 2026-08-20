package tr.gov.karatay.asistan.chat;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import reactor.core.publisher.Flux;
import tr.gov.karatay.asistan.chat.dto.ChatRequest;
import tr.gov.karatay.asistan.chat.dto.ChatResponse;
import tr.gov.karatay.asistan.common.enums.SohbetModu;
import tr.gov.karatay.asistan.personel.PersonelDetails;
import tr.gov.karatay.asistan.sohbet.SohbetService;
import tr.gov.karatay.asistan.sohbet.dto.EkVerisi;
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

    // Dosya ekli gonderim icin ayri, multipart bir handler - mevcut JSON
    // handler'lara (yukarida) hic dokunulmuyor, boylece eksiz-mesaj yolunda
    // sifir regresyon riski var (bkz. plan). mesaj burada bos olabilir -
    // @Valid/@NotBlank kasitli olarak devre disi, cunku sadece dosyayla
    // (metinsiz) gonderim gecerli bir senaryo.
    @PostMapping(value = "/api/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponse chatEkli(
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String mesaj,
            @RequestParam(required = false) SohbetModu mod,
            @RequestParam("dosya") MultipartFile dosya,
            @AuthenticationPrincipal PersonelDetails personel) {
        ChatRequest istek = new ChatRequest(conversationId, mesaj == null ? "" : mesaj, mod);
        return chatService.yanitla(istek, personel.getPersonel().getId(), dosya);
    }

    @PostMapping(
            value = "/api/chat/stream",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStreamEkli(
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String mesaj,
            @RequestParam(required = false) SohbetModu mod,
            @RequestParam("dosya") MultipartFile dosya,
            @AuthenticationPrincipal PersonelDetails personel) {
        ChatRequest istek = new ChatRequest(conversationId, mesaj == null ? "" : mesaj, mod);
        return chatService.akisliYanitla(istek, personel.getPersonel().getId(), dosya);
    }

    @GetMapping("/api/sohbetler/{sohbetId}/mesajlar/{mesajId}/ek")
    public ResponseEntity<byte[]> ekGetir(
            @PathVariable String sohbetId, @PathVariable Long mesajId, @AuthenticationPrincipal PersonelDetails personel) {
        EkVerisi ek = sohbetService.ekGetir(sohbetId, mesajId, personel.getPersonel().getId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ek.mimeTipi()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"%s\"".formatted(ek.dosyaAdi()))
                .body(ek.veri());
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
