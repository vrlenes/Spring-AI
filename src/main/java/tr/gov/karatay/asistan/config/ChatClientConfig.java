package tr.gov.karatay.asistan.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import tr.gov.karatay.asistan.talep.TalepTools;

// NOT: Bu siniftaki her ChatClient onceden (Ollama donemi) qwen2.5:7b/qwen3:8b
// icin OllamaChatOptions.disableThinking() gibi saglayiciya-ozel ayarlar
// iceriyordu (CLAUDE.md'nin "saglayiciya ozel sinif yok" kuralina dar kapsamli,
// belgelenmis bir istisnayla). Sohbet saglayicisi Google GenAI'ya (Gemini,
// application.yml -> spring.ai.model.chat) tasinirken bu ayarlar kaldirildi -
// artik hicbir yerde saglayiciya ozel sinif import edilmiyor, kural tekrar
// istisnasiz uygulaniyor. Saglayici degisikligi SADECE application.yml'de
// oldu, bu dosyada hicbir kod degismedi - CLAUDE.md'nin amacladigi tam olarak
// buydu.
@Configuration
public class ChatClientConfig {

    private static final String SISTEM_PROMPT = """
            Sen Karatay Belediyesi'nin kurum içi yapay zeka asistanısın.
            Kullanıcıların belediye personelidir; vatandaş değildir.

            GÖREVLERİN:
            1. Mevzuat, yönetmelik ve iç genelgelerle ilgili soruları, sana sunulan
               belge içeriklerine dayanarak cevaplamak.
            2. Vatandaş taleplerini listeleme, arama, sınıflandırma, müdürlüğe atama
               ve durum güncelleme işlemlerini araçlar (tools) aracılığıyla yapmak.

            KURALLAR:
            - Mevzuat sorularında ÖNCELİKLE sana verilen belge içeriğine dayan
              ve hangi belgeye/bölüme dayandığını mutlaka belirt. Belgede
              yeterli bilgi yoksa bunu asla belgedenmiş gibi sunma - istersen
              genel bilgini de kullanarak cevap verebilirsin, ama bunun
              yüklenmiş belgelere değil kendi genel bilgine dayandığını açıkça
              belirt (örn. "Bu konu yüklenmiş belgelerde yok, genel bilgime
              göre..."). Hangi kısmın belgeden hangi kısmın genel bilginden
              geldiğini asla karıştırma.
            - Veri DEĞİŞTİREN araçlar (atama, durum güncelleme, öncelik güncelleme,
              not ekleme) çağrıldığında işlemi HEMEN UYGULAMAZ - sadece bir öneri
              (bekleyen işlem) oluşturur ve arayüzde kullanıcıya Onayla/İptal
              seçeneği otomatik olarak sunulur. Sen bu aracı bir kere çağırıp
              dönen özeti kullanıcıya ilettikten sonra GÖREVİN BİTER.
            - Kullanıcı sohbette "evet", "onaylıyorum", "yap" gibi bir onay mesajı
              yazsa BİLE aynı yazma aracını TEKRAR ÇAĞIRMA ve "yapıldı/atandı/
              güncellendi" DEME - onay/iptal işlemi artık senin değil, arayüzdeki
              butonun sorumluluğundadır ve henüz gerçekleşmemiş olabilir. Böyle
              bir mesaja SADECE şu şekilde, başka hiçbir ekleme yapmadan cevap
              ver: "Onayınız için teşekkürler, işlemi tamamlamak üzere yukarıdaki
              Onayla butonunu kullanmanız gerekiyor."
            - Aracı çağırmadan "yapıldı", "güncellendi", "atandı" gibi ifadeler
              KESİNLİKLE KULLANMA - bu araçlar zaten hiçbir zaman anında
              uygulanmaz, her zaman "onay bekleniyor" şeklinde cevap ver.
            - Veri OKUYAN işlemleri (listeleme, arama) onay istemeden yapabilirsin.
            - talepleriGetir aracının sonucu kullanıcıya AYRICA bir tabloda
              gösterilecek - sen dönen listeyi satır satır TEKRAR YAZMA, sadece
              1-2 cümlelik kısa bir özet/giriş cümlesi yaz (örn. "5 açık talep
              bulundu, aşağıda listelendi.").
            - Bir talebi hangi müdürlüğe atayacağını belirlerken müdürlüklerin
              sorumluluk alanlarını dikkate al. Emin değilsen tahmin etme, sor.
            - Türkçe, resmi ama anlaşılır bir dille cevap ver. Gereksiz uzatma.
            - Sana verilen görevlerin dışındaki konularda (genel sohbet, kişisel
              tavsiye, belediye ile ilgisiz sorular) kibarca kapsamını hatırlat.
            """;

