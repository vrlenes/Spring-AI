package tr.gov.karatay.asistan;

import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AsistanApplication {

    public static void main(String[] args) {
        // Turkce "i/I" locale hatasi: JVM varsayilan locale'i Turkce oldugunda
        // (bu makine gibi) String.toUpperCase()/toLowerCase() ASCII harfleri
        // beklenmedik sekilde donusturur (orn. "STRING" -> "STRİNG"). Bunu
        // kendi kodumuzda Locale.ROOT ile tek tek onleyebiliyoruz ama ucuncu
        // parti kutuphaneler (orn. Spring AI'nin Google GenAI modulu, tool
        // semasi JSON'una "STRING"/"INTEGER" yazarken) bunu yapamiyor - Google
        // API'si "STRİNG" gibi bozuk bir deger aldiginda 400 donuyor (canli
        // test edilerek bulundu). JVM'in varsayilan locale'ini en basta sabit
        // ve ASCII-guvenli bir degere cekmek, hem bizim kodumuzu hem butun
        // bagimliliklari bu hatadan koruyor.
        Locale.setDefault(Locale.ROOT);
        SpringApplication.run(AsistanApplication.class, args);
    }

}
