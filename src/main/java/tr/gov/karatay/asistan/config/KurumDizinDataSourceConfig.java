package tr.gov.karatay.asistan.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

// Kurum dizini (mudurluk iletisim + personel dizini, bkz. kurum paketi) ayri
// bir veritabaninda tutuluyor - bu, ikinci bir DataSource bean'i gerektiriyor.
// Spring Boot'un DataSource otomatik yapilandirmasi @ConditionalOnMissingBean
// (DataSource.class) ile CONTEXT'teki HERHANGI bir DataSource turune bakiyor,
// sadece "primary" adli bean'e degil - yani ikinci bir DataSource eklendigi
// an, ana veritabani baglantisi da elle (bu sinifta, @Primary ile) tanimlanmak
// ZORUNDA, aksi halde Boot kendi otomatik ana DataSource'unu hic olusturmuyor
// ve butun uygulama (talep/sohbet/rag) calismiyor. Bu deger degismedi, sadece
// implicit'ten explicit'e tasindi (docs.spring.io'daki guncel coklu-DataSource
// deseni dogrulanarak yazildi).
@Configuration
public class KurumDizinDataSourceConfig {

    // kurumDataSourceProperties eklenince Boot'un kendi otomatik yapilandirdigi
    // DataSourceProperties bean'i ile birlikte context'te BIRDEN FAZLA
    // DataSourceProperties bean'i olusuyor - @Primary olmadan asagidaki
    // dataSource(...) metodunun parametresi belirsiz kalip
    // NoUniqueBeanDefinitionException atiyordu (canli testle yakalandi).
    // Kendi DataSourceProperties bean'imizi @Primary yaparak Boot'un kendi
    // otomatik olanini (@ConditionalOnMissingBean korumali) devre disi
    // birakiyoruz - deger degismiyor, sadece implicit'ten explicit'e tasiniyor.
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    // initializeDataSourceBuilder() Hikari'nin jdbcUrl/url takma ad
    // eslemesini dogru yapan, Boot'un kendi onerdigi yardimci metot.
    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    // KURUM DIZINI BUG'UNUN GERCEK KOK NEDENI (canli, cok derin bir teshis
    // surecinde bulundu - ham JDBC, duz JdbcTemplate, HikariCP'li JdbcTemplate
    // hepsi izole test edildi ve HEPSI DOGRU calisti; sadece TAM uygulama
    // context'inde RAG aramasi sessizce sifir sonuc donuyordu): asagidaki
    // kurumJdbcTemplate bean'i, Boot'un otomatik yapilandirdigi ana
    // JdbcTemplate bean'ini de (Flyway'de daha once yasanan @ConditionalOn
    // MissingBean sorunuyla AYNI SINIFTAN bir hata) SESSIZCE devre disi
    // birakiyordu - PgVectorStoreAutoConfiguration da JdbcTemplate'i TURE
    // GORE (qualifier olmadan) autowire ettigi icin, geriye kalan TEK
    // JdbcTemplate bean'i olan kurumJdbcTemplate'i (kurum_dizini DB'sine
    // bagli) yanlislikla RAG aramasi icin kullanmaya basladi - ana DB'de
    // arama yapmasi gerekirken kurum_dizini'nde (hicbir vector_store tablosu
    // olmayan) arama yapiyordu, bu yuzden HER ZAMAN sifir sonuc donuyordu.
    // Duzeltme: DataSource/DataSourceProperties'te oldugu gibi, kendi
    // JdbcTemplate'imizi de @Primary ile ACIKCA tanimlayip belirsizligi
    // ortadan kaldiriyoruz.
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConfigurationProperties("kurum.datasource")
    public DataSourceProperties kurumDataSourceProperties() {
        return new DataSourceProperties();
    }

    // Migrate() burada, DataSource bean'inin YAN ETKISI olarak cagriliyor -
    // KASITLI olarak ayri bir "Flyway" TIPINDE bir @Bean OLARAK KAYDEDILMIYOR.
    // Once tam da boyle bir bean denendi (initMethod="migrate" ile) ve Boot'un
    // birincil Flyway otomatik yapilandirmasini (@ConditionalOnMissingBean
    // (Flyway.class) - context'teki HERHANGI bir Flyway bean'ine bakiyor,
    // sadece "primary" olana degil) SESSIZCE DEVRE DISI BIRAKTIGI canli testle
    // yakalandi (ana DB hic migrate olmadan Hibernate "missing table [dokuman]"
    // hatasi verdi). Flyway nesnesi burada context'e hic kaydedilmeden, yerel
    // bir degisken olarak olusturulup migrate edilip atiliyor - Boot'un
    // birincil Flyway'i bundan tamamen habersiz, eskisi gibi calismaya devam
    // ediyor.
    @Bean
    public DataSource kurumDataSource(@Qualifier("kurumDataSourceProperties") DataSourceProperties kurumDataSourceProperties) {
        DataSource kurumDataSource = kurumDataSourceProperties.initializeDataSourceBuilder().build();
        Flyway.configure()
                .dataSource(kurumDataSource)
                .locations("classpath:db/kurum-migration")
                .load()
                .migrate();
        return kurumDataSource;
    }

    @Bean
    public JdbcTemplate kurumJdbcTemplate(@Qualifier("kurumDataSource") DataSource kurumDataSource) {
        return new JdbcTemplate(kurumDataSource);
    }
}