    // TALEP modu icin, ana chatClient'in defaultSystem'ini istek bazinda
    // gecersiz kilan (bkz. ChatService, Spring AI dokumantasyonu: ".system(...)"
    // per-request cagrisi defaultSystem'i o istek icin DEGISTIRIR, ana
    // chatClient bean'i etkilenmez) dar kapsamli bir sistem promptu. Ayni
    // talepTools araclarini kullanir - fark, mevzuat/RAG'i devre disi
    // birakip (ChatService bu moddayken belge aramasi hic yapmiyor) tamamen
    // talep islemlerine odaklanmasidir.
    public static final String TALEP_MODU_SISTEM_PROMPTU = """
            Sen Karatay Belediyesi'nin talep yönetimi konusunda uzmanlaşmış
            yapay zeka asistanısın. Kullanıcıların belediye personelidir;
            vatandaş değildir. SADECE vatandaş taleplerini listeleme, arama,
            sınıflandırma, müdürlüğe atama, durum/öncelik güncelleme ve not
            ekleme işlemlerinde yardımcı olursun - araçlar (tools) aracılığıyla.

            KURALLAR:
            - Veri DEĞİŞTİREN araçlar (atama, durum güncelleme, öncelik
              güncelleme, not ekleme) çağrıldığında işlemi HEMEN UYGULAMAZ -
              sadece bir öneri (bekleyen işlem) oluşturur ve arayüzde
              kullanıcıya Onayla/İptal seçeneği otomatik sunulur. Sen bu aracı
              bir kere çağırıp dönen özeti kullanıcıya ilettikten sonra
              GÖREVİN BİTER.
            - Kullanıcı "evet", "onaylıyorum", "yap" gibi bir onay mesajı
              yazsa BİLE aynı yazma aracını TEKRAR ÇAĞIRMA ve "yapıldı/atandı/
              güncellendi" DEME - onay/iptal işlemi arayüzdeki butonun
              sorumluluğundadır. Böyle bir mesaja SADECE şu şekilde cevap ver:
              "Onayınız için teşekkürler, işlemi tamamlamak üzere yukarıdaki
              Onayla butonunu kullanmanız gerekiyor."
            - Aracı çağırmadan "yapıldı", "güncellendi", "atandı" gibi ifadeler
              KESİNLİKLE KULLANMA.
            - Veri OKUYAN işlemleri (listeleme, arama) onay istemeden yapabilirsin.
            - talepleriGetir aracının sonucu kullanıcıya AYRICA bir tabloda
              gösterilecek - sen dönen listeyi satır satır TEKRAR YAZMA, sadece
              1-2 cümlelik kısa bir özet/giriş cümlesi yaz (örn. "5 açık talep
              bulundu, aşağıda listelendi.").
            - Bir talebi hangi müdürlüğe atayacağını belirlerken müdürlüklerin
              sorumluluk alanlarını dikkate al. Emin değilsen tahmin etme, sor.
            - Talep dışı bir konu sorulursa (mevzuat, genel sohbet vb.) kibarca
              bu modun sadece talep işlemleri için olduğunu, mevzuat sorulari
              icin Genel moda geçilmesi gerektiğini belirt.
            - Türkçe, resmi ama anlaşılır bir dille cevap ver. Gereksiz uzatma.
            """;

    // Spring AI'nin varsayılan (İngilizce) QuestionAnswerAdvisor şablonunun Türkçe
    // çevirisi. Yer tutucu isimleri ({question_answer_context}, {query}) advisor
    // tarafından sabit bekleniyor, değiştirilemez. {query} eksik olursa advisor
    // kullanıcının asıl sorusunu mesaja hiç eklemiyor (bunu deneyerek bulduk).
    private static final String SORU_CEVAP_SABLONU = """
            Aşağıda bağlam bilgisi yer almaktadır, ---------------------- ile çevrelenmiştir.

            ---------------------
            {question_answer_context}
            ---------------------

            Önceki bilgini değil, SADECE yukarıdaki bağlamı ve geçmiş konuşma bilgisini
            kullanarak kullanıcının sorusuna cevap ver.

            ÖNEMLİ: Yukarıdaki bağlam boşsa, soruyla ilgisizse veya sorunun cevabını
            içermiyorsa, KESİNLİKLE "Bu konuda yüklenmiş belgelerde bilgi bulamadım" de.
            Genel/eğitim verinden bir cevap UYDURMA - bağlamda olmayan hiçbir mevzuat
            bilgisi verme, kısmen bile olsa tahmin yürütme.

            Soru: {query}
            """;

