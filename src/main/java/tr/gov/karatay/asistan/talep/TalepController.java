package tr.gov.karatay.asistan.talep;

import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tr.gov.karatay.asistan.common.enums.TalepDurumu;
import tr.gov.karatay.asistan.common.enums.TalepOnceligi;
import tr.gov.karatay.asistan.talep.dto.ResmiYaziTaslagi;
import tr.gov.karatay.asistan.talep.dto.SiniflandirmaOnerisi;
import tr.gov.karatay.asistan.talep.dto.TalepDetay;
import tr.gov.karatay.asistan.talep.dto.TalepIstatistik;
import tr.gov.karatay.asistan.talep.dto.TalepOzeti;
import tr.gov.karatay.asistan.talep.dto.TopluSiniflandirmaOnerisi;

// Sohbetten bagimsiz, dogrudan REST uzerinden calisan talep yonetim ekrani icin.
// Chat'teki tool-calling akisinin aksine, buradaki sonuclar LLM'in yazdigi metne
// degil dogrudan bu endpoint'lerin donduugu yapisal JSON'a dayanir - kucuk yerel
// modelin liste sonuclarini bazen yanlis aktarabildigi (test edilerek gorulen)
// bir sinirlama bu ekranda hic devreye girmez. Yazma islemleri de burada dogrudan
// uygulanir (PendingAction'a gerek yok) - cunku aksiyonu baslatan zaten LLM degil,
// formu dolduran kullanicinin kendisi.
@RestController
@RequestMapping("/api/talepler")
public class TalepController {

    private final TalepService talepService;
    private final TalepOneriService talepOneriService;
    private final ResmiYaziService resmiYaziService;

    public TalepController(
            TalepService talepService, TalepOneriService talepOneriService, ResmiYaziService resmiYaziService) {
        this.talepService = talepService;
        this.talepOneriService = talepOneriService;
        this.resmiYaziService = resmiYaziService;
    }

    @GetMapping
    public List<TalepOzeti> talepleriGetir(
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String anahtarKelime,
            @RequestParam(required = false) String mudurluk,
            @RequestParam(required = false) String mahalle,
            @RequestParam(required = false) Integer gunSayisi,
            @RequestParam(required = false) Boolean atanmamis,
            @RequestParam(required = false) Integer limit) {
        TalepDurumu durumEnum = durumCoz(durum);
        return talepService.talepleriGetir(durumEnum, anahtarKelime, mudurluk, mahalle, gunSayisi, atanmamis, limit);
    }

    @GetMapping("/{takipNo}/ai-oneri")
    public SiniflandirmaOnerisi aiOnerisi(@PathVariable String takipNo) {
        return talepOneriService.oneriOlustur(takipNo);
    }

    @GetMapping("/ai-oneri-toplu")
    public List<TopluSiniflandirmaOnerisi> topluAiOnerisi(@RequestParam(required = false) Integer limit) {
        return talepOneriService.topluOneriOlustur(limit);
    }

    @GetMapping("/{takipNo}/resmi-yazi")
    public ResmiYaziTaslagi resmiYaziTaslagi(@PathVariable String takipNo) {
        return resmiYaziService.taslakOlustur(takipNo);
    }

    @GetMapping("/istatistik")
    public TalepIstatistik istatistik(
            @RequestParam(defaultValue = "30") int gunSayisi, @RequestParam(required = false) String mudurluk) {
        return talepService.talepIstatistik(gunSayisi, mudurluk);
    }

    @GetMapping("/{takipNo}")
    public ResponseEntity<TalepDetay> detayGetir(@PathVariable String takipNo) {
        return talepService
                .talepDetayGetir(takipNo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{takipNo}/mudurluk")
    public TalepDetay mudurlugeAta(@PathVariable String takipNo, @RequestBody MudurlukAtaIstek istek) {
        return talepService.talebiMudurlugeAta(takipNo, istek.mudurlukAdi());
    }

    @PostMapping("/{takipNo}/durum")
    public TalepDetay durumGuncelle(@PathVariable String takipNo, @RequestBody DurumGuncelleIstek istek) {
        return talepService.talepDurumGuncelle(takipNo, TalepDurumu.valueOf(istek.durum().trim().toUpperCase(Locale.ROOT)));
    }

    @PostMapping("/{takipNo}/oncelik")
    public TalepDetay oncelikGuncelle(@PathVariable String takipNo, @RequestBody OncelikGuncelleIstek istek) {
        return talepService.talepOncelikGuncelle(
                takipNo, TalepOnceligi.valueOf(istek.oncelik().trim().toUpperCase(Locale.ROOT)));
    }

    @PostMapping("/{takipNo}/kategori")
    public TalepDetay kategoriGuncelle(@PathVariable String takipNo, @RequestBody KategoriGuncelleIstek istek) {
        return talepService.talepKategoriGuncelle(takipNo, istek.kategori());
    }

    @PostMapping("/{takipNo}/notlar")
    public TalepDetay notEkle(@PathVariable String takipNo, @RequestBody NotEkleIstek istek) {
        return talepService.talebeNotEkle(takipNo, istek.notMetni(), istek.personel());
    }

    private TalepDurumu durumCoz(String durum) {
        return durum == null || durum.isBlank() ? null : TalepDurumu.valueOf(durum.trim().toUpperCase(Locale.ROOT));
    }

    public record MudurlukAtaIstek(String mudurlukAdi) {
    }

    public record DurumGuncelleIstek(String durum) {
    }

    public record OncelikGuncelleIstek(String oncelik) {
    }

    public record KategoriGuncelleIstek(String kategori) {
    }

    public record NotEkleIstek(String notMetni, String personel) {
    }
}
