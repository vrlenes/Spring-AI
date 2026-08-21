package tr.gov.karatay.asistan.sohbet.dto;

import tr.gov.karatay.asistan.common.enums.GeriBildirim;

// deger null olabilir - kullanici zaten verdigi bir geri bildirimi geri
// almak (toggle-off) istediginde gonderilir.
public record GeriBildirimIstegi(GeriBildirim deger) {
}
