package tr.gov.karatay.asistan.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginIstegi(@NotBlank String kullaniciAdi, @NotBlank String sifre) {
}
