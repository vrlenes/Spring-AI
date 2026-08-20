package tr.gov.karatay.asistan.sohbet;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SohbetMesajiRepository extends JpaRepository<SohbetMesaji, Long> {

    List<SohbetMesaji> findBySohbetIdOrderByOlusturmaTarihiAsc(String sohbetId);
}
