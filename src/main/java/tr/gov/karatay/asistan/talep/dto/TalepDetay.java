package tr.gov.karatay.asistan.talep.dto;

import java.time.LocalDateTime;
import java.util.List;

import tr.gov.karatay.asistan.common.enums.TalepDurumu;
import tr.gov.karatay.asistan.common.enums.TalepOnceligi;

public record TalepDetay(
        String takipNo,
        String vatandasAd,
        String iletisim,
        String mahalle,
        String konuMetni,
        String kategori,
        String mudurlukAdi,
        TalepDurumu durum,
        TalepOnceligi oncelik,
        LocalDateTime olusturmaTarihi,
        LocalDateTime guncellemeTarihi,
        List<TalepNotuOzeti> notlar) {
}
