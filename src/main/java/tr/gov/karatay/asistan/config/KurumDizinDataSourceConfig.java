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
