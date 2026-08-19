package tr.gov.karatay.asistan.talep;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tr.gov.karatay.asistan.common.enums.TalepDurumu;
import tr.gov.karatay.asistan.talep.dto.TalepDetay;
import tr.gov.karatay.asistan.talep.dto.TalepOzeti;

import static org.assertj.core.api.Assertions.assertThat;

// TalepServiceTest'teki mock'lu unit testler is mantigini dogruluyordu; bu test
// GERCEK bir Postgres'e (Testcontainers - dev DB degil, izole/tek kullanimlik)
// karsi calisip Specification tabanli filtrelerin (talepleriGetir) gercekten
// dogru SQL'e cevrildigini dogruluyor. Flyway V1-V9 ayni sekilde calisip mock
// veriyi (V5-V9) yukluyor, o yuzden ek fixture eklemeye gerek yok - ama
// sonuclar SAYI degil YAPI (her sonucun beklenen alanlari tasidigi) uzerinden
// dogrulaniyor, boylece mock veri ileride degisirse test kirilgan olmaz.
@SpringBootTest
@Testcontainers
class TalepServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("belediye_asistan_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void veritabaniAyarlari(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TalepService talepService;

    @Test
    void durumFiltresiSadeceBelirtilenDurumdakiKayitlariGetirir() {
        List<TalepOzeti> sonuc = talepService.talepleriGetir(TalepDurumu.REDDEDILDI, null, null, null, null, null, null);

        assertThat(sonuc).isNotEmpty();
        assertThat(sonuc).allSatisfy(t -> assertThat(t.durum()).isEqualTo(TalepDurumu.REDDEDILDI));
    }

    @Test
    void durumBelirtilmezseSadeceAcikTaleplerGelir() {
        List<TalepOzeti> sonuc = talepService.talepleriGetir(null, null, null, null, null, null, null);

        assertThat(sonuc).isNotEmpty();
        assertThat(sonuc)
                .allSatisfy(t -> assertThat(t.durum())
                        .isIn(TalepDurumu.YENI, TalepDurumu.ATANDI, TalepDurumu.ISLEMDE));
    }

    @Test
    void atanmamisFiltresiSadeceMudurluksuzKayitlariGetirir() {
        List<TalepOzeti> sonuc = talepService.talepleriGetir(null, null, null, null, null, true, null);

        assertThat(sonuc).isNotEmpty();
        assertThat(sonuc).allSatisfy(t -> assertThat(t.mudurlukAdi()).isNull());
    }

    @Test
    void mudurlukFiltresiBuyukKucukHarfDuyarsizCalisir() {
        List<TalepOzeti> sonuc =
                talepService.talepleriGetir(null, null, "zabıta müdürlüğü", null, null, null, null);

        assertThat(sonuc).isNotEmpty();
        assertThat(sonuc).allSatisfy(t -> assertThat(t.mudurlukAdi()).isEqualTo("Zabıta Müdürlüğü"));
    }

    @Test
    void anahtarKelimeFiltresiKonuMetniniVeyaKategoriyiArar() {
        List<TalepOzeti> sonuc = talepService.talepleriGetir(null, "köpek", null, null, null, null, null);

        assertThat(sonuc).isNotEmpty();
        assertThat(sonuc)
                .allSatisfy(t -> assertThat(
                                t.konuMetni().toLowerCase().contains("köpek")
                                        || (t.kategori() != null && t.kategori().toLowerCase().contains("köpek")))
                        .isTrue());
    }

    @Test
    void gunSayisiFiltresiEskiKayitlariHaricTutar() {
        LocalDateTime esik = LocalDateTime.now().minusDays(7);

        List<TalepOzeti> sonuc = talepService.talepleriGetir(null, null, null, null, 7, null, null);

        assertThat(sonuc).allSatisfy(t -> assertThat(t.olusturmaTarihi()).isAfterOrEqualTo(esik));
    }

    @Test
    void sertLimitGercekVeritabaninaKarsiDaUygulanir() {
        List<TalepOzeti> sonuc = talepService.talepleriGetir(null, null, null, null, null, null, 1000);

        assertThat(sonuc).hasSizeLessThanOrEqualTo(20);
    }

    @Test
    void talepDetayGetir_gercekNotlariTarihSirasinaGoreGetirir() {
        // V6 seed'inde TLP-2026-00001 icin iki not var (bkz. migration dosyasi).
        var sonuc = talepService.talepDetayGetir("TLP-2026-00001");

        assertThat(sonuc).isPresent();
        TalepDetay detay = sonuc.get();
        assertThat(detay.notlar()).isNotEmpty();
        // En yeni not en basta olmali (findByTalepIdOrderByTarihDesc).
        for (int i = 0; i < detay.notlar().size() - 1; i++) {
            assertThat(detay.notlar().get(i).tarih()).isAfterOrEqualTo(detay.notlar().get(i + 1).tarih());
        }
    }
}
