package tr.gov.karatay.asistan.rag;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tr.gov.karatay.asistan.rag.dto.RagAramaLoguOzeti;

@RestController
public class RagAramaLoguController {

    private final RagAramaLoguService ragAramaLoguService;

    public RagAramaLoguController(RagAramaLoguService ragAramaLoguService) {
        this.ragAramaLoguService = ragAramaLoguService;
    }

    @GetMapping("/api/rag-arama-loglari")
    public List<RagAramaLoguOzeti> sonAramalariGetir(
            @RequestParam(value = "sadeceIskalamalar", defaultValue = "false") boolean sadeceIskalamalar) {
        return ragAramaLoguService.sonAramalariGetir(sadeceIskalamalar);
    }
}
