package tr.gov.karatay.asistan.common;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> gecersizIstek(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("hata", e.getMessage()));
    }

    @ExceptionHandler(CokFazlaIstekException.class)
    public ResponseEntity<Map<String, String>> cokFazlaIstek(CokFazlaIstekException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("hata", e.getMessage()));
    }

    // Beklenmeyen her hata icin son çare: istemciye ic detaylari (stack trace,
    // SQL, vs.) SIZDIRMADAN genel bir mesaj donuyoruz, ama gercek hatayi
    // sunucu logunda tutuyoruz - aksi halde sorunu teshis etmek imkansiz olur.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> beklenmeyenHata(Exception e) {
        log.error("Beklenmeyen hata", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("hata", "Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin."));
    }
}
