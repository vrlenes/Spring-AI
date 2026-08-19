package tr.gov.karatay.asistan.talep;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import tr.gov.karatay.asistan.common.enums.TalepDurumu;
import tr.gov.karatay.asistan.common.enums.TalepOnceligi;
import tr.gov.karatay.asistan.mudurluk.MudurlukService;
import tr.gov.karatay.asistan.mudurluk.dto.MudurlukOzeti;
import tr.gov.karatay.asistan.talep.dto.PendingActionTeklifi;
import tr.gov.karatay.asistan.talep.dto.TalepDetay;
import tr.gov.karatay.asistan.talep.dto.TalepIstatistik;
import tr.gov.karatay.asistan.talep.dto.TalepOzeti;

// Bu sinif is mantigi icermez: sadece TalepService/PendingActionService'i cagirir,
// LLM'in okuyabilecegi Turkce metne bicimlendirir ve hatalari yakalayip anlamli
// metin dondurur (CLAUDE.md - "*Tools siniflari ince bir sarmalayici olmali" kurali).
//
// Yazma araclari (talebiMudurlugeAta vb.) VERIYI DOGRUDAN DEGISTIRMEZ: sadece
// dogrulanmis bir PendingAction olusturur. Gercek mutasyon, kullanici arayuzde
// Onayla butonuna basip PendingActionController'i tetikledigi zaman olur. Bu,
// kucuk yerel modelin (qwen2.5:7b) onay akisinda gozlenen iki hatasini
// (onay almadan islem yapmasi / onaydan sonra araci hic cagirmadan "yapildi"
// demesi) koddan kapatir - onay artik modelin degil, kullanicinin butonuna
// basmasina bagli.
@Component
public class TalepTools {

    public static final String PENDING_ACTION_ID_SINK = "pendingActionIdSink";
    public static final String KULLANILAN_ARAC_SINK = "kullanilanAracSink";

    private static final DateTimeFormatter TARIH_BICIMI = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final TalepService talepService;
    private final PendingActionService pendingActionService;
    private final MudurlukService mudurlukService;

    public TalepTools(TalepService talepService, PendingActionService pendingActionService, MudurlukService mudurlukService) {
        this.talepService = talepService;
        this.pendingActionService = pendingActionService;
        this.mudurlukService = mudurlukService;
    }

