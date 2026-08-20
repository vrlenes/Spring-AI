package tr.gov.karatay.asistan.personel;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Gelistirme/staj ortami icin varsayilan bir personel olusturur (tablo boşsa).
// Sifre acik metin olarak BURADA DEGIL, sadece bu dosyada gorunur ve calisma
// zamaninda PasswordEncoder ile hash'lenerek kaydedilir - migration'a elle
// hesaplanmis bir BCrypt hash yazmaktan (kirilgan, dogrulamasi zor) daha
// guvenilir. GERCEK bir kuruma tasinirsa bu varsayilan kullanici/sifre
// MUTLAKA degistirilmeli/kaldirilmali.
@Component
public class PersonelSeeder implements CommandLineRunner {

    private static final String VARSAYILAN_KULLANICI_ADI = "admin";
    private static final String VARSAYILAN_SIFRE = "admin123";
    private static final String VARSAYILAN_AD_SOYAD = "Yönetici";

    private final PersonelRepository personelRepository;
    private final PasswordEncoder passwordEncoder;

    public PersonelSeeder(PersonelRepository personelRepository, PasswordEncoder passwordEncoder) {
        this.personelRepository = personelRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (personelRepository.existsByKullaniciAdi(VARSAYILAN_KULLANICI_ADI)) {
            return;
        }
        Personel personel = new Personel();
        personel.setKullaniciAdi(VARSAYILAN_KULLANICI_ADI);
        personel.setSifreHash(passwordEncoder.encode(VARSAYILAN_SIFRE));
        personel.setAdSoyad(VARSAYILAN_AD_SOYAD);
        personel.setOlusturmaTarihi(LocalDateTime.now());
        personelRepository.save(personel);
    }
}
