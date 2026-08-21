package tr.gov.karatay.asistan.talep;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import tr.gov.karatay.asistan.common.enums.TalepDurumu;
import tr.gov.karatay.asistan.common.enums.TalepOnceligi;
import tr.gov.karatay.asistan.mudurluk.Mudurluk;
import tr.gov.karatay.asistan.mudurluk.MudurlukRepository;
import tr.gov.karatay.asistan.talep.dto.PendingActionTeklifi;
import tr.gov.karatay.asistan.talep.dto.TalepDetay;
import tr.gov.karatay.asistan.talep.dto.TalepIstatistik;
import tr.gov.karatay.asistan.talep.dto.TalepOzeti;
import tr.gov.karatay.asistan.talep.dto.TopluIslemSonucu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TalepService, LLM'den tamamen bagimsiz saf is mantigi - CLAUDE.md'nin
// "*Tools sinifi ince sarmalayici olmali" kuralinin tam da bunu saglamak
// icin var oldugunu dogrulayan testler. Repository'ler mocklaniyor, gercek
// veritabani/Ollama gerekmiyor - saniyeler icinde calisir.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TalepServiceTest {

    @Mock
    private TalepRepository talepRepository;

    @Mock
    private TalepNotuRepository talepNotuRepository;

    @Mock
    private MudurlukRepository mudurlukRepository;

    private TalepService talepService;

    @BeforeEach
    void hazirla() {
        talepService = new TalepService(talepRepository, talepNotuRepository, mudurlukRepository);
        when(talepNotuRepository.findByTalepIdOrderByTarihDesc(anyLong())).thenReturn(List.of());
    }

    private Talep ornekTalep(String takipNo, TalepOnceligi oncelik, TalepDurumu durum) {
        Talep talep = new Talep();
        talep.setId(1L);
        talep.setTakipNo(takipNo);
        talep.setKonuMetni("Örnek konu metni");
        talep.setDurum(durum);
        talep.setOncelik(oncelik);
        talep.setOlusturmaTarihi(LocalDateTime.now());
        return talep;
    }

    private Mudurluk ornekMudurluk(String ad) {
        Mudurluk mudurluk = new Mudurluk();
        mudurluk.setId(1L);
        mudurluk.setAd(ad);
        mudurluk.setSorumlulukAlani("Test sorumluluk alanı");
        mudurluk.setAktif(true);
        return mudurluk;
    }

    @Nested
    class TalepDetayGetirTestleri {

        @Test
        void bulunanTalepIcinDoluOptionalDoner() {
            Talep talep = ornekTalep("TLP-2026-00001", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            when(talepRepository.findByTakipNo("TLP-2026-00001")).thenReturn(Optional.of(talep));

            Optional<TalepDetay> sonuc = talepService.talepDetayGetir("tlp-2026-00001");

            assertThat(sonuc).isPresent();
            assertThat(sonuc.get().takipNo()).isEqualTo("TLP-2026-00001");
        }

        @Test
        void takipNoBuyukKucukHarfDuyarsizAranir() {
            when(talepRepository.findByTakipNo("TLP-2026-00001")).thenReturn(Optional.empty());

            talepService.talepDetayGetir("  tlp-2026-00001  ");

            verify(talepRepository).findByTakipNo("TLP-2026-00001");
        }

        @Test
        void bulunamayanTalepIcinBosOptionalDoner() {
            when(talepRepository.findByTakipNo(any())).thenReturn(Optional.empty());

            assertThat(talepService.talepDetayGetir("TLP-2026-99999")).isEmpty();
        }
    }

    @Nested
    class TalepleriGetirTestleri {

        @Test
        void sonuclarOncelikSirasinaGoreSiralanir() {
            Talep dusuk = ornekTalep("TLP-1", TalepOnceligi.DUSUK, TalepDurumu.YENI);
            Talep acil = ornekTalep("TLP-2", TalepOnceligi.ACIL, TalepDurumu.YENI);
            Talep normal = ornekTalep("TLP-3", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            // Repository'den kasten oncelik sirasina uymayan bir sirada donuyoruz.
            when(talepRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(dusuk, acil, normal));

            List<TalepOzeti> sonuc = talepService.talepleriGetir(null, null, null, null, null, null, null);

            assertThat(sonuc).extracting(TalepOzeti::takipNo).containsExactly("TLP-2", "TLP-3", "TLP-1");
        }

        @Test
        void limitBelirtilmezseVarsayilanYirmiKullanilir() {
            List<Talep> yirmiBesKayit = java.util.stream.IntStream.range(0, 25)
                    .mapToObj(i -> ornekTalep("TLP-" + i, TalepOnceligi.NORMAL, TalepDurumu.YENI))
                    .toList();
            when(talepRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(yirmiBesKayit);

            List<TalepOzeti> sonuc = talepService.talepleriGetir(null, null, null, null, null, null, null);

            assertThat(sonuc).hasSize(20);
        }

        @Test
        void istenenLimitYirmiyiGeciyorsaYirmiyeSinirlandirilir() {
            List<Talep> yirmiBesKayit = java.util.stream.IntStream.range(0, 25)
                    .mapToObj(i -> ornekTalep("TLP-" + i, TalepOnceligi.NORMAL, TalepDurumu.YENI))
                    .toList();
            when(talepRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(yirmiBesKayit);

            List<TalepOzeti> sonuc = talepService.talepleriGetir(null, null, null, null, null, null, 100);

            assertThat(sonuc).hasSize(20);
        }

        @Test
        void istenenLimitYirminAltindaysaOldugoGibiKullanilir() {
            List<Talep> besKayit = java.util.stream.IntStream.range(0, 5)
                    .mapToObj(i -> ornekTalep("TLP-" + i, TalepOnceligi.NORMAL, TalepDurumu.YENI))
                    .toList();
            when(talepRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(besKayit);

            List<TalepOzeti> sonuc = talepService.talepleriGetir(null, null, null, null, null, null, 3);

            assertThat(sonuc).hasSize(3);
        }
    }

    @Nested
    class TalepIstatistikTestleri {

        @Test
        void tumDurumlarSifirdanBaslarVeDogruSayilir() {
            List<Talep> kayitlar = List.of(
                    ornekTalep("TLP-1", TalepOnceligi.NORMAL, TalepDurumu.YENI),
                    ornekTalep("TLP-2", TalepOnceligi.NORMAL, TalepDurumu.YENI),
                    ornekTalep("TLP-3", TalepOnceligi.NORMAL, TalepDurumu.COZULDU));
            when(talepRepository.findAll(any(Specification.class))).thenReturn(kayitlar);

            TalepIstatistik istatistik = talepService.talepIstatistik(30, null);

            assertThat(istatistik.toplamTalep()).isEqualTo(3);
            assertThat(istatistik.durumDagilimi())
                    .containsEntry("YENI", 2L)
                    .containsEntry("COZULDU", 1L)
                    .containsEntry("REDDEDILDI", 0L)
                    .containsEntry("ATANDI", 0L)
                    .containsEntry("ISLEMDE", 0L);
        }

        @Test
        void gunlukTrend_istenenGunSayisiKadarKayitDonerVeBugunuDogruSayar() {
            Talep bugunTalebi = ornekTalep("TLP-1", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            bugunTalebi.setOlusturmaTarihi(LocalDateTime.now());
            when(talepRepository.findAll(any(Specification.class))).thenReturn(List.of(bugunTalebi));

            TalepIstatistik istatistik = talepService.talepIstatistik(7, null);

            assertThat(istatistik.gunlukTrend()).hasSize(7);
            assertThat(istatistik.gunlukTrend().get(6).tarih()).isEqualTo(java.time.LocalDate.now());
            assertThat(istatistik.gunlukTrend().get(6).sayi()).isEqualTo(1L);
            assertThat(istatistik.gunlukTrend().get(0).sayi()).isEqualTo(0L);
        }

        @Test
        void gunlukTrend_doksanGundenFazlaIstenirseBosDoner() {
            when(talepRepository.findAll(any(Specification.class))).thenReturn(List.of());

            TalepIstatistik istatistik = talepService.talepIstatistik(180, null);

            assertThat(istatistik.gunlukTrend()).isEmpty();
        }

        @Test
        void ortalamaCozumSuresi_guncellemeTarihiOlanCozulenlerdenHesaplanir() {
            Talep cozulenBir = ornekTalep("TLP-1", TalepOnceligi.NORMAL, TalepDurumu.COZULDU);
            LocalDateTime simdi = LocalDateTime.now();
            cozulenBir.setOlusturmaTarihi(simdi.minusHours(10));
            cozulenBir.setGuncellemeTarihi(simdi);

            Talep guncellemesizCozulen = ornekTalep("TLP-2", TalepOnceligi.NORMAL, TalepDurumu.COZULDU);
            guncellemesizCozulen.setGuncellemeTarihi(null);

            when(talepRepository.findAll(any(Specification.class))).thenReturn(List.of(cozulenBir, guncellemesizCozulen));

            TalepIstatistik istatistik = talepService.talepIstatistik(30, null);

            assertThat(istatistik.ortalamaCozumSuresiSaat()).isEqualTo(10.0);
        }

        @Test
        void ortalamaCozumSuresi_cozulenYoksaNullDoner() {
            when(talepRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(ornekTalep("TLP-1", TalepOnceligi.NORMAL, TalepDurumu.YENI)));

            TalepIstatistik istatistik = talepService.talepIstatistik(30, null);

            assertThat(istatistik.ortalamaCozumSuresiSaat()).isNull();
        }
    }

    @Nested
    class TeklifOlusturmaTestleri {

        @Test
        void mudurlugeAtaTeklifiHicbirSeyiDegistirmez() {
            Talep talep = ornekTalep("TLP-2026-00001", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            Mudurluk mudurluk = ornekMudurluk("Fen İşleri Müdürlüğü");
            when(talepRepository.findByTakipNo("TLP-2026-00001")).thenReturn(Optional.of(talep));
            when(mudurlukRepository.findByAdIgnoreCase("Fen İşleri Müdürlüğü")).thenReturn(Optional.of(mudurluk));

            PendingActionTeklifi teklif = talepService.mudurlugeAtaTeklifOlustur("TLP-2026-00001", "Fen İşleri Müdürlüğü");

            assertThat(teklif.tur()).isEqualTo(PendingActionTuru.MUDURLUGE_ATA);
            assertThat(teklif.parametreler()).containsEntry("mudurlukAdi", "Fen İşleri Müdürlüğü");
            verify(talepRepository, never()).save(any());
            verify(talepNotuRepository, never()).save(any());
        }

        @Test
        void mudurlugeAtaTeklifi_talepBulunamazsaHataFirlatir() {
            when(talepRepository.findByTakipNo(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> talepService.mudurlugeAtaTeklifOlustur("TLP-2026-99999", "Fen İşleri Müdürlüğü"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TLP-2026-99999");
        }

        @Test
        void mudurlugeAtaTeklifi_mudurlukBulunamazsaHataFirlatir() {
            Talep talep = ornekTalep("TLP-2026-00001", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            when(talepRepository.findByTakipNo(any())).thenReturn(Optional.of(talep));
            when(mudurlukRepository.findByAdIgnoreCase(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> talepService.mudurlugeAtaTeklifOlustur("TLP-2026-00001", "Olmayan Müdürlük"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Olmayan Müdürlük");
        }

        @Test
        void durumGuncelleTeklifiHicbirSeyiDegistirmez() {
            Talep talep = ornekTalep("TLP-2026-00001", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            when(talepRepository.findByTakipNo(any())).thenReturn(Optional.of(talep));

            PendingActionTeklifi teklif = talepService.durumGuncelleTeklifOlustur("TLP-2026-00001", TalepDurumu.ISLEMDE);

            assertThat(teklif.parametreler()).containsEntry("yeniDurum", "ISLEMDE");
            verify(talepRepository, never()).save(any());
        }
    }

    @Nested
    class MutasyonTestleri {

        @Test
        void talebiMudurlugeAta_durumuAtandiYaparVeNotEkler() {
            Talep talep = ornekTalep("TLP-2026-00001", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            Mudurluk mudurluk = ornekMudurluk("Fen İşleri Müdürlüğü");
            when(talepRepository.findByTakipNo(any())).thenReturn(Optional.of(talep));
            when(mudurlukRepository.findByAdIgnoreCase(any())).thenReturn(Optional.of(mudurluk));

            TalepDetay sonuc = talepService.talebiMudurlugeAta("TLP-2026-00001", "Fen İşleri Müdürlüğü");

            assertThat(sonuc.durum()).isEqualTo(TalepDurumu.ATANDI);
            assertThat(sonuc.mudurlukAdi()).isEqualTo("Fen İşleri Müdürlüğü");
            verify(talepRepository, times(1)).save(talep);

            ArgumentCaptor<TalepNotu> notCaptor = ArgumentCaptor.forClass(TalepNotu.class);
            verify(talepNotuRepository).save(notCaptor.capture());
            assertThat(notCaptor.getValue().getPersonel()).isEqualTo("AI Asistan");
        }

        @Test
        void talepDurumGuncelle_eskiVeYeniDurumuNotaYazar() {
            Talep talep = ornekTalep("TLP-2026-00001", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            when(talepRepository.findByTakipNo(any())).thenReturn(Optional.of(talep));

            talepService.talepDurumGuncelle("TLP-2026-00001", TalepDurumu.COZULDU);

            ArgumentCaptor<TalepNotu> notCaptor = ArgumentCaptor.forClass(TalepNotu.class);
            verify(talepNotuRepository).save(notCaptor.capture());
            assertThat(notCaptor.getValue().getNotu()).contains("YENI").contains("COZULDU");
        }

        @Test
        void talebeNotEkle_personelBosVerilirseBilinmiyorYazilir() {
            Talep talep = ornekTalep("TLP-2026-00001", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            when(talepRepository.findByTakipNo(any())).thenReturn(Optional.of(talep));

            talepService.talebeNotEkle("TLP-2026-00001", "Saha kontrolü yapıldı", "  ");

            ArgumentCaptor<TalepNotu> notCaptor = ArgumentCaptor.forClass(TalepNotu.class);
            verify(talepNotuRepository).save(notCaptor.capture());
            assertThat(notCaptor.getValue().getPersonel()).isEqualTo("Bilinmiyor");
            assertThat(notCaptor.getValue().getNotu()).isEqualTo("Saha kontrolü yapıldı");
        }

        @Test
        void talepKategoriGuncelle_eskiKategoriYoksaBosYazilir() {
            Talep talep = ornekTalep("TLP-2026-00001", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            talep.setKategori(null);
            when(talepRepository.findByTakipNo(any())).thenReturn(Optional.of(talep));

            talepService.talepKategoriGuncelle("TLP-2026-00001", "Yol Hasarı");

            ArgumentCaptor<TalepNotu> notCaptor = ArgumentCaptor.forClass(TalepNotu.class);
            verify(talepNotuRepository).save(notCaptor.capture());
            assertThat(notCaptor.getValue().getNotu()).contains("(boş)").contains("Yol Hasarı");
            assertThat(talep.getKategori()).isEqualTo("Yol Hasarı");
        }

        @Test
        void mutasyonMetodu_talepBulunamazsaHataFirlaturVeHicbirSeyKaydetmez() {
            when(talepRepository.findByTakipNo(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> talepService.talepDurumGuncelle("TLP-2026-99999", TalepDurumu.COZULDU))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(talepRepository, never()).save(any());
            verify(talepNotuRepository, never()).save(any());
        }
    }

    @Nested
    class TopluIslemTestleri {

        @Test
        void topluDurumGuncelle_hepsiBulunursaHepsiBasarili() {
            Talep birinci = ornekTalep("TLP-1", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            Talep ikinci = ornekTalep("TLP-2", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            when(talepRepository.findByTakipNo("TLP-1")).thenReturn(Optional.of(birinci));
            when(talepRepository.findByTakipNo("TLP-2")).thenReturn(Optional.of(ikinci));

            List<TopluIslemSonucu> sonuc =
                    talepService.talepleriTopluDurumGuncelle(List.of("TLP-1", "TLP-2"), TalepDurumu.ISLEMDE);

            assertThat(sonuc).hasSize(2);
            assertThat(sonuc).allMatch(TopluIslemSonucu::basarili);
            verify(talepRepository, times(2)).save(any());
        }

        @Test
        void topluDurumGuncelle_bulunamayanKayitDigerlerinEtkilemez() {
            Talep bulunan = ornekTalep("TLP-1", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            when(talepRepository.findByTakipNo("TLP-1")).thenReturn(Optional.of(bulunan));
            when(talepRepository.findByTakipNo("TLP-99999")).thenReturn(Optional.empty());

            List<TopluIslemSonucu> sonuc =
                    talepService.talepleriTopluDurumGuncelle(List.of("TLP-1", "TLP-99999"), TalepDurumu.ISLEMDE);

            assertThat(sonuc).hasSize(2);
            assertThat(sonuc).filteredOn(TopluIslemSonucu::takipNo, "TLP-1").first().satisfies(
                    s -> assertThat(s.basarili()).isTrue());
            assertThat(sonuc).filteredOn(TopluIslemSonucu::takipNo, "TLP-99999").first().satisfies(s -> {
                assertThat(s.basarili()).isFalse();
                assertThat(s.hata()).contains("TLP-99999");
            });
            verify(talepRepository, times(1)).save(any());
        }

        @Test
        void topluDurumGuncelle_ellidenFazlaKayitSertLimiteKesilir() {
            Talep talep = ornekTalep("TLP-X", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            when(talepRepository.findByTakipNo(any())).thenReturn(Optional.of(talep));
            List<String> altmisKayit =
                    java.util.stream.IntStream.range(0, 60).mapToObj(i -> "TLP-" + i).toList();

            List<TopluIslemSonucu> sonuc = talepService.talepleriTopluDurumGuncelle(altmisKayit, TalepDurumu.ISLEMDE);

            assertThat(sonuc).hasSize(50);
        }

        @Test
        void topluMudurlugeAta_mudurlukBulunamazsaOKaydinSonucuBasarisiz() {
            Talep talep = ornekTalep("TLP-1", TalepOnceligi.NORMAL, TalepDurumu.YENI);
            when(talepRepository.findByTakipNo("TLP-1")).thenReturn(Optional.of(talep));
            when(mudurlukRepository.findByAdIgnoreCase("Olmayan Müdürlük")).thenReturn(Optional.empty());

            List<TopluIslemSonucu> sonuc =
                    talepService.talepleriTopluMudurlugeAta(List.of("TLP-1"), "Olmayan Müdürlük");

            assertThat(sonuc).hasSize(1);
            assertThat(sonuc.get(0).basarili()).isFalse();
            verify(talepRepository, never()).save(any());
        }
    }
}