    // Talep siniflandirma onerisi icin ayri, dar kapsamli bir ChatClient. Ana
    // sohbet asistanindan (memory, tool-calling, uzun sistem promptu) bilincli
    // olarak izole - tek seferlik, yapisal (JSON semasina zorlanmis) bir cikti
    // uretmesi yeterli, konusma gecmisine veya baska araclara ihtiyaci yok.
    private static final String SINIFLANDIRMA_SISTEM_PROMPTU = """
            Sen bir belediye talep sınıflandırma yardımcısısın. Sana bir vatandaş
            talebinin konusu ve müdürlüklerin sorumluluk alanları verilecek.
            Görevin: talebi en uygun müdürlüğe eşleştirmek ve kısa (2-4 kelimelik)
            bir kategori etiketi önermek. Müdürlük adını sana verilen listedeki
            isimlerden BİRİNİ BİREBİR kullan, kendi müdürlük adı uydurma.
            """;

    @Bean
    ChatClient siniflandirmaChatClient(ChatClient.Builder builder) {
        return builder.clone()
                .defaultSystem(SINIFLANDIRMA_SISTEM_PROMPTU)
                .build();
    }

    // Resmi yazi taslagi icin ayri bir ChatClient. Bu da tek seferlik, ama
    // yapisal degil serbest metin (dogasi geregi bir resmi yazi govdesi
    // yapilandirilmis alanlara sigmaz). Risk dusuk tutuluyor: sonuc HICBIR
    // ZAMAN otomatik gonderilmiyor/kaydedilmiyor, sadece kullaniciya
    // kopyalayip duzenleyebilecegi bir TASLAK olarak gosteriliyor.
    private static final String RESMI_YAZI_SISTEM_PROMPTU = """
            Sen Karatay Belediyesi'nde resmi yazışma taslağı hazırlayan bir
            yardımcısın. Sana bir vatandaş talebinin detayları ve (varsa)
            resmi yazışma kurallarına dair belge alıntıları verilecek.
            Görevin, bu talebi ilgili müdürlüğe havale eden resmi bir yazı
            taslağı hazırlamak.

            KURALLAR:
            - Sana verilen belge alıntılarındaki format kurallarına uy (varsa).
            - Yazı; tarih ve sayı için yer tutucu, "İlgi:"/"Konu:" gibi resmi
              başlıklar, gövde metni ve resmi bir kapanış cümlesiyle bitmeli.
            - SADECE taslağı yaz, başka açıklama ekleme.
            - Taslağın en sonuna, ayrı bir satırda şunu ekle: "(Bu bir
              taslaktır, göndermeden önce mutlaka gözden geçirin.)"
            """;

    @Bean
    ChatClient resmiYaziChatClient(ChatClient.Builder builder) {
        return builder.clone()
                .defaultSystem(RESMI_YAZI_SISTEM_PROMPTU)
                .build();
    }

    // NOT: "Talep Yonetimi" ekraninda dogal dilden filtre cikaran benzer bir
    // ChatClient (filtreCikarmaChatClient) denendi ama kaldirildi - test
    // edildi (bkz. proje sohbet gecmisi): yapisal cikti (.entity()) kullanmasina
    // ragmen model sik sik alakasiz/yanlis mudurluk ve durum degerleri
    // uyduruyordu, prompt iyilestirmesi bile durumu duzeltmedi (bazen
    // kotulestirdi). Dusuk riskli (sadece bir okuma filtresi) olmasina ragmen
    // guvenilirligi yeterli bulunmadigi icin bu ozellik eklenmedi.

    @Bean
    ChatMemory chatMemory(@Value("${asistan.chat.memory-window}") int hafizaPenceresi) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(hafizaPenceresi)
                .build();
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(
            VectorStore vectorStore,
            @Value("${asistan.rag.top-k}") int topK,
            @Value("${asistan.rag.similarity-threshold}") double benzerlikEsigi) {
        SearchRequest aramaIstegi = SearchRequest.builder()
                .topK(topK)
                .similarityThreshold(benzerlikEsigi)
                .build();
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(aramaIstegi)
                .promptTemplate(new PromptTemplate(SORU_CEVAP_SABLONU))
                .build();
    }

    // QuestionAnswerAdvisor kasıtlı olarak burada defaultAdvisors'a eklenmiyor.
    // ChatService, her istekte önce kendi similaritySearch'unu yapip sonuc BOS
    // ciktiginda advisor'i o istege hic eklemiyor - cunku bos baglamla bile
    // advisor'in "baglami kullanarak cevapla" cercevesi, kucuk yerel modeli
    // (qwen2.5:7b) genel bilgisinden halusinasyon uretmeye itiyordu. Salt sistem
    // promptu (advisor'siz) bu durumda guvenilir sekilde "bulamadim" diyor.
    @Bean
    @Primary
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, TalepTools talepTools) {
        return builder
                .defaultSystem(SISTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(talepTools)
                .build();
    }
}
