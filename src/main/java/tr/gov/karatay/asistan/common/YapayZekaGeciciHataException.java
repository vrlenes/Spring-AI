package tr.gov.karatay.asistan.common;

// AI saglayicisindan (kota dolmasi, oran siniri gibi) GECICI bir hata alindiginda
// firlatilir - kullanicinin "birazdan tekrar dene" diye anlayabilecegi bir mesajla.
// Kodun kendisinde bir hata OLMADIGINI ayirt etmek icin genel RuntimeException
// yerine ayri bir tip (bkz. GlobalExceptionHandler, YapayZekaHataYorumlayici).
public class YapayZekaGeciciHataException extends RuntimeException {

    public YapayZekaGeciciHataException(String mesaj, Throwable neden) {
        super(mesaj, neden);
    }
}
