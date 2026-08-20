package tr.gov.karatay.asistan.sohbet.dto;

import java.time.LocalDateTime;

import tr.gov.karatay.asistan.common.enums.SohbetModu;

public record SohbetOzeti(String id, SohbetModu mod, String baslik, LocalDateTime guncellemeTarihi) {
}
