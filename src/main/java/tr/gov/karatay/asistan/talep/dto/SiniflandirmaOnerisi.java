package tr.gov.karatay.asistan.talep.dto;

// LLM'in .entity() ile yapisal (JSON semasina zorlanmis) urettigi bir ONERI.
// Sohbetteki serbest metin araclarindan farkli olarak burada model konusma
// uretmiyor, dogrudan bu semaya uyan bir nesne uretiyor - Spring AI bunu
// otomatik ayristirip dogruluyor. Yine de mudurlukAdi'nin GERCEKTEN var olan
// bir mudurlukle eslesip eslesmedigi TalepOneriService'te ayrica dogrulanir;
// LLM bir mudurluk adi UYDURMUS olabilir.
public record SiniflandirmaOnerisi(String mudurlukAdi, String kategori, String gerekce) {
}
