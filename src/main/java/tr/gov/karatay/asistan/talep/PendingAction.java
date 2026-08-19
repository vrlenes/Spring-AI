package tr.gov.karatay.asistan.talep;

import java.time.Instant;
import java.util.Map;

// LLM'in onay gerektiren bir yazma islemi TALEP ETTIGINI, ama henuz
// UYGULAMADIGINI temsil eder. Gercek veritabani degisikligi sadece
// PendingActionService.onayla(...) ile - yani kullanicinin arayuzde
// "Onayla" butonuna basmasiyla - tetiklenir. Boylece yazma islemleri
// LLM'in kendi kararina/hafizasina degil, koda dayanir (bkz. CLAUDE.md:
// "Yazma islemleri once kullanici onayi gerektirir").
public record PendingAction(
        String id,
        PendingActionTuru tur,
        String takipNo,
        Map<String, String> parametreler,
        String aciklama,
        Instant olusturmaZamani) {
}
