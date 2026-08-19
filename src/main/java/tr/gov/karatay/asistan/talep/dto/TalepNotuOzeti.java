package tr.gov.karatay.asistan.talep.dto;

import java.time.LocalDateTime;

public record TalepNotuOzeti(String personel, String notu, LocalDateTime tarih) {
}
