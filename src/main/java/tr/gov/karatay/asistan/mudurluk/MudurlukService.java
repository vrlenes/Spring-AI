package tr.gov.karatay.asistan.mudurluk;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.gov.karatay.asistan.mudurluk.dto.MudurlukOzeti;

@Service
public class MudurlukService {

    private final MudurlukRepository mudurlukRepository;

    public MudurlukService(MudurlukRepository mudurlukRepository) {
        this.mudurlukRepository = mudurlukRepository;
    }

    @Transactional(readOnly = true)
    public List<MudurlukOzeti> mudurlukleriListele() {
        return mudurlukRepository.findByAktifTrue().stream()
                .map(m -> new MudurlukOzeti(m.getAd(), m.getSorumlulukAlani()))
                .toList();
    }
}
