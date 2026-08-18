package tr.gov.karatay.asistan.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(String conversationId, @NotBlank String mesaj) {
}
