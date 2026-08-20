package tr.gov.karatay.asistan.chat.dto;

// Tool sonuclarinin, modelin yazdigi serbest metne degil, KODUN urettigi ham
// veriye dayanarak sohbette gosterilmesi icin (bkz. kaynaklar/araclar ile
// ayni ilke, CLAUDE.md). "tip" alani frontend'de hangi kart bilesenin
// render edilecegini belirler (orn. "TALEP_LISTESI"); "veri" o tipe uygun
// ham DTO/liste. Yeni bir tip eklemek, burada yeni bir sabit + ilgili
// tool'da kaydetYapisalVeri cagrisi + frontend'de yeni bir kart bileseni
// eklemekten ibaret olmali.
public record YapisalVeriPaketi(String tip, Object veri) {
}
