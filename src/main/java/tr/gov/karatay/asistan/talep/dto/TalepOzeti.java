package tr.gov.karatay.asistan.talep.dto;

import java.time.LocalDateTime;

import tr.gov.karatay.asistan.common.enums.TalepDurumu;
import tr.gov.karatay.asistan.common.enums.TalepOnceligi;

public record TalepOzeti(
        String takipNo,
        String vatandasAd,
        String mahalle,
        String konuMetni,
        String kategori,
        String mudurlukAdi,
        TalepDurumu durum,
        TalepOnceligi oncelik,
        LocalDateTime olusturmaTarihi) {
}
