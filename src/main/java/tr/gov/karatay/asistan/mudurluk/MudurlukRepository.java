package tr.gov.karatay.asistan.mudurluk;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MudurlukRepository extends JpaRepository<Mudurluk, Long> {

    Optional<Mudurluk> findByAdIgnoreCase(String ad);

    List<Mudurluk> findByAktifTrue();
}
