package tr.gov.karatay.asistan.chat.dto;

import java.util.List;

public record ChatResponse(String conversationId, String cevap, List<Kaynak> kaynaklar) {
}
