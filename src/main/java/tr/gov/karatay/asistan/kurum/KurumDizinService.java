package tr.gov.karatay.asistan.kurum;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class KurumDizinService {

    private final KurumDizinRepository kurumDizinRepository;

    public KurumDizinService(KurumDizinRepository kurumDizinRepository) {
        this.kurumDizinRepository = kurumDizinRepository;
    }

    public List<MudurlukIletisim> mudurlukIletisimAra(String mudurlukAdi) {
        return kurumDizinRepository.mudurlukAra(mudurlukAdi);
    }

    public List<PersonelDizinKaydi> personelAra(String sorgu) {
        return kurumDizinRepository.personelAra(sorgu);
    }
}
