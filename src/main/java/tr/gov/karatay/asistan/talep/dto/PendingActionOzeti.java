package tr.gov.karatay.asistan.talep.dto;

import tr.gov.karatay.asistan.talep.PendingActionTuru;

public record PendingActionOzeti(String id, PendingActionTuru tur, String takipNo, String aciklama) {
}