    @Tool(description = """
            Vatandas taleplerini listeler/arar. Hicbir parametre verilmezse acik
            (YENI/ATANDI/ISLEMDE) talepleri oncelik sirasina gore (ACIL en once)
            gosterir, en fazla 20 tanesini dondurur. Durum, mudurluk, mahalle ve/veya
            anahtar kelimeyle filtrelenebilir. Ornek: kullanici "Durumu YENI olan
            talepleri listele" derse, durum="YENI" ile cagir, diger parametreleri
            bos birak.""")
    public String talepleriGetir(
            @ToolParam(required = false, description = "Kullanici bir durum belirttiyse birebir yaz (YENI, ATANDI, ISLEMDE, COZULDU, REDDEDILDI), belirtmediyse bos birak - bos birakilirsa sadece acik (cozulmemis) talepler gelir.")
            String durum,
            @ToolParam(required = false, description = "Kullanici bir kelime/konu belirttiyse yaz, belirtmediyse bos birak.")
            String anahtarKelime,
            @ToolParam(required = false, description = "Kullanici bir mudurluk belirttiyse tam adini yaz, belirtmediyse bos birak.")
            String mudurluk,
            @ToolParam(required = false, description = "Kullanici bir mahalle belirttiyse yaz, belirtmediyse bos birak.")
            String mahalle,
            @ToolParam(required = false, description = "Kullanici bir zaman araligi belirttiyse (orn. 'son 30 gun') gun sayisi olarak yaz, belirtmediyse bos birak.")
            Integer gunSayisi,
            @ToolParam(required = false, description = "Donecek maksimum kayit sayisi, varsayilan ve ust sinir 20.")
            Integer limit,
            ToolContext toolContext) {
        kaydetKullanilanArac(toolContext, "Talepler sorgulandı");
        TalepDurumu durumEnum;
        try {
            durumEnum = durum == null || durum.isBlank() ? null : TalepDurumu.valueOf(durum.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Gecersiz durum degeri: \"%s\". Gecerli degerler: %s".formatted(durum, gecerliDegerler(TalepDurumu.class));
        }

        List<TalepOzeti> sonuc =
                talepService.talepleriGetir(durumEnum, anahtarKelime, mudurluk, mahalle, gunSayisi, null, limit);
        if (sonuc.isEmpty()) {
            return "Belirtilen kriterlere uyan talep bulunamadi.";
        }
        return sonuc.stream().map(this::ozetSatiri).collect(Collectors.joining("\n"));
    }

    @Tool(description = "Verilen takip numarasina ait talebin tum detaylarini ve gecmis notlarini getirir.")
    public String talepDetayGetir(
            @ToolParam(description = "Talebin takip numarasi, orn. 'TLP-2026-00001'.") String takipNo,
            ToolContext toolContext) {
        kaydetKullanilanArac(toolContext, "Talep detayı getirildi");
        return talepService.talepDetayGetir(takipNo)
                .map(this::detayMetni)
                .orElse("\"%s\" takip numarali talep bulunamadi.".formatted(takipNo));
    }

    @Tool(description = "Son N gundeki talep sayilarini durumlarina gore ozetler, istege bagli olarak tek bir mudurluge gore filtreler.")
    public String talepIstatistik(
            @ToolParam(description = "Istatistigin kapsayacagi gun sayisi, orn. 30.") int gunSayisi,
            @ToolParam(required = false, description = "Filtrelenecek mudurlugun tam adi. Bos birakilirsa tum mudurlukler dahil edilir.")
            String mudurluk,
            ToolContext toolContext) {
        kaydetKullanilanArac(toolContext, "İstatistik hesaplandı");
        TalepIstatistik istatistik = talepService.talepIstatistik(gunSayisi, mudurluk);
        String basaLik = istatistik.mudurlukAdi() == null
                ? "Son %d gun - tum mudurlukler".formatted(istatistik.gunSayisi())
                : "Son %d gun - %s".formatted(istatistik.gunSayisi(), istatistik.mudurlukAdi());

        String durumSatirlari = istatistik.durumDagilimi().entrySet().stream()
                .map(e -> "  - %s: %d".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));

        return "%s\nToplam: %d\n%s".formatted(basaLik, istatistik.toplamTalep(), durumSatirlari);
    }

