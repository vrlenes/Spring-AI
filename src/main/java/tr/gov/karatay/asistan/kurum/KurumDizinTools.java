package tr.gov.karatay.asistan.kurum;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import tr.gov.karatay.asistan.talep.TalepTools;

// TalepTools/RagTools ile ayni ince-sarmalayici ilkesi (CLAUDE.md): is
// mantigi burada degil, sadece KurumDizinService'i cagirip sonucu modelin
// okuyacagi metne cevirir. Salt-okunur (yazma yok) - onay akisi gerekmiyor.
// Veri, ana uygulama veritabanindan AYRI bir veritabanindan (kurum_dizini)
// geliyor (bkz. config/KurumDizinDataSourceConfig).
@Component
public class KurumDizinTools {

    private static final int SERT_LIMIT = 20;

    private final KurumDizinService kurumDizinService;

    public KurumDizinTools(KurumDizinService kurumDizinService) {
        this.kurumDizinService = kurumDizinService;
    }

    @Tool(description = "Bir mudurlugun iletisim bilgilerini (telefon, e-posta, adres) getirir. "
            + "Kullanicinin belirttigi mudurluk adiyla kismi eslesme yapar. "
            + "\"bulunamadi\" donerse KESINLIKLE kendi bilginden telefon/e-posta/adres UYDURMA - "
            + "sadece boyle bir kayit bulunamadigini soyle. Bu, mevzuat sorularindan farkli: "
            + "yanlis bir telefon numarasi/e-posta gercekten zararli olabilir.")
    public String mudurlukIletisimGetir(
            @ToolParam(description = "Aranacak mudurluk adi veya bir kismi, orn. 'Fen Isleri' veya 'Imar'")
            String mudurlukAdi,
            ToolContext toolContext) {
        kaydetKullanilanArac(toolContext, "Kurum dizininde arandı");
        List<MudurlukIletisim> sonuclar = kurumDizinService.mudurlukIletisimAra(mudurlukAdi).stream()
                .limit(SERT_LIMIT)
                .toList();
        if (sonuclar.isEmpty()) {
            return "\"%s\" ile eslesen bir mudurluk bulunamadi.".formatted(mudurlukAdi);
        }
        return sonuclar.stream()
                .map(m -> "%s - Telefon: %s, E-posta: %s, Adres: %s".formatted(
                        m.mudurlukAdi(),
                        bosSa(m.telefon()),
                        bosSa(m.eposta()),
                        bosSa(m.adres())))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Personel dizininde ad-soyad, unvan veya mudurluk adina gore arama yapar, "
            + "eslesen personelin iletisim bilgilerini (unvan, mudurluk, telefon, e-posta) getirir. "
            + "\"bulunamadi\" donerse KESINLIKLE kendi bilginden bir isim/telefon/e-posta UYDURMA - "
            + "sadece boyle bir kayit bulunamadigini soyle.")
    public String personelAra(
            @ToolParam(description = "Aranacak ad-soyad, unvan veya mudurluk adi (kismi eslesme)")
            String sorgu,
            ToolContext toolContext) {
        kaydetKullanilanArac(toolContext, "Kurum dizininde arandı");
        List<PersonelDizinKaydi> sonuclar = kurumDizinService.personelAra(sorgu).stream()
                .limit(SERT_LIMIT)
                .toList();
        if (sonuclar.isEmpty()) {
            return "\"%s\" ile eslesen bir personel bulunamadi.".formatted(sorgu);
        }
        return sonuclar.stream()
                .map(p -> "%s (%s, %s) - Telefon: %s, E-posta: %s".formatted(
                        p.adSoyad(),
                        bosSa(p.unvan()),
                        bosSa(p.mudurlukAdi()),
                        bosSa(p.telefon()),
                        bosSa(p.eposta())))
                .collect(Collectors.joining("\n"));
    }

    private String bosSa(String deger) {
        return deger == null || deger.isBlank() ? "bilinmiyor" : deger;
    }

    // TalepTools.KULLANILAN_ARAC_SINK, ChatService'in doldurup toolContext
    // uzerinden gecirdigi ortak sink - bu araci kullanan cevaplarin frontend'de
    // yanlislikla "Genel bilgi" (kaynaksiz) rozeti almamasi icin gerekli (bkz.
    // CLAUDE.md "kaynak gosterimi koddan uretilir" ilkesi).
    @SuppressWarnings("unchecked")
    private void kaydetKullanilanArac(ToolContext toolContext, String etiket) {
        if (toolContext == null) {
            return;
        }
        Object sink = toolContext.getContext().get(TalepTools.KULLANILAN_ARAC_SINK);
        if (sink instanceof List<?> liste) {
            ((List<String>) liste).add(etiket);
        }
    }
}
