package tr.gov.karatay.asistan.sohbet;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SohbetRepository extends JpaRepository<Sohbet, String> {

    List<Sohbet> findByPersonelIdOrderByGuncellemeTarihiDesc(Long personelId);

    Optional<Sohbet> findByIdAndPersonelId(String id, Long personelId);
}
