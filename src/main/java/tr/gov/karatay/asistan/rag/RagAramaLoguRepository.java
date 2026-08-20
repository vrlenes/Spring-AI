package tr.gov.karatay.asistan.rag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RagAramaLoguRepository extends JpaRepository<RagAramaLogu, Long> {

    List<RagAramaLogu> findTop200ByOrderByOlusturmaTarihiDesc();

    List<RagAramaLogu> findTop200BySonucSayisiOrderByOlusturmaTarihiDesc(int sonucSayisi);
}
