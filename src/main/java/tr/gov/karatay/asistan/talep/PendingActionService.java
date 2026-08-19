package tr.gov.karatay.asistan.talep;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tr.gov.karatay.asistan.common.enums.TalepDurumu;
import tr.gov.karatay.asistan.common.enums.TalepOnceligi;
import tr.gov.karatay.asistan.talep.dto.PendingActionTeklifi;
import tr.gov.karatay.asistan.talep.dto.TalepDetay;

// Bekleyen (henuz onaylanmamis) yazma islemlerini bellekte tutar. Tek surecli
// bir demo uygulamasi oldugu icin kalicilik (DB) veya son kullanma suresi
// gerekmiyor - uygulama yeniden baslatilinca bekleyen islemler kaybolur,
// bu kabul edilebilir bir davranis.
@Service
public class PendingActionService {

    private static final Logger log = LoggerFactory.getLogger(PendingActionService.class);

    private final Map<String, PendingAction> depo = new ConcurrentHashMap<>();
    private final TalepService talepService;

    public PendingActionService(TalepService talepService) {
        this.talepService = talepService;
    }

    public PendingAction olustur(PendingActionTeklifi teklif) {
        String id = UUID.randomUUID().toString();
        PendingAction action = new PendingAction(
                id, teklif.tur(), teklif.takipNo(), teklif.parametreler(), teklif.aciklama(), Instant.now());
        depo.put(id, action);
        log.info("Bekleyen islem olusturuldu: id={}, tur={}, takipNo={}", id, teklif.tur(), teklif.takipNo());
        return action;
    }

    public Optional<PendingAction> getir(String id) {
        return Optional.ofNullable(depo.get(id));
    }

    // Onayla: bekleyen islemi depodan kaldirir ve gercek mutasyonu
    // TalepService uzerinden uygular. LLM bu asamada tamamen devre disidir.
    public TalepDetay onayla(String id) {
        PendingAction action = depo.remove(id);
        if (action == null) {
            log.warn("Onaylanmaya calisilan bekleyen islem bulunamadi: id={}", id);
            throw new IllegalArgumentException("Bu onay isteği bulunamadı veya zaten işlendi.");
        }

        Map<String, String> p = action.parametreler();
        TalepDetay sonuc = switch (action.tur()) {
            case MUDURLUGE_ATA -> talepService.talebiMudurlugeAta(action.takipNo(), p.get("mudurlukAdi"));
            case DURUM_GUNCELLE -> talepService.talepDurumGuncelle(action.takipNo(), TalepDurumu.valueOf(p.get("yeniDurum")));
            case ONCELIK_GUNCELLE -> talepService.talepOncelikGuncelle(
                    action.takipNo(), TalepOnceligi.valueOf(p.get("yeniOncelik")));
            case NOT_EKLE -> talepService.talebeNotEkle(action.takipNo(), p.get("notMetni"), p.get("personel"));
        };
        log.info("Bekleyen islem onaylandi: id={}, tur={}, takipNo={}", id, action.tur(), action.takipNo());
        return sonuc;
    }

    public void iptalEt(String id) {
        PendingAction kaldirilan = depo.remove(id);
        if (kaldirilan != null) {
            log.info("Bekleyen islem iptal edildi: id={}, tur={}, takipNo={}", id, kaldirilan.tur(), kaldirilan.takipNo());
        }
    }
}
