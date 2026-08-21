package tr.gov.karatay.asistan.talep;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.gov.karatay.asistan.common.enums.TalepDurumu;
import tr.gov.karatay.asistan.common.enums.TalepOnceligi;
import tr.gov.karatay.asistan.mudurluk.Mudurluk;
import tr.gov.karatay.asistan.mudurluk.MudurlukRepository;
import tr.gov.karatay.asistan.talep.dto.GunlukSayim;
import tr.gov.karatay.asistan.talep.dto.PendingActionTeklifi;
import tr.gov.karatay.asistan.talep.dto.TalepDetay;
import tr.gov.karatay.asistan.talep.dto.TalepIstatistik;
import tr.gov.karatay.asistan.talep.dto.TalepNotuOzeti;
import tr.gov.karatay.asistan.talep.dto.TalepOzeti;
import tr.gov.karatay.asistan.talep.dto.TopluIslemSonucu;

@Service
public class TalepService {

    private static final Logger log = LoggerFactory.getLogger(TalepService.class);
    private static final int LISTE_SERT_LIMIT = 20;
    private static final int TOPLU_ISLEM_SERT_LIMIT = 50;
    private static final int TREND_MAKS_GUN = 90;

    private final TalepRepository talepRepository;
    private final TalepNotuRepository talepNotuRepository;
    private final MudurlukRepository mudurlukRepository;

    public TalepService(
            TalepRepository talepRepository,
            TalepNotuRepository talepNotuRepository,
            MudurlukRepository mudurlukRepository) {
        this.talepRepository = talepRepository;
        this.talepNotuRepository = talepNotuRepository;
        this.mudurlukRepository = mudurlukRepository;
    }

    @Transactional(readOnly = true)
    public Optional<TalepDetay> talepDetayGetir(String takipNo) {
        return talepRepository.findByTakipNo(normalizeTakipNo(takipNo)).map(this::detayVer);
    }

