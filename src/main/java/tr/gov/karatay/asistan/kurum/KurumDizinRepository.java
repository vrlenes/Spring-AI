package tr.gov.karatay.asistan.kurum;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// JdbcTemplate ile "kurum" veritabanina (ayri DataSource, bkz.
// config/KurumDizinDataSourceConfig) erisir - JPA entity kullanilmiyor,
// cunku iki basit sorgu tablosu icin ikinci bir EntityManagerFactory/
// @EnableJpaRepositories seti gereksiz karmasiklik ve entity-scan
// cakisma riski getirirdi (bkz. plan).
@Repository
public class KurumDizinRepository {

    private final JdbcTemplate kurumJdbcTemplate;

    public KurumDizinRepository(@Qualifier("kurumJdbcTemplate") JdbcTemplate kurumJdbcTemplate) {
        this.kurumJdbcTemplate = kurumJdbcTemplate;
    }

    // unaccent(...) HER IKI tarafta da uygulanir: modelin urettigi arama
    // metni her zaman dogru aksanli Turkce olmayabilir (canli testte
    // yakalandi: "Fen Isleri" sorgusu "Fen İşleri Müdürlüğü" ile ILIKE ile
    // eslesmiyordu - model bulamayinca kendi genel bilgisinden yanlis bir
    // telefon/adres uydurmustu). unaccent extension'i V2 migration'da
    // etkinlestirildi.
    public List<MudurlukIletisim> mudurlukAra(String mudurlukAdi) {
        return kurumJdbcTemplate.query(
                "SELECT id, mudurluk_adi, telefon, eposta, adres FROM mudurluk_iletisim "
                        + "WHERE unaccent(mudurluk_adi) ILIKE unaccent(?) ORDER BY mudurluk_adi",
                (rs, i) -> new MudurlukIletisim(
                        rs.getLong("id"),
                        rs.getString("mudurluk_adi"),
                        rs.getString("telefon"),
                        rs.getString("eposta"),
                        rs.getString("adres")),
                "%" + mudurlukAdi + "%");
    }

    public List<PersonelDizinKaydi> personelAra(String sorgu) {
        return kurumJdbcTemplate.query(
                """
                SELECT id, ad_soyad, unvan, mudurluk_adi, telefon, eposta FROM personel_dizini
                WHERE unaccent(ad_soyad) ILIKE unaccent(?) OR unaccent(unvan) ILIKE unaccent(?) OR unaccent(mudurluk_adi) ILIKE unaccent(?)
                ORDER BY ad_soyad
                """,
                (rs, i) -> new PersonelDizinKaydi(
                        rs.getLong("id"),
                        rs.getString("ad_soyad"),
                        rs.getString("unvan"),
                        rs.getString("mudurluk_adi"),
                        rs.getString("telefon"),
                        rs.getString("eposta")),
                "%" + sorgu + "%", "%" + sorgu + "%", "%" + sorgu + "%");
    }
}
