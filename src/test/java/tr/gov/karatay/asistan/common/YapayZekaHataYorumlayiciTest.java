package tr.gov.karatay.asistan.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 21 Agustos'ta canli olarak yakalanan gercek Gemini kota hatasinin (bkz.
// backend loglari) dogru siniflandirildigini dogrular - saglayiciya ozel
// sinif import etmeden, sadece metin tabanli tespitle (bkz. CLAUDE.md).
class YapayZekaHataYorumlayiciTest {

    @Test
    void gercekGeminiKotaMesajiYapayZekaHatasinaCevrilir() {
        RuntimeException hata = new RuntimeException(
                "429 . You exceeded your current quota, please check your plan and billing details. "
                        + "Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests");

        RuntimeException sonuc = YapayZekaHataYorumlayici.yorumla(hata);

        assertThat(sonuc).isInstanceOf(YapayZekaGeciciHataException.class);
        assertThat(sonuc.getCause()).isSameAs(hata);
        assertThat(sonuc.getMessage()).containsIgnoringCase("kota");
    }

    @Test
    void nedenZincirindekiKotaMesajiDaTespitEdilir() {
        RuntimeException icNeden = new RuntimeException("quota exceeded for this model");
        RuntimeException disSaran = new RuntimeException("Failed to generate content", icNeden);

        RuntimeException sonuc = YapayZekaHataYorumlayici.yorumla(disSaran);

        assertThat(sonuc).isInstanceOf(YapayZekaGeciciHataException.class);
    }

    @Test
    void rateLimitMesajiDaTespitEdilir() {
        RuntimeException hata = new RuntimeException("Rate limit exceeded, try again later");

        assertThat(YapayZekaHataYorumlayici.yorumla(hata)).isInstanceOf(YapayZekaGeciciHataException.class);
    }

    @Test
    void ilgisizHataOlduguGibiDoner() {
        RuntimeException hata = new IllegalStateException("Beklenmeyen bir durum olustu");

        RuntimeException sonuc = YapayZekaHataYorumlayici.yorumla(hata);

        assertThat(sonuc).isSameAs(hata);
    }

    @Test
    void mesajiNullOlanHataOlduguGibiDoner() {
        RuntimeException hata = new NullPointerException();

        assertThat(YapayZekaHataYorumlayici.yorumla(hata)).isSameAs(hata);
    }
}
