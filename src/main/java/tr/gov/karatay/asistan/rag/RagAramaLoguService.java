package tr.gov.karatay.asistan.rag;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.gov.karatay.asistan.common.enums.SohbetModu;
import tr.gov.karatay.asistan.rag.dto.RagAramaLoguOzeti;

@Service
public class RagAramaLoguService {

    private final RagAramaLoguRepository ragAramaLoguRepository;

    public RagAramaLoguService(RagAramaLoguRepository ragAramaLoguRepository) {
        this.ragAramaLoguRepository = ragAramaLoguRepository;
    }

    @Transactional
    public void kaydet(SohbetModu mod, String sorgu, int sonucSayisi, Double enIyiBenzerlik) {
        RagAramaLogu kayit = new RagAramaLogu();
        kayit.setMod(mod == null ? null : mod.name());
        kayit.setSorgu(sorgu);
        kayit.setSonucSayisi(sonucSayisi);
        kayit.setEnIyiBenzerlik(enIyiBenzerlik);
        kayit.setOlusturmaTarihi(LocalDateTime.now());
        ragAramaLoguRepository.save(kayit);
    }

    @Transactional(readOnly = true)
    public List<RagAramaLoguOzeti> sonAramalariGetir(boolean sadeceIskalamalar) {
        List<RagAramaLogu> kayitlar = sadeceIskalamalar
                ? ragAramaLoguRepository.findTop200BySonucSayisiOrderByOlusturmaTarihiDesc(0)
                : ragAramaLoguRepository.findTop200ByOrderByOlusturmaTarihiDesc();
        return kayitlar.stream()
                .map(k -> new RagAramaLoguOzeti(
                        k.getId(), k.getMod(), k.getSorgu(), k.getSonucSayisi(), k.getEnIyiBenzerlik(), k.getOlusturmaTarihi()))
                .toList();
    }
}
