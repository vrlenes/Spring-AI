package tr.gov.karatay.asistan.chat.dto;

import jakarta.validation.constraints.NotBlank;

import tr.gov.karatay.asistan.common.enums.SohbetModu;

// mod verilmezse (eski istemciler/varsayilan) GENEL kabul edilir - bkz.
// ChatService. conversationId, kalici Sohbet kaydinin da id'sidir (bkz.
// Sohbet.java) - LLM baglami ve kalici gecmis ayni id'yi paylasir.
public record ChatRequest(String conversationId, @NotBlank String mesaj, SohbetModu mod) {
}
