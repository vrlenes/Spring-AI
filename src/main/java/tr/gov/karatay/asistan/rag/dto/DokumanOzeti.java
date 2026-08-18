package tr.gov.karatay.asistan.rag.dto;

import java.time.LocalDateTime;

public record DokumanOzeti(
        Long id,
        String dosyaAdi,
        String baslik,
        String kategori,
        LocalDateTime yuklenmeTarihi,
        Integer chunkSayisi) {
}
