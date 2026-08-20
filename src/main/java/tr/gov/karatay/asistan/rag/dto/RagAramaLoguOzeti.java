package tr.gov.karatay.asistan.rag.dto;

import java.time.LocalDateTime;

public record RagAramaLoguOzeti(
        Long id,
        String mod,
        String sorgu,
        int sonucSayisi,
        Double enIyiBenzerlik,
        LocalDateTime olusturmaTarihi) {
}
