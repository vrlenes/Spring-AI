package tr.gov.karatay.asistan.common;

import java.util.Locale;

// Google GenAI kota/oran-siniri hatasi (bkz. 21 Agustos canli olay - Gemini
// ucretsiz katmaninin gunluk 500 istek sinirina takilinca "com.google.genai.
// errors.ClientException: 429 ... Quota exceeded" firlatiyor) BUGUNE KADAR
// kullaniciya duz "Beklenmeyen bir hata olustu" olarak yansiyordu - ayirt
// edilemedigi icin gercek bir kod hatasindan farksiz gorunuyordu.
//
// Saglayiciya ozel istisna sinifini (ClientException) ASLA import ETMEDEN
// (bkz. CLAUDE.md - "saglayiciya ozel sinif import etme" kurali) sadece hata
// zincirindeki mesaj METNINE bakarak tespit eder. Saglayici degisirse (orn.
// Ollama'ya donus) bu anahtar kelimeler eslesmeyebilir - o zaman tespit
// sessizce devre disi kalir ve hata oldugu gibi yukari firlatilir; kodun
// kendisi DEGISMEZ.
public final class YapayZekaHataYorumlayici {

    private static final int MAKS_NEDEN_DERINLIGI = 10;

    private YapayZekaHataYorumlayici() {
    }

    public static RuntimeException yorumla(RuntimeException e) {
        if (kotaHatasiMi(e)) {
            return new YapayZekaGeciciHataException(
                    "Yapay zeka servisinin kullanım kotası şu anda dolu. Lütfen birkaç dakika sonra tekrar deneyin.",
                    e);
        }
        return e;
    }

    private static boolean kotaHatasiMi(Throwable e) {
        Throwable mevcut = e;
        for (int derinlik = 0; mevcut != null && derinlik < MAKS_NEDEN_DERINLIGI; derinlik++, mevcut = mevcut.getCause()) {
            String mesaj = mevcut.getMessage();
            if (mesaj == null) {
                continue;
            }
            String kucukHarf = mesaj.toLowerCase(Locale.ROOT);
            if (kucukHarf.contains("429")
                    || kucukHarf.contains("quota")
                    || kucukHarf.contains("rate limit")
                    || kucukHarf.contains("resource_exhausted")) {
                return true;
            }
        }
        return false;
    }
}
