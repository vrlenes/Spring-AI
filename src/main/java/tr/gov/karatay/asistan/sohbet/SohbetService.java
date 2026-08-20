package tr.gov.karatay.asistan.sohbet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import tr.gov.karatay.asistan.chat.dto.Kaynak;
import tr.gov.karatay.asistan.chat.dto.YapisalVeriPaketi;
import tr.gov.karatay.asistan.common.enums.MesajRolu;
import tr.gov.karatay.asistan.common.enums.SohbetModu;
import tr.gov.karatay.asistan.personel.PersonelRepository;
import tr.gov.karatay.asistan.sohbet.dto.SohbetMesajOzeti;
import tr.gov.karatay.asistan.sohbet.dto.SohbetOzeti;
import tr.gov.karatay.asistan.talep.dto.PendingActionOzeti;

// kaynaklar/araclar/bekleyenIslem alanlari DB'de TEXT kolonda JSON string
// olarak tutuluyor (Hibernate'in JSON tipiyle ugrasmadan) - bu servis
// serialize/deserialize islerini tek yerde topluyor.
@Service
public class SohbetService {

    private static final int BASLIK_MAKS_UZUNLUK = 60;

    private final SohbetRepository sohbetRepository;
    private final SohbetMesajiRepository sohbetMesajiRepository;
    private final PersonelRepository personelRepository;
    private final ObjectMapper objectMapper;

    public SohbetService(
            SohbetRepository sohbetRepository,
            SohbetMesajiRepository sohbetMesajiRepository,
            PersonelRepository personelRepository,
            ObjectMapper objectMapper) {
        this.sohbetRepository = sohbetRepository;
        this.sohbetMesajiRepository = sohbetMesajiRepository;
        this.personelRepository = personelRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Sohbet sohbetBaslat(Long personelId, SohbetModu mod) {
        Sohbet sohbet = new Sohbet();
        sohbet.setId(UUID.randomUUID().toString());
        sohbet.setPersonel(personelRepository.getReferenceById(personelId));
        sohbet.setMod(mod);
        LocalDateTime simdi = LocalDateTime.now();
        sohbet.setOlusturmaTarihi(simdi);
        sohbet.setGuncellemeTarihi(simdi);
        return sohbetRepository.save(sohbet);
    }

    @Transactional(readOnly = true)
    public List<SohbetOzeti> sohbetleriGetir(Long personelId) {
        return sohbetRepository.findByPersonelIdOrderByGuncellemeTarihiDesc(personelId).stream()
                .map(s -> new SohbetOzeti(s.getId(), s.getMod(), s.getBaslik(), s.getGuncellemeTarihi()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SohbetMesajOzeti> mesajlariGetir(String sohbetId, Long personelId) {
        sahiplikDogrula(sohbetId, personelId);
        return sohbetMesajiRepository.findBySohbetIdOrderByOlusturmaTarihiAsc(sohbetId).stream()
                .map(this::ozetleVer)
                .toList();
    }

    @Transactional
    public void mesajEkle(
            String sohbetId,
            MesajRolu rol,
            String icerik,
            List<Kaynak> kaynaklar,
            List<String> araclar,
            PendingActionOzeti bekleyenIslem,
            YapisalVeriPaketi yapisalVeri) {
        Sohbet sohbet = sohbetRepository
                .findById(sohbetId)
                .orElseThrow(() -> new IllegalArgumentException("\"%s\" id'li sohbet bulunamadı.".formatted(sohbetId)));

        SohbetMesaji mesaj = new SohbetMesaji();
        mesaj.setSohbet(sohbet);
        mesaj.setRol(rol);
        mesaj.setIcerik(icerik);
        mesaj.setKaynaklar(yaz(kaynaklar));
        mesaj.setAraclar(yaz(araclar));
        mesaj.setBekleyenIslem(yaz(bekleyenIslem));
        mesaj.setYapisalVeri(yaz(yapisalVeri));
        mesaj.setOlusturmaTarihi(LocalDateTime.now());
        sohbetMesajiRepository.save(mesaj);

        if (sohbet.getBaslik() == null && rol == MesajRolu.KULLANICI) {
            sohbet.setBaslik(baslikUret(icerik));
        }
        sohbet.setGuncellemeTarihi(LocalDateTime.now());
        sohbetRepository.save(sohbet);
    }

    private void sahiplikDogrula(String sohbetId, Long personelId) {
        sohbetRepository
                .findByIdAndPersonelId(sohbetId, personelId)
                .orElseThrow(() -> new IllegalArgumentException("\"%s\" id'li sohbet bulunamadı.".formatted(sohbetId)));
    }

    private String baslikUret(String icerik) {
        String tekSatir = icerik.strip().replaceAll("\\s+", " ");
        return tekSatir.length() <= BASLIK_MAKS_UZUNLUK ? tekSatir : tekSatir.substring(0, BASLIK_MAKS_UZUNLUK) + "…";
    }

    private SohbetMesajOzeti ozetleVer(SohbetMesaji mesaj) {
        return new SohbetMesajOzeti(
                mesaj.getRol(),
                mesaj.getIcerik(),
                oku(mesaj.getKaynaklar(), new TypeReference<List<Kaynak>>() {}),
                oku(mesaj.getAraclar(), new TypeReference<List<String>>() {}),
                oku(mesaj.getBekleyenIslem(), new TypeReference<PendingActionOzeti>() {}),
                oku(mesaj.getYapisalVeri(), new TypeReference<YapisalVeriPaketi>() {}),
                mesaj.getOlusturmaTarihi());
    }

    private String yaz(Object deger) {
        if (deger == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(deger);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private <T> T oku(String json, TypeReference<T> tip) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, tip);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
