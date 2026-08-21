package tr.gov.karatay.asistan.rag;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.ai.chat.client.ChatClient;

import tr.gov.karatay.asistan.chat.dto.KaynakDogrulamaSonucu;
import tr.gov.karatay.asistan.common.LlmEsZamanliSinirlayici;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Herhangi bir hata/gecersiz durumda null donup rozetin hic gosterilmemesini
// (yanlis "dogrulandi"/"dogrulanamadi" iddiasi uretmemeyi) dogrular - bkz.
// KaynakDogrulamaService yorumu.
class KaynakDogrulamaServiceTest {

    private ChatClient chatClient;
    private KaynakDogrulamaService kaynakDogrulamaService;

    @BeforeEach
    void hazirla() {
        chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        kaynakDogrulamaService = new KaynakDogrulamaService(chatClient, new LlmEsZamanliSinirlayici(2));
    }

    @Test
    void kaynaklaTutarliCevapDogrulanir() {
        when(chatClient.prompt().user(anyString()).call().entity(KaynakDogrulamaSonucu.class))
                .thenReturn(new KaynakDogrulamaSonucu(true, null));

        KaynakDogrulamaSonucu sonuc =
                kaynakDogrulamaService.dogrula("Madde 20'ye göre meclis her ayın ilk haftası toplanır.", List.of("Madde 20 - Meclis her ayın ilk haftası toplanır."));

        assertThat(sonuc).isNotNull();
        assertThat(sonuc.dogrulandi()).isTrue();
    }

    @Test
    void kaynakla_celisen_cevap_dogrulanmaz() {
        when(chatClient.prompt().user(anyString()).call().entity(KaynakDogrulamaSonucu.class))
                .thenReturn(new KaynakDogrulamaSonucu(false, "Toplantı günü kaynakta farklı yazıyor."));

        KaynakDogrulamaSonucu sonuc =
                kaynakDogrulamaService.dogrula("Meclis her ayın son haftası toplanır.", List.of("Madde 20 - Meclis her ayın ilk haftası toplanır."));

        assertThat(sonuc).isNotNull();
        assertThat(sonuc.dogrulandi()).isFalse();
        assertThat(sonuc.not()).isNotBlank();
    }

    @Test
    void bosKaynakListesindeDogrulamaYapilmaz() {
        KaynakDogrulamaSonucu sonuc = kaynakDogrulamaService.dogrula("Bir cevap.", List.of());

        assertThat(sonuc).isNull();
    }

    @Test
    void bosCevapMetnindeDogrulamaYapilmaz() {
        KaynakDogrulamaSonucu sonuc = kaynakDogrulamaService.dogrula("", List.of("kaynak metni"));

        assertThat(sonuc).isNull();
    }

    @Test
    void modelHataFirlatirsaNullDoner() {
        when(chatClient.prompt().user(anyString()).call().entity(KaynakDogrulamaSonucu.class))
                .thenThrow(new RuntimeException("bağlantı hatası"));

        KaynakDogrulamaSonucu sonuc = kaynakDogrulamaService.dogrula("Bir cevap.", List.of("kaynak metni"));

        assertThat(sonuc).isNull();
    }
}
