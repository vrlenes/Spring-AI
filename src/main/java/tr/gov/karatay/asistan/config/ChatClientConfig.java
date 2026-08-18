package tr.gov.karatay.asistan.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            - Mevzuat sorularında SADECE sana verilen belge içeriğine dayan.
              Belgede yoksa "Bu konuda yüklenmiş belgelerde bilgi bulamadım" de.
              ASLA hafızandan mevzuat maddesi uydurma. Yanlış bilgi, bilgi
              vermemekten çok daha zararlıdır.
            - Cevabında hangi belgeye ve hangi bölüme dayandığını mutlaka belirt.
            - Veri DEĞİŞTİREN bir işlem yapmadan önce (atama, durum güncelleme,
              not ekleme) kullanıcıya ne yapacağını özetle ve onay iste.
              Onay gelmeden aracı çağırma.
            - Veri OKUYAN işlemleri (listeleme, arama) onay istemeden yapabilirsin.
            - Bir talebi hangi müdürlüğe atayacağını belirlerken müdürlüklerin
              sorumluluk alanlarını dikkate al. Emin değilsen tahmin etme, sor.
            - Türkçe, resmi ama anlaşılır bir dille cevap ver. Gereksiz uzatma.
            - Sana verilen görevlerin dışındaki konularda (genel sohbet, kişisel
              tavsiye, belediye ile ilgisiz sorular) kibarca kapsamını hatırlat.
            """;

    @Bean
    ChatMemory chatMemory(@Value("${asistan.chat.memory-window}") int hafizaPenceresi) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(hafizaPenceresi)
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem(SISTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
