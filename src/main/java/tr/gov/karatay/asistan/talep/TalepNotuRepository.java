package tr.gov.karatay.asistan.talep;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TalepNotuRepository extends JpaRepository<TalepNotu, Long> {

    List<TalepNotu> findByTalepIdOrderByTarihDesc(Long talepId);
}
