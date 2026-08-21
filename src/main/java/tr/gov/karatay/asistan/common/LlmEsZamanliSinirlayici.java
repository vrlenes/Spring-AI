package tr.gov.karatay.asistan.common;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Yerel Ollama modeli tek bir GPU/CPU kaynagini paylasiyor - ayni anda birden
// fazla istek gelirse (orn. birden fazla sekme, ya da toplu AI onerisi
// calisirken bir de sohbet mesaji gonderilmesi) hem ciddi yavaslama hem asiri
// isinma yasandigini bugun canli olarak gozlemledik (bkz. proje sohbet
// gecmisi). Bu sinirlayici, LLM cagiran tum servislerin PAYLASTIGI tek bir
// izin havuzuyla ayni anda kac istegin islenebilecegini sinirlar; havuz
// doluyken gelen istekler kuyruga alinmaz, hemen 429 ile reddedilir - kullanici
// belirsiz bir sure beklemek yerine "az sonra tekrar dene" mesajini hemen gorur.
@Component
public class LlmEsZamanliSinirlayici {

    private final Semaphore izinler;

    public LlmEsZamanliSinirlayici(@Value("${asistan.llm.es-zamanli-istek-siniri:2}") int sinir) {
        this.izinler = new Semaphore(sinir);
    }

    // Senkron (blocking) cagrilar icin: izin varsa islemi calistirir, yoksa
    // hemen CokFazlaIstekException firlatir.
    public <T> T sinirliCagir(Supplier<T> islem) {
        if (!izinler.tryAcquire()) {
            throw new CokFazlaIstekException(
                    "Sistem şu anda başka isteklerle meşgul, lütfen birkaç saniye sonra tekrar deneyin.");
        }
        try {
            return islem.get();
        } catch (RuntimeException e) {
            throw YapayZekaHataYorumlayici.yorumla(e);
        } finally {
            izinler.release();
        }
    }

    // Reactive/streaming akislar icin: izin senkron olarak alinir/birakilir
    // (Flux olusturulmadan once izinAl, Flux tamamlaninca/hata alinca/iptal
    // edilince doFinally ile izinBirak cagrilmali).
    public boolean izinAl() {
        return izinler.tryAcquire();
    }

    public void izinBirak() {
        izinler.release();
    }
}
