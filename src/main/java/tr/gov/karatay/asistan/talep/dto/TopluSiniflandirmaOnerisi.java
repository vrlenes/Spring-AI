package tr.gov.karatay.asistan.talep.dto;

// Toplu AI oneri listesindeki tek bir satir: hangi talep icin hangi oneri
// uretildi. mudurlukAdi/kategori null olabilir (SiniflandirmaOnerisi ile ayni
// dogrulama kurali gecerli - LLM'in uydurdugu bir mudurluk adi kabul edilmez).
public record TopluSiniflandirmaOnerisi(String takipNo, String konuMetni, SiniflandirmaOnerisi oneri) {
}
