package tr.gov.karatay.asistan.personel;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// Spring Security'nin UserDetails'i, Personel entity'sini dogrudan sarmalar - boylece
// oturum acmis kullanicinin id/adSoyad bilgisine (SohbetService icin gereken) sadece
// kullanici adindan degil, tam entity'den erisilebilir (bkz. AuthController, ChatController).
public class PersonelDetails implements UserDetails {

    private final Personel personel;

    public PersonelDetails(Personel personel) {
        this.personel = personel;
    }

    public Personel getPersonel() {
        return personel;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_PERSONEL"));
    }

    @Override
    public String getPassword() {
        return personel.getSifreHash();
    }

    @Override
    public String getUsername() {
        return personel.getKullaniciAdi();
    }
}
