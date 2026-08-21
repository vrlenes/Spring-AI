package tr.gov.karatay.asistan.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmEsZamanliSinirlayiciTest {

    @Test
    void izinVarsaIslemiCalistirirVeSonucuDoner() {
        LlmEsZamanliSinirlayici sinirlayici = new LlmEsZamanliSinirlayici(2);

        String sonuc = sinirlayici.sinirliCagir(() -> "tamam");

        assertThat(sonuc).isEqualTo("tamam");
    }

    @Test
    void izinYoksaCokFazlaIstekFirlatir() {
        LlmEsZamanliSinirlayici sinirlayici = new LlmEsZamanliSinirlayici(1);
        sinirlayici.izinAl();

        assertThatThrownBy(() -> sinirlayici.sinirliCagir(() -> "tamam")).isInstanceOf(CokFazlaIstekException.class);
    }

    @Test
    void kotaHatasiYapayZekaGeciciHatasinaCevrilir() {
        LlmEsZamanliSinirlayici sinirlayici = new LlmEsZamanliSinirlayici(2);

        assertThatThrownBy(() -> sinirlayici.sinirliCagir(() -> {
            throw new RuntimeException("429 quota exceeded");
        })).isInstanceOf(YapayZekaGeciciHataException.class);
    }

    @Test
    void ilgisizHataOlduguGibiFirlatilir() {
        LlmEsZamanliSinirlayici sinirlayici = new LlmEsZamanliSinirlayici(2);

        assertThatThrownBy(() -> sinirlayici.sinirliCagir(() -> {
            throw new IllegalStateException("baska bir hata");
        })).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void hataSonrasiIzinSerbestBirakilir() {
        LlmEsZamanliSinirlayici sinirlayici = new LlmEsZamanliSinirlayici(1);

        assertThatThrownBy(() -> sinirlayici.sinirliCagir(() -> {
            throw new IllegalStateException("hata");
        })).isInstanceOf(IllegalStateException.class);

        // Izin serbest birakilmadiysa asagidaki cagri CokFazlaIstekException firlatirdi.
        String sonuc = sinirlayici.sinirliCagir(() -> "tamam");
        assertThat(sonuc).isEqualTo("tamam");
    }
}
