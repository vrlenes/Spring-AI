package tr.gov.karatay.asistan.chat.dto;

import java.util.List;

import tr.gov.karatay.asistan.talep.dto.PendingActionOzeti;

public record ChatResponse(
        String conversationId,
        String cevap,
        List<Kaynak> kaynaklar,
        PendingActionOzeti bekleyenIslem,
        List<String> araclar,
        YapisalVeriPaketi yapisalVeri) {
}
