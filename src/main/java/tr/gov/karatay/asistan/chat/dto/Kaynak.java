package tr.gov.karatay.asistan.chat.dto;

// maddeNo, parca metninden regex ile cikarilir (bkz. RagTools) - "Parca N"
// (chunk index, kullaniciya anlamsiz) yerine kullaniciya asil anlamli olan
// "Madde N" referansini gostermek icin. Bulunamazsa null - frontend bu
// durumda Parca No'ya geri doner.
public record Kaynak(String baslik, int parcaNo, Double benzerlik, Integer maddeNo) {
}
