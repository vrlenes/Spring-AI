package tr.gov.karatay.asistan.talep;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tr.gov.karatay.asistan.common.enums.TalepDurumu;
import tr.gov.karatay.asistan.common.enums.TalepOnceligi;
import tr.gov.karatay.asistan.talep.dto.PendingActionTeklifi;
import tr.gov.karatay.asistan.talep.dto.TalepDetay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Bugunku oturumun ana bulgusu buradan dogruluyor: yazma islemleri sadece
// bu servis uzerinden, "onayla" cagrisiyla gerceklesir - LLM'in metninde ne
// yazdiginin veri butunlugune hicbir etkisi yok. Ayni id'nin iki kez
// onaylanamamasi (bkz. onayla_ikinciCagridaHataFirlatirVeTekrarUygulanmaz)
// ozellikle onemli - bugun canli olarak curl ile de dogrulanmisti.
@ExtendWith(MockitoExtension.class)
class PendingActionServiceTest {

    @Mock
    private TalepService talepService;

    private PendingActionService pendingActionService;

    @BeforeEach
    void hazirla() {
        pendingActionService = new PendingActionService(talepService);
    }

    private PendingActionTeklifi teklif(PendingActionTuru tur, Map<String, String> parametreler) {
        return new PendingActionTeklifi(tur, "TLP-2026-00001", parametreler, "test açıklaması");
    }

    @Test
    void olustur_bellekteSaklarVeGercekBenzersizIdDoner() {
        PendingAction a = pendingActionService.olustur(teklif(PendingActionTuru.MUDURLUGE_ATA, Map.of("mudurlukAdi", "Fen İşleri Müdürlüğü")));
        PendingAction b = pendingActionService.olustur(teklif(PendingActionTuru.MUDURLUGE_ATA, Map.of("mudurlukAdi", "Fen İşleri Müdürlüğü")));

        assertThat(a.id()).isNotEqualTo(b.id());
        assertThat(pendingActionService.getir(a.id())).isPresent();
        assertThat(pendingActionService.getir(b.id())).isPresent();
    }

    @Test
    void olustur_onaylanmadanTalepServisiHicCagrilmaz() {
        pendingActionService.olustur(teklif(PendingActionTuru.MUDURLUGE_ATA, Map.of("mudurlukAdi", "Fen İşleri Müdürlüğü")));

        verifyNoInteractions(talepService);
    }

    @Test
    void onayla_MUDURLUGE_ATA_dogruParametrelerleTalepServisiniCagirir() {
        PendingAction action = pendingActionService.olustur(
                teklif(PendingActionTuru.MUDURLUGE_ATA, Map.of("mudurlukAdi", "Fen İşleri Müdürlüğü")));
        TalepDetay beklenen = mock(TalepDetay.class);
        when(talepService.talebiMudurlugeAta("TLP-2026-00001", "Fen İşleri Müdürlüğü")).thenReturn(beklenen);

        TalepDetay sonuc = pendingActionService.onayla(action.id());

        assertThat(sonuc).isSameAs(beklenen);
        verify(talepService).talebiMudurlugeAta("TLP-2026-00001", "Fen İşleri Müdürlüğü");
    }

    @Test
    void onayla_DURUM_GUNCELLE_dogruEnumIleCagirir() {
        PendingAction action = pendingActionService.olustur(
                teklif(PendingActionTuru.DURUM_GUNCELLE, Map.of("yeniDurum", "ISLEMDE")));
        when(talepService.talepDurumGuncelle("TLP-2026-00001", TalepDurumu.ISLEMDE)).thenReturn(mock(TalepDetay.class));

        pendingActionService.onayla(action.id());

        verify(talepService).talepDurumGuncelle("TLP-2026-00001", TalepDurumu.ISLEMDE);
    }

    @Test
    void onayla_ONCELIK_GUNCELLE_dogruEnumIleCagirir() {
        PendingAction action = pendingActionService.olustur(
                teklif(PendingActionTuru.ONCELIK_GUNCELLE, Map.of("yeniOncelik", "ACIL")));
        when(talepService.talepOncelikGuncelle("TLP-2026-00001", TalepOnceligi.ACIL)).thenReturn(mock(TalepDetay.class));

        pendingActionService.onayla(action.id());

        verify(talepService).talepOncelikGuncelle("TLP-2026-00001", TalepOnceligi.ACIL);
    }

    @Test
    void onayla_NOT_EKLE_dogruParametrelerleCagirir() {
        PendingAction action = pendingActionService.olustur(
                teklif(PendingActionTuru.NOT_EKLE, Map.of("notMetni", "Saha kontrolü yapıldı", "personel", "Ahmet")));
        when(talepService.talebeNotEkle("TLP-2026-00001", "Saha kontrolü yapıldı", "Ahmet")).thenReturn(mock(TalepDetay.class));

        pendingActionService.onayla(action.id());

        verify(talepService).talebeNotEkle("TLP-2026-00001", "Saha kontrolü yapıldı", "Ahmet");
    }

    @Test
    void onayla_ikinciCagridaHataFirlatirVeTekrarUygulanmaz() {
        PendingAction action = pendingActionService.olustur(
                teklif(PendingActionTuru.MUDURLUGE_ATA, Map.of("mudurlukAdi", "Fen İşleri Müdürlüğü")));
        when(talepService.talebiMudurlugeAta("TLP-2026-00001", "Fen İşleri Müdürlüğü")).thenReturn(mock(TalepDetay.class));

        pendingActionService.onayla(action.id());
        assertThatThrownBy(() -> pendingActionService.onayla(action.id())).isInstanceOf(IllegalArgumentException.class);

        // Ikinci cagrida talepService TEKRAR cagrilmamali - sadece 1 kez.
        verify(talepService, times(1)).talebiMudurlugeAta("TLP-2026-00001", "Fen İşleri Müdürlüğü");
    }

    @Test
    void onayla_bilinmeyenIdIcinHataFirlatirVeTalepServisiCagrilmaz() {
        assertThatThrownBy(() -> pendingActionService.onayla("olmayan-id")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(talepService);
    }

    @Test
    void iptalEt_talepServisiniHicCagirmaz() {
        PendingAction action = pendingActionService.olustur(
                teklif(PendingActionTuru.MUDURLUGE_ATA, Map.of("mudurlukAdi", "Fen İşleri Müdürlüğü")));

        pendingActionService.iptalEt(action.id());

        verifyNoInteractions(talepService);
        assertThat(pendingActionService.getir(action.id())).isEmpty();
    }

    @Test
    void iptalEt_sonrasindaAyniIdOnaylanamaz() {
        PendingAction action = pendingActionService.olustur(
                teklif(PendingActionTuru.MUDURLUGE_ATA, Map.of("mudurlukAdi", "Fen İşleri Müdürlüğü")));

        pendingActionService.iptalEt(action.id());

        assertThatThrownBy(() -> pendingActionService.onayla(action.id())).isInstanceOf(IllegalArgumentException.class);
        verify(talepService, never()).talebiMudurlugeAta(any(), any());
    }
}
