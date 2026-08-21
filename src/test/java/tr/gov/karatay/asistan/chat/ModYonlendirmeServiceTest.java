package tr.gov.karatay.asistan.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.ai.chat.client.ChatClient;

import tr.gov.karatay.asistan.common.LlmEsZamanliSinirlayici;
import tr.gov.karatay.asistan.common.enums.SohbetModu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// "Belirsizlikte GENEL'e dus" ilkesinin (bkz. ModYonlendirmeService yorumu)
// gercekten calistigini dogrular - modelin gecerli/gecersiz/hatali cikti
// verdigi durumlar, gercek bir ChatClient/LLM cagrisi olmadan.
class ModYonlendirmeServiceTest {

    private ChatClient chatClient;
    private ModYonlendirmeService modYonlendirmeService;

    @BeforeEach
    void hazirla() {
        chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        modYonlendirmeService = new ModYonlendirmeService(chatClient, new LlmEsZamanliSinirlayici(2));
    }

    @Test
    void gecerliModStringiDogruEnumaCevrilir() {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("TALEP");

        assertThat(modYonlendirmeService.yonlendir("açık talepleri listele")).isEqualTo(SohbetModu.TALEP);
    }

    @Test
    void kucukHarfVeBosluklarToleransli() {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("  imar \n");

        assertThat(modYonlendirmeService.yonlendir("ada parsel nasıl belirlenir")).isEqualTo(SohbetModu.IMAR);
    }

    @Test
    void gecersizCiktiGenelModunaDuser() {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("BILINMEYEN_SEY");

        assertThat(modYonlendirmeService.yonlendir("herhangi bir mesaj")).isEqualTo(SohbetModu.GENEL);
    }

    @Test
    void bosCiktiGenelModunaDuser() {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn(null);

        assertThat(modYonlendirmeService.yonlendir("mesaj")).isEqualTo(SohbetModu.GENEL);
    }

    @Test
    void modelHataFirlatirsaGenelModunaDuser() {
        when(chatClient.prompt().user(anyString()).call().content()).thenThrow(new RuntimeException("bağlantı hatası"));

        assertThat(modYonlendirmeService.yonlendir("mesaj")).isEqualTo(SohbetModu.GENEL);
    }
}