    @Tool(description = "Tum aktif mudurlukleri ve sorumluluk alanlarini listeler. Bir talebi hangi mudurluge atayacagini belirlemeden once bunu kullan.")
    public String mudurlukleriListele(ToolContext toolContext) {
        kaydetKullanilanArac(toolContext, "Müdürlükler listelendi");
        List<MudurlukOzeti> mudurlukler = mudurlukService.mudurlukleriListele();
        return mudurlukler.stream()
                .map(m -> "- %s: %s".formatted(m.ad(), m.sorumlulukAlani()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = """
            Bir talebi belirtilen mudurluge atama TEKLIFI olusturur. Bu arac cagrildiginda
            atama HEMEN uygulanmaz - kullaniciya arayuzde Onayla/Iptal secenegi sunulur.
            Bu aracin donen metnini oldugu gibi kullaniciya ilet; kullanici ayrica "evet"
            derse bu araci TEKRAR CAGIRMA, onay islemi arayuzdeki buton uzerinden
            otomatik yapilacaktir.""")
    public String talebiMudurlugeAta(
            @ToolParam(description = "Atanacak talebin takip numarasi, orn. 'TLP-2026-00001'.") String takipNo,
            @ToolParam(description = "Talebin atanacagi mudurlugun tam adi. mudurlukleriListele araciyla dogru adi teyit et.")
            String mudurlukAdi,
            ToolContext toolContext) {
        try {
            PendingActionTeklifi teklif = talepService.mudurlugeAtaTeklifOlustur(takipNo, mudurlukAdi);
            return teklifOlustur(teklif, toolContext);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Tool(description = """
            Bir talebin durumunu guncelleme TEKLIFI olusturur. Bu arac cagrildiginda
            guncelleme HEMEN uygulanmaz - kullaniciya arayuzde Onayla/Iptal secenegi
            sunulur. Bu aracin donen metnini oldugu gibi kullaniciya ilet; kullanici
            ayrica "evet" derse bu araci TEKRAR CAGIRMA.""")
    public String talepDurumGuncelle(
            @ToolParam(description = "Guncellenecek talebin takip numarasi.") String takipNo,
            @ToolParam(description = "Yeni durum. Gecerli degerler: YENI, ATANDI, ISLEMDE, COZULDU, REDDEDILDI.")
            String yeniDurum,
            ToolContext toolContext) {
        TalepDurumu durumEnum;
        try {
            durumEnum = TalepDurumu.valueOf(yeniDurum.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Gecersiz durum degeri: \"%s\". Gecerli degerler: %s".formatted(yeniDurum, gecerliDegerler(TalepDurumu.class));
        }

        try {
            PendingActionTeklifi teklif = talepService.durumGuncelleTeklifOlustur(takipNo, durumEnum);
            return teklifOlustur(teklif, toolContext);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Tool(description = """
            Bir talebin onceligini guncelleme TEKLIFI olusturur. Bu arac cagrildiginda
            guncelleme HEMEN uygulanmaz - kullaniciya arayuzde Onayla/Iptal secenegi
            sunulur. Bu aracin donen metnini oldugu gibi kullaniciya ilet; kullanici
            ayrica "evet" derse bu araci TEKRAR CAGIRMA.""")
    public String talepOncelikGuncelle(
            @ToolParam(description = "Guncellenecek talebin takip numarasi.") String takipNo,
            @ToolParam(description = "Yeni oncelik. Gecerli degerler: DUSUK, NORMAL, YUKSEK, ACIL.")
            String yeniOncelik,
            ToolContext toolContext) {
        TalepOnceligi oncelikEnum;
        try {
            oncelikEnum = TalepOnceligi.valueOf(yeniOncelik.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Gecersiz oncelik degeri: \"%s\". Gecerli degerler: %s".formatted(yeniOncelik, gecerliDegerler(TalepOnceligi.class));
        }

        try {
            PendingActionTeklifi teklif = talepService.oncelikGuncelleTeklifOlustur(takipNo, oncelikEnum);
            return teklifOlustur(teklif, toolContext);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Tool(description = """
            Bir talebe serbest metin not ekleme TEKLIFI olusturur (islem gecmisine
            kaydedilecek). Bu arac cagrildiginda not HEMEN eklenmez - kullaniciya
            arayuzde Onayla/Iptal secenegi sunulur. Bu aracin donen metnini oldugu
            gibi kullaniciya ilet; kullanici ayrica "evet" derse bu araci TEKRAR
            CAGIRMA.""")
    public String talebeNotEkle(
            @ToolParam(description = "Not eklenecek talebin takip numarasi.") String takipNo,
            @ToolParam(description = "Eklenecek notun metni.") String notMetni,
            @ToolParam(required = false, description = "Notu ekleyen personelin adi. Belirtilmezse 'Bilinmiyor' olarak kaydedilir.")
            String personel,
            ToolContext toolContext) {
        try {
            PendingActionTeklifi teklif = talepService.notEkleTeklifOlustur(takipNo, notMetni, personel);
            return teklifOlustur(teklif, toolContext);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String teklifOlustur(PendingActionTeklifi teklif, ToolContext toolContext) {
        PendingAction action = pendingActionService.olustur(teklif);
        kaydetPendingActionId(toolContext, action.id());
        kaydetKullanilanArac(toolContext, teklifEtiketi(teklif.tur()));
        return "ONAY BEKLENIYOR: %s Bu ozeti kullaniciya ilet ve arayuzdeki Onayla/Iptal "
                .formatted(teklif.aciklama())
                + "secenegini kullanmasini soyle. Onay/iptal islemi otomatik olarak arayuz uzerinden yapilacak, "
                + "sen bir sey yapmana gerek yok.";
    }

    private String teklifEtiketi(PendingActionTuru tur) {
        return switch (tur) {
            case MUDURLUGE_ATA -> "Atama teklifi oluşturuldu";
            case DURUM_GUNCELLE -> "Durum güncelleme teklifi oluşturuldu";
            case ONCELIK_GUNCELLE -> "Öncelik güncelleme teklifi oluşturuldu";
            case NOT_EKLE -> "Not ekleme teklifi oluşturuldu";
        };
    }

    @SuppressWarnings("unchecked")
    private void kaydetPendingActionId(ToolContext toolContext, String id) {
        if (toolContext == null) {
            return;
        }
        Object sink = toolContext.getContext().get(PENDING_ACTION_ID_SINK);
        if (sink instanceof List<?> liste) {
            ((List<String>) liste).add(id);
        }
    }

    @SuppressWarnings("unchecked")
    private void kaydetKullanilanArac(ToolContext toolContext, String etiket) {
        if (toolContext == null) {
            return;
        }
        Object sink = toolContext.getContext().get(KULLANILAN_ARAC_SINK);
        if (sink instanceof List<?> liste) {
            ((List<String>) liste).add(etiket);
        }
    }

    private String ozetSatiri(TalepOzeti t) {
        return "- %s | %s | %s | %s | %s%s | %s".formatted(
                t.takipNo(),
                t.durum(),
                t.oncelik(),
                t.olusturmaTarihi().format(TARIH_BICIMI),
                t.mudurlukAdi() == null ? "(atanmamis)" : t.mudurlukAdi(),
                t.mahalle() == null ? "" : " - " + t.mahalle(),
                t.konuMetni());
    }

    private String detayMetni(TalepDetay d) {
        StringBuilder sb = new StringBuilder();
        sb.append("Takip No: %s\n".formatted(d.takipNo()));
        sb.append("Durum: %s | Oncelik: %s\n".formatted(d.durum(), d.oncelik()));
        sb.append("Mudurluk: %s\n".formatted(d.mudurlukAdi() == null ? "(atanmamis)" : d.mudurlukAdi()));
        sb.append("Mahalle: %s\n".formatted(d.mahalle() == null ? "-" : d.mahalle()));
        sb.append("Kategori: %s\n".formatted(d.kategori() == null ? "(siniflandirilmamis)" : d.kategori()));
        sb.append("Konu: %s\n".formatted(d.konuMetni()));
        sb.append("Vatandas: %s%s\n".formatted(
                d.vatandasAd() == null ? "-" : d.vatandasAd(),
                d.iletisim() == null ? "" : " (" + d.iletisim() + ")"));
        sb.append("Olusturma: %s\n".formatted(d.olusturmaTarihi().format(TARIH_BICIMI)));
        if (d.guncellemeTarihi() != null) {
            sb.append("Son Guncelleme: %s\n".formatted(d.guncellemeTarihi().format(TARIH_BICIMI)));
        }
        if (!d.notlar().isEmpty()) {
            sb.append("Notlar:\n");
            d.notlar().forEach(n -> sb.append("  [%s] %s: %s\n".formatted(n.tarih().format(TARIH_BICIMI), n.personel(), n.notu())));
        }
        return sb.toString();
    }

    private String gecerliDegerler(Class<? extends Enum<?>> enumSinifi) {
        return Arrays.stream(enumSinifi.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
