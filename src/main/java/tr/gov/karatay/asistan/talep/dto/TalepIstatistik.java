package tr.gov.karatay.asistan.talep.dto;

import java.util.Map;

public record TalepIstatistik(
        int gunSayisi,
        String mudurlukAdi,
        int toplamTalep,
        Map<String, Long> durumDagilimi) {
}
