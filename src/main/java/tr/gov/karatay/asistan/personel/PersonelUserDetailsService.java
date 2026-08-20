package tr.gov.karatay.asistan.personel;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PersonelUserDetailsService implements UserDetailsService {

    private final PersonelRepository personelRepository;

    public PersonelUserDetailsService(PersonelRepository personelRepository) {
        this.personelRepository = personelRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String kullaniciAdi) throws UsernameNotFoundException {
        return personelRepository
                .findByKullaniciAdi(kullaniciAdi)
                .map(PersonelDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "\"%s\" kullanıcı adında bir personel bulunamadı.".formatted(kullaniciAdi)));
    }
}