    // Tek, birlesik listeleme/arama metodu. Onceden acikTalepleriListele ve
    // talepAra diye iki ayri metot/tool vardi; kucuk yerel model ikisi
    // arasinda hangisini sececegine guvenilir karar veremiyordu (bazen yanlis
    // araci cagiriyor, bazen "listele" kelimesini goruyor secim yapamayip
    // hayali parametreler uyduruyordu). Tek arac + "durum verilmezse acik
    // talepler" varsayilani bu secim sorununu koktan ortadan kaldiriyor.
    @Transactional(readOnly = true)
    public List<TalepOzeti> talepleriGetir(
            TalepDurumu durum,
            String anahtarKelime,
            String mudurlukAdi,
            String mahalle,
            Integer gunSayisi,
            Boolean sadeceAtanmamis,
            Integer limit) {
        Specification<Talep> spec = Specification.allOf(
                durum != null
                        ? (root, query, cb) -> cb.equal(root.get("durum"), durum)
                        : (root, query, cb) -> root.get("durum").in(TalepDurumu.YENI, TalepDurumu.ATANDI, TalepDurumu.ISLEMDE),
                anahtarKelimeSpec(anahtarKelime),
                mudurlukSpec(mudurlukAdi),
                mahalleSpec(mahalle),
                gunSayisiSpec(gunSayisi),
                atanmamisSpec(sadeceAtanmamis));

        return talepRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "olusturmaTarihi")).stream()
                .sorted(Comparator.comparing(Talep::getOncelik, Comparator.reverseOrder()))
                .limit(sertLimit(limit))
                .map(this::ozetleVer)
                .toList();
    }

    @Transactional(readOnly = true)
    public TalepIstatistik talepIstatistik(int gunSayisi, String mudurlukAdi) {
        Specification<Talep> spec = Specification.allOf(gunSayisiSpec(gunSayisi), mudurlukSpec(mudurlukAdi));
        List<Talep> sonuclar = talepRepository.findAll(spec);

        Map<String, Long> durumDagilimi = new LinkedHashMap<>();
        for (TalepDurumu d : TalepDurumu.values()) {
            durumDagilimi.put(d.name(), 0L);
        }
        for (Talep t : sonuclar) {
            durumDagilimi.merge(t.getDurum().name(), 1L, Long::sum);
        }

        return new TalepIstatistik(
                gunSayisi,
                mudurlukAdi,
                sonuclar.size(),
                durumDagilimi,
                gunlukTrendHesapla(sonuclar, gunSayisi),
                ortalamaCozumSuresiHesapla(sonuclar));
    }

    // Trend, sadece makul (<=90 gun) bir aralik icin hesaplanir - daha buyuk
    // bir aralikta gun basina cizgi grafik zaten okunakli olmaz. Talebin
    // olustugu HER gun (sonuc bos bile olsa) listede yer alir ki frontend'deki
    // grafik "bu gun hic talep yoktu" ile "bu gun hic sorgulanmadi" arasindaki
    // farki karistirmasin.
    private List<GunlukSayim> gunlukTrendHesapla(List<Talep> sonuclar, int gunSayisi) {
        if (gunSayisi <= 0 || gunSayisi > TREND_MAKS_GUN) {
            return List.of();
        }
        Map<LocalDate, Long> gunlukSayilar = new HashMap<>();
        for (Talep t : sonuclar) {
            gunlukSayilar.merge(t.getOlusturmaTarihi().toLocalDate(), 1L, Long::sum);
        }
        LocalDate bugun = LocalDate.now();
        List<GunlukSayim> trend = new ArrayList<>();
        for (int i = gunSayisi - 1; i >= 0; i--) {
            LocalDate gun = bugun.minusDays(i);
            trend.add(new GunlukSayim(gun, gunlukSayilar.getOrDefault(gun, 0L)));
        }
        return trend;
    }

    // Sadece guncellemeTarihi dolu (yani en az bir kez guncellenmis) COZULDU
    // kayitlar dahil edilir - olusturulur olusturulmaz COZULDU olarak eklenen
    // (guncellemeTarihi null) mock/seed veri, ortalamayi anlamsizca sifira
    // cekmesin diye disarida birakilir.
    private Double ortalamaCozumSuresiHesapla(List<Talep> sonuclar) {
        List<Talep> cozulenler = sonuclar.stream()
                .filter(t -> t.getDurum() == TalepDurumu.COZULDU && t.getGuncellemeTarihi() != null)
                .toList();
        if (cozulenler.isEmpty()) {
            return null;
        }
        double toplamSaat = cozulenler.stream()
                .mapToDouble(t -> Duration.between(t.getOlusturmaTarihi(), t.getGuncellemeTarihi()).toMinutes() / 60.0)
                .sum();
        return toplamSaat / cozulenler.size();
    }

    // Asagidaki dort "teklif olustur" metodu HICBIR SEY DEGISTIRMEZ - sadece
    // girdiyi dogrular (talep/mudurluk/enum degeri var mi) ve insanin
    // okuyabilecegi bir aciklama uretir. Gercek degisiklik, kullanici arayuzde
    // onay verdiginde PendingActionService uzerinden asagidaki mutasyon
    // metotlarindan biri cagrilarak yapilir.

    @Transactional(readOnly = true)
    public PendingActionTeklifi mudurlugeAtaTeklifOlustur(String takipNo, String mudurlukAdi) {
        Talep talep = talepGetirYoksaHataFirlat(takipNo);
        Mudurluk mudurluk = mudurlukRepository
                .findByAdIgnoreCase(mudurlukAdi)
                .orElseThrow(() -> new IllegalArgumentException(
                        "\"%s\" adında bir müdürlük bulunamadı.".formatted(mudurlukAdi)));

        String mevcut = talep.getMudurluk() == null ? "atanmamış" : talep.getMudurluk().getAd();
        String aciklama = "\"%s\" takip numaralı talebi \"%s\" müdürlüğüne atamak (mevcut: %s)."
                .formatted(talep.getTakipNo(), mudurluk.getAd(), mevcut);

        return new PendingActionTeklifi(
                PendingActionTuru.MUDURLUGE_ATA, talep.getTakipNo(), Map.of("mudurlukAdi", mudurluk.getAd()), aciklama);
    }

    @Transactional(readOnly = true)
    public PendingActionTeklifi durumGuncelleTeklifOlustur(String takipNo, TalepDurumu yeniDurum) {
        Talep talep = talepGetirYoksaHataFirlat(takipNo);
        String aciklama = "\"%s\" takip numaralı talebin durumunu \"%s\" -> \"%s\" olarak güncellemek."
                .formatted(talep.getTakipNo(), talep.getDurum(), yeniDurum);

        return new PendingActionTeklifi(
                PendingActionTuru.DURUM_GUNCELLE, talep.getTakipNo(), Map.of("yeniDurum", yeniDurum.name()), aciklama);
    }

    @Transactional(readOnly = true)
    public PendingActionTeklifi oncelikGuncelleTeklifOlustur(String takipNo, TalepOnceligi yeniOncelik) {
        Talep talep = talepGetirYoksaHataFirlat(takipNo);
        String aciklama = "\"%s\" takip numaralı talebin önceliğini \"%s\" -> \"%s\" olarak güncellemek."
                .formatted(talep.getTakipNo(), talep.getOncelik(), yeniOncelik);

        return new PendingActionTeklifi(
                PendingActionTuru.ONCELIK_GUNCELLE, talep.getTakipNo(), Map.of("yeniOncelik", yeniOncelik.name()), aciklama);
    }

    @Transactional(readOnly = true)
    public PendingActionTeklifi notEkleTeklifOlustur(String takipNo, String notMetni, String personel) {
        Talep talep = talepGetirYoksaHataFirlat(takipNo);
        String kaydedecekPersonel = personel == null || personel.isBlank() ? "Bilinmiyor" : personel;
        String aciklama = "\"%s\" takip numaralı talebe not eklemek: \"%s\" (ekleyen: %s)."
                .formatted(talep.getTakipNo(), notMetni, kaydedecekPersonel);

        return new PendingActionTeklifi(
                PendingActionTuru.NOT_EKLE,
                talep.getTakipNo(),
                Map.of("notMetni", notMetni, "personel", kaydedecekPersonel),
                aciklama);
    }

    @Transactional
    public TalepDetay talebiMudurlugeAta(String takipNo, String mudurlukAdi) {
        Talep talep = talepGetirYoksaHataFirlat(takipNo);
        Mudurluk mudurluk = mudurlukRepository
                .findByAdIgnoreCase(mudurlukAdi)
                .orElseThrow(() -> new IllegalArgumentException(
                        "\"%s\" adında bir müdürlük bulunamadı.".formatted(mudurlukAdi)));

        talep.setMudurluk(mudurluk);
        talep.setDurum(TalepDurumu.ATANDI);
        talep.setGuncellemeTarihi(LocalDateTime.now());
        talepRepository.save(talep);

        notEkle(talep, "AI Asistan", "Talep \"%s\" müdürlüğüne atandı.".formatted(mudurluk.getAd()));
        log.info("Talep mudurluge atandi: takipNo={}, mudurluk={}", talep.getTakipNo(), mudurluk.getAd());
        return detayVer(talep);
    }

    @Transactional
    public TalepDetay talepDurumGuncelle(String takipNo, TalepDurumu yeniDurum) {
        Talep talep = talepGetirYoksaHataFirlat(takipNo);
        TalepDurumu eskiDurum = talep.getDurum();
        talep.setDurum(yeniDurum);
        talep.setGuncellemeTarihi(LocalDateTime.now());
        talepRepository.save(talep);

        notEkle(talep, "AI Asistan", "Durum \"%s\" -> \"%s\" olarak güncellendi.".formatted(eskiDurum, yeniDurum));
        log.info("Talep durumu guncellendi: takipNo={}, {} -> {}", talep.getTakipNo(), eskiDurum, yeniDurum);
        return detayVer(talep);
    }

    @Transactional
    public TalepDetay talepOncelikGuncelle(String takipNo, TalepOnceligi yeniOncelik) {
        Talep talep = talepGetirYoksaHataFirlat(takipNo);
        TalepOnceligi eskiOncelik = talep.getOncelik();
        talep.setOncelik(yeniOncelik);
        talep.setGuncellemeTarihi(LocalDateTime.now());
        talepRepository.save(talep);

        notEkle(talep, "AI Asistan", "Öncelik \"%s\" -> \"%s\" olarak güncellendi.".formatted(eskiOncelik, yeniOncelik));
        log.info("Talep onceligi guncellendi: takipNo={}, {} -> {}", talep.getTakipNo(), eskiOncelik, yeniOncelik);
        return detayVer(talep);
    }

    @Transactional
    public TalepDetay talepKategoriGuncelle(String takipNo, String kategori) {
        Talep talep = talepGetirYoksaHataFirlat(takipNo);
        String eskiKategori = talep.getKategori();
        talep.setKategori(kategori);
        talep.setGuncellemeTarihi(LocalDateTime.now());
        talepRepository.save(talep);

        notEkle(
                talep,
                "AI Önerisi",
                "Kategori \"%s\" -> \"%s\" olarak güncellendi."
                        .formatted(eskiKategori == null ? "(boş)" : eskiKategori, kategori));
        log.info("Talep kategorisi guncellendi: takipNo={}, {} -> {}", talep.getTakipNo(), eskiKategori, kategori);
        return detayVer(talep);
    }

    @Transactional
    public TalepDetay talebeNotEkle(String takipNo, String notMetni, String personel) {
        Talep talep = talepGetirYoksaHataFirlat(takipNo);
        notEkle(talep, personel == null || personel.isBlank() ? "Bilinmiyor" : personel, notMetni);
        log.info("Talebe not eklendi: takipNo={}", talep.getTakipNo());
        return detayVer(talep);
    }

    // Toplu islemler dogrudan TalepController'dan (yonetim ekrani, kullanicinin
    // kendi sectigi kayitlar) cagrilir - PendingAction'a gerek yok, ayni
    // TalepController'daki tekli yazma uclari gibi (bkz. o siniftaki yorum).
    // Listedeki bir takipNo gecersizse/bulunamazsa TUM istek basarisiz olmaz -
    // her kayit kendi sonucunu (basarili/hata) doner, digerleri etkilenmez.
    @Transactional
    public List<TopluIslemSonucu> talepleriTopluDurumGuncelle(List<String> takipNolar, TalepDurumu yeniDurum) {
        return topluUygula(takipNolar, takipNo -> talepDurumGuncelle(takipNo, yeniDurum));
    }

    @Transactional
    public List<TopluIslemSonucu> talepleriTopluMudurlugeAta(List<String> takipNolar, String mudurlukAdi) {
        return topluUygula(takipNolar, takipNo -> talebiMudurlugeAta(takipNo, mudurlukAdi));
    }

    private List<TopluIslemSonucu> topluUygula(List<String> takipNolar, Consumer<String> islem) {
        return takipNolar.stream()
                .limit(TOPLU_ISLEM_SERT_LIMIT)
                .map(takipNo -> {
                    try {
                        islem.accept(takipNo);
                        return new TopluIslemSonucu(normalizeTakipNo(takipNo), true, null);
                    } catch (IllegalArgumentException e) {
                        return new TopluIslemSonucu(normalizeTakipNo(takipNo), false, e.getMessage());
                    }
                })
                .toList();
    }

    private void notEkle(Talep talep, String personel, String notMetni) {
        TalepNotu notu = new TalepNotu();
        notu.setTalep(talep);
        notu.setPersonel(personel);
        notu.setNotu(notMetni);
        notu.setTarih(LocalDateTime.now());
        talepNotuRepository.save(notu);
    }

    private Talep talepGetirYoksaHataFirlat(String takipNo) {
        return talepRepository
                .findByTakipNo(normalizeTakipNo(takipNo))
                .orElseThrow(() -> new IllegalArgumentException(
                        "\"%s\" takip numaralı talep bulunamadı.".formatted(takipNo)));
    }

    private Specification<Talep> mudurlukSpec(String mudurlukAdi) {
        if (mudurlukAdi == null || mudurlukAdi.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("mudurluk").get("ad")), mudurlukAdi.toLowerCase());
    }

    private Specification<Talep> mahalleSpec(String mahalle) {
        if (mahalle == null || mahalle.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("mahalle")), "%" + mahalle.toLowerCase() + "%");
    }

    private Specification<Talep> anahtarKelimeSpec(String anahtarKelime) {
        if (anahtarKelime == null || anahtarKelime.isBlank()) {
            return null;
        }
        String desen = "%" + anahtarKelime.toLowerCase() + "%";
        // takipNo da dahil - orn. "@" ile takip no otomatik tamamlamasi
        // (frontend) kullaniciyla ayni alani kullanabilsin diye (bkz.
        // ChatComposer.tsx). Once konu/kategori metniyle arama yapan bu
        // filtre, artik kismi takip no ile de eslesebiliyor.
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("konuMetni")), desen),
                cb.like(cb.lower(root.get("kategori")), desen),
                cb.like(cb.lower(root.get("takipNo")), desen));
    }

    private Specification<Talep> atanmamisSpec(Boolean sadeceAtanmamis) {
        if (sadeceAtanmamis == null || !sadeceAtanmamis) {
            return null;
        }
        return (root, query, cb) -> cb.isNull(root.get("mudurluk"));
    }

    private Specification<Talep> gunSayisiSpec(Integer gunSayisi) {
        if (gunSayisi == null || gunSayisi <= 0) {
            return null;
        }
        LocalDateTime esikTarih = LocalDateTime.now().minusDays(gunSayisi);
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("olusturmaTarihi"), esikTarih);
    }

    private int sertLimit(Integer istenen) {
        if (istenen == null || istenen <= 0) {
            return LISTE_SERT_LIMIT;
        }
        return Math.min(istenen, LISTE_SERT_LIMIT);
    }

    private String normalizeTakipNo(String takipNo) {
        return takipNo == null ? null : takipNo.trim().toUpperCase();
    }

    private TalepOzeti ozetleVer(Talep talep) {
        return new TalepOzeti(
                talep.getTakipNo(),
                talep.getVatandasAd(),
                talep.getMahalle(),
                talep.getKonuMetni(),
                talep.getKategori(),
                talep.getMudurluk() == null ? null : talep.getMudurluk().getAd(),
                talep.getDurum(),
                talep.getOncelik(),
                talep.getOlusturmaTarihi());
    }

    private TalepDetay detayVer(Talep talep) {
        List<TalepNotuOzeti> notlar = talepNotuRepository.findByTalepIdOrderByTarihDesc(talep.getId()).stream()
                .map(n -> new TalepNotuOzeti(n.getPersonel(), n.getNotu(), n.getTarih()))
                .toList();

        return new TalepDetay(
                talep.getTakipNo(),
                talep.getVatandasAd(),
                talep.getIletisim(),
                talep.getMahalle(),
                talep.getKonuMetni(),
                talep.getKategori(),
                talep.getMudurluk() == null ? null : talep.getMudurluk().getAd(),
                talep.getDurum(),
                talep.getOncelik(),
                talep.getOlusturmaTarihi(),
                talep.getGuncellemeTarihi(),
                notlar);
    }
}
