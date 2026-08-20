package tr.gov.karatay.asistan.rag;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import tr.gov.karatay.asistan.rag.dto.DokumanOzeti;

@RestController
@RequestMapping("/api/dokumanlar")
public class DokumanController {

    private final DokumanIngestService dokumanIngestService;
    private final DokumanRepository dokumanRepository;

    public DokumanController(DokumanIngestService dokumanIngestService, DokumanRepository dokumanRepository) {
        this.dokumanIngestService = dokumanIngestService;
        this.dokumanRepository = dokumanRepository;
    }

    @PostMapping
    public ResponseEntity<DokumanOzeti> yukle(
            @RequestParam("dosya") MultipartFile dosya,
            @RequestParam("baslik") String baslik,
            @RequestParam(value = "kategori", required = false) String kategori,
            @RequestParam(value = "mod", required = false) String mod) {
        Dokuman dokuman = dokumanIngestService.iceriAktar(dosya, baslik, kategori, mod);
        return ResponseEntity.status(HttpStatus.CREATED).body(ozetleVer(dokuman));
    }

    @GetMapping
    public List<DokumanOzeti> listele() {
        return dokumanRepository.findAll().stream().map(this::ozetleVer).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        dokumanIngestService.sil(id);
        return ResponseEntity.noContent().build();
    }

    private DokumanOzeti ozetleVer(Dokuman dokuman) {
        return new DokumanOzeti(
                dokuman.getId(),
                dokuman.getDosyaAdi(),
                dokuman.getBaslik(),
                dokuman.getKategori(),
                dokuman.getMod(),
                dokuman.getYuklenmeTarihi(),
                dokuman.getChunkSayisi());
    }
}
