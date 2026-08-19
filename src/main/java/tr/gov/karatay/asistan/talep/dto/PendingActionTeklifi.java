package tr.gov.karatay.asistan.talep.dto;

import java.util.Map;

import tr.gov.karatay.asistan.talep.PendingActionTuru;

// Henuz onaylanmamis bir yazma islemi teklifi: TalepService tarafindan
// dogrulanmis (takip no / mudurluk / enum degeri var mi) ve insanin
// okuyabilecegi bir aciklamayla bicimlendirilmis, ama henuz uygulanmamis.
public record PendingActionTeklifi(
        PendingActionTuru tur, String takipNo, Map<String, String> parametreler, String aciklama) {
}
