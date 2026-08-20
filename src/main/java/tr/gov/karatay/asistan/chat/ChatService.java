package tr.gov.karatay.asistan.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import tr.gov.karatay.asistan.chat.dto.ChatRequest;
import tr.gov.karatay.asistan.chat.dto.ChatResponse;
import tr.gov.karatay.asistan.chat.dto.Kaynak;
import tr.gov.karatay.asistan.chat.dto.YapisalVeriPaketi;
import tr.gov.karatay.asistan.common.CokFazlaIstekException;
import tr.gov.karatay.asistan.common.LlmEsZamanliSinirlayici;
import tr.gov.karatay.asistan.common.enums.MesajRolu;
import tr.gov.karatay.asistan.common.enums.SohbetModu;
import tr.gov.karatay.asistan.config.ChatClientConfig;
import tr.gov.karatay.asistan.sohbet.SohbetService;
import tr.gov.karatay.asistan.talep.PendingActionService;
import tr.gov.karatay.asistan.talep.TalepTools;
import tr.gov.karatay.asistan.talep.dto.PendingActionOzeti;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    // Onceden, kucuk yerel modelin (qwen2.5:7b) belgesiz mevzuat sorularinda
    // hafizasindan halusinasyon uretmesini engellemek icin mesaja zorla bir
    // "sadece 'bulamadim' de" talimati ekleniyordu (BELGE_BULUNAMADI_ONEKI) -
    // ve bunun talep isteklerine karismamasi icin ayri bir anahtar-kelime
    // yonlendirmesi (talepIstegiGibi) gerekiyordu. Bu mekanizma kaldirildi:
    // artik model belgede olmayan bir soruya kendi genel bilgisiyle cevap
    // VEREBILIR (sistem promptu bunu "genel bilgiye dayaniyorum" diye acikca
    // belirtmesini istiyor), ama cevabin belgeye mi yoksa genel bilgiye mi
    // dayandigi frontend'de KODDAN turetiliyor: kaynaklar ve araclar listesi
    // ikisi de bossa, arayuz bunu "genel bilgi" olarak isaretliyor - modelin
    // kendi ifadesine guvenmeden, ayni "kaynak gosterimi koddan uretilir"
    // prensibiyle (bkz. CLAUDE.md).
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final PendingActionService pendingActionService;
    private final SohbetService sohbetService;
    private final ObjectMapper objectMapper;
    private final LlmEsZamanliSinirlayici llmSinirlayici;
    private final int topK;
    private final double benzerlikEsigi;

    public ChatService(
            ChatClient chatClient,
            VectorStore vectorStore,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            PendingActionService pendingActionService,
            SohbetService sohbetService,
            ObjectMapper objectMapper,
            LlmEsZamanliSinirlayici llmSinirlayici,
            @Value("${asistan.rag.top-k}") int topK,
            @Value("${asistan.rag.similarity-threshold}") double benzerlikEsigi) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.questionAnswerAdvisor = questionAnswerAdvisor;
        this.pendingActionService = pendingActionService;
        this.sohbetService = sohbetService;
        this.objectMapper = objectMapper;
        this.llmSinirlayici = llmSinirlayici;
        this.topK = topK;
        this.benzerlikEsigi = benzerlikEsigi;
    }

    public ChatResponse yanitla(ChatRequest istek, Long personelId) {
        SohbetModu mod = modCoz(istek.mod());
        String conversationId = sohbetIdCoz(istek.conversationId(), personelId, mod);
        List<Kaynak> kaynaklar = kaynaklarBul(istek.mesaj(), mod);
        List<String> bekleyenIslemIdleri = new ArrayList<>();
        List<String> kullanilanAraclar = new ArrayList<>();
        List<YapisalVeriPaketi> yapisalVeriListesi = new ArrayList<>();

        sohbetService.mesajEkle(conversationId, MesajRolu.KULLANICI, istek.mesaj(), null, null, null, null);

        // .system(...) SADECE GENEL disi modlarda cagirilir: Spring AI'de bu
        // metot cagrilirsa builder'daki defaultSystem'i o istek icin
        // DEGISTIRIR - hicbir zaman "bos" bir Consumer ile cagirmiyoruz,
        // aksi halde GENEL modun ana sistem promptu kaybolabilir.
        ChatClient.ChatClientRequestSpec istekSpec = chatClient.prompt().user(istek.mesaj());
        String promptOverride = sistemPromptuOverride(mod);
        if (promptOverride != null) {
            istekSpec = istekSpec.system(promptOverride);
        }
        final ChatClient.ChatClientRequestSpec sonIstekSpec = istekSpec
                .toolContext(Map.of(
                        TalepTools.PENDING_ACTION_ID_SINK, bekleyenIslemIdleri,
                        TalepTools.KULLANILAN_ARAC_SINK, kullanilanAraclar,
                        TalepTools.YAPISAL_VERI_SINK, yapisalVeriListesi))
                .advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, conversationId);
                    if (!kaynaklar.isEmpty()) {
                        a.advisors(questionAnswerAdvisor);
                    }
                });

        ChatClientResponse yanit =
                llmSinirlayici.sinirliCagir(() -> sonIstekSpec.call().chatClientResponse());

        String cevapMetni = metniCikar(yanit);
        List<String> araclar = benzersiz(kullanilanAraclar);
        PendingActionOzeti bekleyenIslem = bekleyenIslemBul(bekleyenIslemIdleri);
        YapisalVeriPaketi yapisalVeri = yapisalVeriListesi.isEmpty() ? null : yapisalVeriListesi.get(0);
        sohbetService.mesajEkle(
                conversationId, MesajRolu.ASISTAN, cevapMetni, bosMu(kaynaklar), bosMu(araclar), bekleyenIslem, yapisalVeri);

        log.info(
                "Sohbet yaniti uretildi: conversationId={}, mod={}, kaynakSayisi={}, kullanilanArac={}",
                conversationId,
                mod,
                kaynaklar.size(),
                araclar);

        return new ChatResponse(conversationId, cevapMetni, kaynaklar, bekleyenIslem, araclar, yapisalVeri);
    }

    public Flux<ServerSentEvent<String>> akisliYanitla(ChatRequest istek, Long personelId) {
        // Izin, Flux insa edilmeden ONCE senkron olarak alinir: Flux'lar tembel
        // (lazy) kuruldugu icin, izinAl() burada degil de bir Flux operatoru
        // icinde cagrilsaydi, hemen firlatilan bir CokFazlaIstekException yerine
        // reaktif zincirin bir yerinde sinyallenen bir hata elde ederdik - bunun
        // @RestControllerAdvice tarafindan guvenilir sekilde yakalanip
        // yakalanmayacagi (WebFlux olmayan bu uygulamada) belirsiz. Senkron
        // firlatma, controller metodu cagrilirken atildigi icin kesin yakalanir.
        if (!llmSinirlayici.izinAl()) {
            throw new CokFazlaIstekException(
                    "Sistem şu anda başka isteklerle meşgul, lütfen birkaç saniye sonra tekrar deneyin.");
        }

        SohbetModu mod = modCoz(istek.mod());
        String conversationId = sohbetIdCoz(istek.conversationId(), personelId, mod);
        List<Kaynak> kaynaklar = kaynaklarBul(istek.mesaj(), mod);
        List<String> bekleyenIslemIdleri = new ArrayList<>();
        List<String> kullanilanAraclar = new ArrayList<>();
        List<YapisalVeriPaketi> yapisalVeriListesi = new ArrayList<>();
        StringBuilder birikenMetin = new StringBuilder();

        sohbetService.mesajEkle(conversationId, MesajRolu.KULLANICI, istek.mesaj(), null, null, null, null);

        Flux<ServerSentEvent<String>> conversationIdOlayi = Flux.just(
                ServerSentEvent.builder(conversationId).event("conversationId").build());

        ChatClient.ChatClientRequestSpec akisIstekSpec = chatClient.prompt().user(istek.mesaj());
        String akisPromptOverride = sistemPromptuOverride(mod);
        if (akisPromptOverride != null) {
            akisIstekSpec = akisIstekSpec.system(akisPromptOverride);
        }

        Flux<ServerSentEvent<String>> tokenOlaylari = akisIstekSpec
                .toolContext(Map.of(
                        TalepTools.PENDING_ACTION_ID_SINK, bekleyenIslemIdleri,
                        TalepTools.KULLANILAN_ARAC_SINK, kullanilanAraclar,
                        TalepTools.YAPISAL_VERI_SINK, yapisalVeriListesi))
                .advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, conversationId);
                    if (!kaynaklar.isEmpty()) {
                        a.advisors(questionAnswerAdvisor);
                    }
                })
                .stream()
                .chatClientResponse()
                .mapNotNull(yanit -> {
                    String parca = metniCikar(yanit);
                    if (parca == null || parca.isEmpty()) {
                        return null;
                    }
                    birikenMetin.append(parca);
                    return ServerSentEvent.builder(parca).event("token").build();
                });

        Flux<ServerSentEvent<String>> kaynakOlayi = Flux.defer(() -> {
            if (kaynaklar.isEmpty()) {
                return Flux.empty();
            }
            try {
                String json = objectMapper.writeValueAsString(kaynaklar);
                return Flux.just(ServerSentEvent.builder(json).event("kaynaklar").build());
            } catch (JsonProcessingException e) {
                return Flux.empty();
            }
        });

        Flux<ServerSentEvent<String>> bekleyenIslemOlayi = Flux.defer(() -> {
            PendingActionOzeti bekleyenIslem = bekleyenIslemBul(bekleyenIslemIdleri);
            if (bekleyenIslem == null) {
                return Flux.empty();
            }
            try {
                String json = objectMapper.writeValueAsString(bekleyenIslem);
                return Flux.just(ServerSentEvent.builder(json).event("bekleyenIslem").build());
            } catch (JsonProcessingException e) {
                return Flux.empty();
            }
        });

        Flux<ServerSentEvent<String>> araclarOlayi = Flux.defer(() -> {
            List<String> araclar = benzersiz(kullanilanAraclar);
            if (araclar.isEmpty()) {
                return Flux.empty();
            }
            try {
                String json = objectMapper.writeValueAsString(araclar);
                return Flux.just(ServerSentEvent.builder(json).event("araclar").build());
            } catch (JsonProcessingException e) {
                return Flux.empty();
            }
        });

        Flux<ServerSentEvent<String>> yapisalVeriOlayi = Flux.defer(() -> {
            if (yapisalVeriListesi.isEmpty()) {
                return Flux.empty();
            }
            try {
                String json = objectMapper.writeValueAsString(yapisalVeriListesi.get(0));
                return Flux.just(ServerSentEvent.builder(json).event("yapisalVeri").build());
            } catch (JsonProcessingException e) {
                return Flux.empty();
            }
        });

        return Flux.concat(conversationIdOlayi, tokenOlaylari, kaynakOlayi, bekleyenIslemOlayi, araclarOlayi, yapisalVeriOlayi)
                .doFinally(sinyal -> {
                    llmSinirlayici.izinBirak();
                    List<String> araclar = benzersiz(kullanilanAraclar);
                    PendingActionOzeti bekleyenIslem = bekleyenIslemBul(bekleyenIslemIdleri);
                    YapisalVeriPaketi yapisalVeri = yapisalVeriListesi.isEmpty() ? null : yapisalVeriListesi.get(0);
                    if (!birikenMetin.isEmpty()) {
                        sohbetService.mesajEkle(
                                conversationId,
                                MesajRolu.ASISTAN,
                                birikenMetin.toString(),
                                bosMu(kaynaklar),
                                bosMu(araclar),
                                bekleyenIslem,
                                yapisalVeri);
                    }
                    log.info(
                            "Akisli sohbet yaniti tamamlandi: conversationId={}, mod={}, kaynakSayisi={}, kullanilanArac={}, sinyal={}",
                            conversationId,
                            mod,
                            kaynaklar.size(),
                            araclar,
                            sinyal);
                });
    }

    private SohbetModu modCoz(SohbetModu istekteki) {
        return istekteki == null ? SohbetModu.GENEL : istekteki;
    }

    private String sohbetIdCoz(String istekteki, Long personelId, SohbetModu mod) {
        if (istekteki != null && !istekteki.isBlank()) {
            return istekteki;
        }
        return sohbetService.sohbetBaslat(personelId, mod).getId();
    }

    private String sistemPromptuOverride(SohbetModu mod) {
        return switch (mod) {
            case GENEL -> null;
            case TALEP -> ChatClientConfig.TALEP_MODU_SISTEM_PROMPTU;
            case IMAR -> ChatClientConfig.IMAR_MODU_SISTEM_PROMPTU;
            case RUHSAT -> ChatClientConfig.RUHSAT_MODU_SISTEM_PROMPTU;
        };
    }

    private List<Kaynak> kaynaklarBul(String mesaj, SohbetModu mod) {
        return mod == SohbetModu.TALEP ? List.of() : ilgiliKaynaklariBul(mesaj, mod);
    }

    // GENEL modda sadece hicbir moda etiketlenmemis (paylasilan) belgeler
    // aranir - IMAR/RUHSAT gibi ozel modlara etiketlenmis belgeler GENEL'de
    // "gurultu" olmasin diye kapsam disinda tutulur (bkz. Dokuman.mod,
    // DokumanIngestService). IMAR/RUHSAT modlarinda ise SADECE o moda ait
    // belgeler aranir - her modun kendi izole belge havuzu olur.
    private Filter.Expression modFiltresi(SohbetModu mod) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        return switch (mod) {
            case GENEL -> b.eq("mod", "").build();
            case IMAR -> b.eq("mod", SohbetModu.IMAR.name()).build();
            case RUHSAT -> b.eq("mod", SohbetModu.RUHSAT.name()).build();
            case TALEP -> null;
        };
    }

    private <T> List<T> bosMu(List<T> liste) {
        return liste.isEmpty() ? null : liste;
    }

    private List<String> benzersiz(List<String> liste) {
        return liste.stream().distinct().toList();
    }

    private PendingActionOzeti bekleyenIslemBul(List<String> bekleyenIslemIdleri) {
        if (bekleyenIslemIdleri.isEmpty()) {
            return null;
        }
        return pendingActionService
                .getir(bekleyenIslemIdleri.get(0))
                .map(a -> new PendingActionOzeti(a.id(), a.tur(), a.takipNo(), a.aciklama()))
                .orElse(null);
    }

    private List<Kaynak> ilgiliKaynaklariBul(String mesaj, SohbetModu mod) {
        SearchRequest.Builder aramaIstegi = SearchRequest.builder()
                .query(mesaj)
                .topK(topK)
                .similarityThreshold(benzerlikEsigi);

        Filter.Expression filtre = modFiltresi(mod);
        if (filtre != null) {
            aramaIstegi.filterExpression(filtre);
        }

        List<Document> belgeler = vectorStore.similaritySearch(aramaIstegi.build());
        return belgeler.stream()
                .map(belge -> new Kaynak(
                        String.valueOf(belge.getMetadata().getOrDefault("baslik", "Bilinmeyen belge")),
                        belge.getMetadata().get("chunkIndex") instanceof Number sayi ? sayi.intValue() : 0,
                        belge.getScore()))
                .toList();
    }

    private String metniCikar(ChatClientResponse yanit) {
        var cevap = yanit.chatResponse();
        if (cevap == null || cevap.getResults().isEmpty()) {
            return null;
        }
        return cevap.getResult().getOutput().getText();
    }
}
