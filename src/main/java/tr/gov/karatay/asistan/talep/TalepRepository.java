package tr.gov.karatay.asistan.talep;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TalepRepository extends JpaRepository<Talep, Long>, JpaSpecificationExecutor<Talep> {

    Optional<Talep> findByTakipNo(String takipNo);

    boolean existsByTakipNo(String takipNo);
}
