package tr.gov.karatay.asistan.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import tr.gov.karatay.asistan.chat.dto.ChatRequest;
import tr.gov.karatay.asistan.chat.dto.ChatResponse;
import tr.gov.karatay.asistan.chat.dto.Kaynak;
import tr.gov.karatay.asistan.chat.dto.KaynakDogrulamaSonucu;
import tr.gov.karatay.asistan.chat.dto.YapisalVeriPaketi;
import tr.gov.karatay.asistan.common.CokFazlaIstekException;
import tr.gov.karatay.asistan.common.LlmEsZamanliSinirlayici;
import tr.gov.karatay.asistan.common.YapayZekaGeciciHataException;
import tr.gov.karatay.asistan.common.YapayZekaHataYorumlayici;
import tr.gov.karatay.asistan.common.enums.AracGrubu;
import tr.gov.karatay.asistan.common.enums.MesajRolu;
import tr.gov.karatay.asistan.common.enums.SohbetModu;
import tr.gov.karatay.asistan.config.ChatClientConfig;
import tr.gov.karatay.asistan.rag.KaynakDogrulamaService;
import tr.gov.karatay.asistan.rag.RagTools;
import tr.gov.karatay.asistan.sohbet.SohbetService;
import tr.gov.karatay.asistan.sohbet.dto.EkVerisi;
import tr.gov.karatay.asistan.talep.PendingActionService;
import tr.gov.karatay.asistan.talep.TalepTools;
import tr.gov.karatay.asistan.talep.dto.PendingActionOzeti;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    // Sohbete eklenen dosya (gorsel/PDF) kalici RAG doküman havuzuna
    // (Dokuman/vector_store) DAHIL EDILMEZ - sadece bu mesajin Gemini'ye
    // multimodal icerik olarak gonderilmesi icindir (bkz. ekIcerigiHazirla).
    // Global multipart siniri (application.yml) 25MB'dir; burasi Gemini'ye
    // inline gonderilen icerik icin daha siki bir is kurali siniri uygular.
    private static final Set<String> IZIN_VERILEN_EK_TURLERI =
            Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");
    private static final long EK_MAKS_BOYUT_BYTE = 8L * 1024 * 1024;

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
    //
    // RAG DE ARTIK BIR ARAC (bkz. RagTools.belgeAra): eskiden burada SABIT,
    // TEK SEFERLIK bir on-arama yapilip QuestionAnswerAdvisor ile prompt'a
    // enjekte ediliyordu - model arama basarisiz olsa bile tekrar arayamiyordu
    // (canli test: "ada parsel nasil belirlenir" sorusu, cevap belgede
    // Madde 18'de acikca varken ilk aramada eslesmedigi icin "bulunamadi"
    // sonucu veriyordu). Artik ChatService kaynaklari KENDISI ARAMIYOR -
    // sadece RagTools.belgeAra'nin (model tarafindan istendigi kadar
    // cagrilan) sonuclarini bir sink uzerinden toplayip koda dayali
    // "kaynaklar" listesini olusturuyor.
    private final ChatClient chatClient;
    private final PendingActionService pendingActionService;
    private final SohbetService sohbetService;
    private final ObjectMapper objectMapper;
    private final LlmEsZamanliSinirlayici llmSinirlayici;
    private final ModYonlendirmeService modYonlendirmeService;
    private final KaynakDogrulamaService kaynakDogrulamaService;

    public ChatService(
            ChatClient chatClient,
            PendingActionService pendingActionService,
            SohbetService sohbetService,
            ObjectMapper objectMapper,
            LlmEsZamanliSinirlayici llmSinirlayici,
            ModYonlendirmeService modYonlendirmeService,
            KaynakDogrulamaService kaynakDogrulamaService) {
        this.chatClient = chatClient;
        this.pendingActionService = pendingActionService;
        this.sohbetService = sohbetService;
        this.objectMapper = objectMapper;
        this.llmSinirlayici = llmSinirlayici;
        this.modYonlendirmeService = modYonlendirmeService;
        this.kaynakDogrulamaService = kaynakDogrulamaService;
    }

    public ChatResponse yanitla(ChatRequest istek, Long personelId) {
        return yanitla(istek, personelId, null);
    }

    public ChatResponse yanitla(ChatRequest istek, Long personelId, MultipartFile dosya) {
        SohbetModu secilenMod = modCoz(istek.mod());
        SohbetModu mod = secilenMod == SohbetModu.OTOMATIK ? modYonlendirmeService.yonlendir(istek.mesaj()) : secilenMod;
        String conversationId = sohbetIdCoz(istek.conversationId(), personelId, secilenMod);
        sohbetService.modGuncelle(conversationId, secilenMod);
        List<String> bekleyenIslemIdleri = new ArrayList<>();
        List<String> kullanilanAraclar = new ArrayList<>();
        List<YapisalVeriPaketi> yapisalVeriListesi = new ArrayList<>();
        List<Kaynak> kaynakListesi = new ArrayList<>();
        List<String> hamMetinListesi = new ArrayList<>();

        EkIcerik ekIcerik = ekIcerigiHazirla(dosya);
        sohbetService.mesajEkle(
                conversationId,
                MesajRolu.KULLANICI,
                istek.mesaj(),
                null,
                null,
                null,
                null,
                ekIcerik == null ? null : ekIcerik.kayit());

        // .system(...) SADECE GENEL disi modlarda cagirilir: Spring AI'de bu
        // metot cagrilirsa builder'daki defaultSystem'i o istek icin
        // DEGISTIRIR - hicbir zaman "bos" bir Consumer ile cagirmiyoruz,
        // aksi halde GENEL modun ana sistem promptu kaybolabilir.
        ChatClient.ChatClientRequestSpec istekSpec = chatClient.prompt().user(us -> kullaniciMesajiOlustur(us, istek.mesaj(), ekIcerik));
        String promptOverride = sistemPromptuOlustur(mod, istek.kapaliAraclar());
        if (promptOverride != null) {
            istekSpec = istekSpec.system(promptOverride);
        }
        final ChatClient.ChatClientRequestSpec sonIstekSpec = istekSpec
                .toolContext(Map.of(
                        TalepTools.PENDING_ACTION_ID_SINK, bekleyenIslemIdleri,
                        TalepTools.KULLANILAN_ARAC_SINK, kullanilanAraclar,
                        TalepTools.YAPISAL_VERI_SINK, yapisalVeriListesi,
                        RagTools.KAYNAK_SINK, kaynakListesi,
                        RagTools.HAM_METIN_SINK, hamMetinListesi,
                        RagTools.MOD_KEY, mod))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));

        ChatClientResponse yanit = anaCagriYap(sonIstekSpec);

        String cevapMetni = metniCikar(yanit);
        List<String> araclar = benzersiz(kullanilanAraclar);
        List<Kaynak> kaynaklar = benzersiz(kaynakListesi);
        PendingActionOzeti bekleyenIslem = bekleyenIslemBul(bekleyenIslemIdleri);
        YapisalVeriPaketi yapisalVeri = yapisalVeriListesi.isEmpty() ? null : yapisalVeriListesi.get(0);
        KaynakDogrulamaSonucu dogrulama = kaynakDogrulamaService.dogrula(cevapMetni, hamMetinListesi);
        Long mesajId = sohbetService.mesajEkle(
                conversationId, MesajRolu.ASISTAN, cevapMetni, bosMu(kaynaklar), bosMu(araclar), bekleyenIslem, yapisalVeri);

        log.info(
                "Sohbet yaniti uretildi: conversationId={}, mod={}, kaynakSayisi={}, kullanilanArac={}",
                conversationId,
                mod,
                kaynaklar.size(),
                araclar);

        SohbetModu algilananMod = secilenMod == SohbetModu.OTOMATIK ? mod : null;
        return new ChatResponse(
                conversationId, cevapMetni, kaynaklar, bekleyenIslem, araclar, yapisalVeri, algilananMod, mesajId, dogrulama);
    }

    public Flux<ServerSentEvent<String>> akisliYanitla(ChatRequest istek, Long personelId) {
        return akisliYanitla(istek, personelId, null);
    }

    public Flux<ServerSentEvent<String>> akisliYanitla(ChatRequest istek, Long personelId, MultipartFile dosya) {
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

        SohbetModu secilenMod = modCoz(istek.mod());
        SohbetModu mod = secilenMod == SohbetModu.OTOMATIK ? modYonlendirmeService.yonlendir(istek.mesaj()) : secilenMod;
        String conversationId = sohbetIdCoz(istek.conversationId(), personelId, secilenMod);
        sohbetService.modGuncelle(conversationId, secilenMod);
        List<String> bekleyenIslemIdleri = new ArrayList<>();
        List<String> kullanilanAraclar = new ArrayList<>();
        List<YapisalVeriPaketi> yapisalVeriListesi = new ArrayList<>();
        List<Kaynak> kaynakListesi = new ArrayList<>();
        List<String> hamMetinListesi = new ArrayList<>();
        StringBuilder birikenMetin = new StringBuilder();

        EkIcerik ekIcerik = ekIcerigiHazirla(dosya);
        sohbetService.mesajEkle(
                conversationId,
                MesajRolu.KULLANICI,
                istek.mesaj(),
                null,
                null,
                null,
                null,
                ekIcerik == null ? null : ekIcerik.kayit());

        Flux<ServerSentEvent<String>> conversationIdOlayi = Flux.just(
                ServerSentEvent.builder(conversationId).event("conversationId").build());

        // Sadece OTOMATIK modda gonderilir - frontend bunu token akisi
        // baslamadan once alip "Otomatik: X modu algilandi" rozetini hemen
        // gosterebilsin diye conversationId'den hemen sonra, token'lardan once
        // sıraya konur (bkz. asagidaki Flux.concat).
        Flux<ServerSentEvent<String>> algilananModOlayi = secilenMod == SohbetModu.OTOMATIK
                ? Flux.just(ServerSentEvent.builder(mod.name()).event("algilananMod").build())
                : Flux.empty();

        ChatClient.ChatClientRequestSpec akisIstekSpec =
                chatClient.prompt().user(us -> kullaniciMesajiOlustur(us, istek.mesaj(), ekIcerik));
        String akisPromptOverride = sistemPromptuOlustur(mod, istek.kapaliAraclar());
        if (akisPromptOverride != null) {
            akisIstekSpec = akisIstekSpec.system(akisPromptOverride);
        }

        Flux<ServerSentEvent<String>> tokenOlaylari = akisIstekSpec
                .toolContext(Map.of(
                        TalepTools.PENDING_ACTION_ID_SINK, bekleyenIslemIdleri,
                        TalepTools.KULLANILAN_ARAC_SINK, kullanilanAraclar,
                        TalepTools.YAPISAL_VERI_SINK, yapisalVeriListesi,
                        RagTools.KAYNAK_SINK, kaynakListesi,
                        RagTools.HAM_METIN_SINK, hamMetinListesi,
                        RagTools.MOD_KEY, mod))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .chatClientResponse()
                .mapNotNull(yanit -> {
                    String parca = metniCikar(yanit);
                    if (parca == null || parca.isEmpty()) {
                        return null;
                    }
                    birikenMetin.append(parca);
                    return ServerSentEvent.builder(parca).event("token").build();
                })
                // Akis basladiktan (HTTP 200 gonderildikten) SONRA bir hata olursa
                // (orn. Gemini kota/oran siniri) HTTP durum kodu artik degistirilemez -
                // tek yol, akisin icine ozel bir "hata" olayi eklemek. Bu olmadan
                // istemci akisin sessizce (yarim kalmis bir cevapla) bittigini
                // gorurdu - hicbir sey yanlis gitmemis gibi.
                .onErrorResume(RuntimeException.class, e -> {
                    RuntimeException yorumlanan = YapayZekaHataYorumlayici.yorumla(e);
                    log.warn("Akisli sohbet cagrisi hata ile sonuclandi: {}", e.getMessage());
                    String mesaj = yorumlanan instanceof YapayZekaGeciciHataException
                            ? yorumlanan.getMessage()
                            : "Bir hata oluştu, lütfen tekrar deneyin.";
                    return Flux.just(ServerSentEvent.builder(mesaj).event("hata").build());
                });

        Flux<ServerSentEvent<String>> kaynakOlayi = Flux.defer(() -> {
            List<Kaynak> kaynaklar = benzersiz(kaynakListesi);
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

        // tokenOlaylari tamamen bitince (birikenMetin artik tam) calisir - ayni
        // kaynakOlayi/araclarOlayi'nin dayandigi varsayimla (tool cagrilari
        // metin akisindan once tamamlanir, bkz. yukaridaki yorumlar).
        Flux<ServerSentEvent<String>> dogrulamaOlayi = Flux.defer(() -> {
            List<Kaynak> kaynaklar = benzersiz(kaynakListesi);
            if (kaynaklar.isEmpty() || birikenMetin.isEmpty()) {
                return Flux.empty();
            }
            KaynakDogrulamaSonucu sonuc = kaynakDogrulamaService.dogrula(birikenMetin.toString(), hamMetinListesi);
            if (sonuc == null) {
                return Flux.empty();
            }
            try {
                String json = objectMapper.writeValueAsString(sonuc);
                return Flux.just(ServerSentEvent.builder(json).event("dogrulama").build());
            } catch (JsonProcessingException e) {
                return Flux.empty();
            }
        });

        return Flux.concat(
                        conversationIdOlayi,
                        algilananModOlayi,
                        tokenOlaylari,
                        kaynakOlayi,
                        dogrulamaOlayi,
                        bekleyenIslemOlayi,
                        araclarOlayi,
                        yapisalVeriOlayi)
                .doFinally(sinyal -> {
                    llmSinirlayici.izinBirak();
                    List<String> araclar = benzersiz(kullanilanAraclar);
                    List<Kaynak> kaynaklar = benzersiz(kaynakListesi);
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

    // Google GenAI SDK'sinin bazen firlattigi genel "Failed to generate
    // content" hatasi Spring AI'nin kendi retry mekanizmasi tarafindan
    // YAKALANMAZ (bkz. SpringAiRetryAutoConfiguration - sadece
    // TransientAiException/ResourceAccessException'i tekrar dener, bu duz
    // bir RuntimeException). Toplu degerlendirme scripti (bkz.
    // scripts/rag-degerlendirme.js) bunu 18 soruda 3 kez yakaladi - genelde
    // gecici gorunuyor, bu yuzden burada kendi basit tekrar deneme
    // mantigimizi ekliyoruz: bir kez basarisiz olursa kisa bir bekleme
    // sonrasi TEKRAR dener, ikinci deneme de basarisiz olursa (mevcut
    // davranis gibi) hatayi oldugu gibi yukari firlatir - GlobalExceptionHandler
    // zaten bunu temiz bir "beklenmeyen hata" mesajina ceviriyor.
    private static final int ANA_CAGRI_MAKS_DENEME = 2;
    private static final long ANA_CAGRI_BEKLEME_MS = 1500;

    private ChatClientResponse anaCagriYap(ChatClient.ChatClientRequestSpec istekSpec) {
        RuntimeException sonHata = null;
        for (int deneme = 1; deneme <= ANA_CAGRI_MAKS_DENEME; deneme++) {
            try {
                return llmSinirlayici.sinirliCagir(() -> istekSpec.call().chatClientResponse());
            } catch (RuntimeException e) {
                sonHata = e;
                log.warn(
                        "Ana sohbet cagrisi basarisiz (deneme {}/{}): {}",
                        deneme,
                        ANA_CAGRI_MAKS_DENEME,
                        e.getMessage());
                if (deneme < ANA_CAGRI_MAKS_DENEME) {
                    try {
                        Thread.sleep(ANA_CAGRI_BEKLEME_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw sonHata;
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

    private static final String RAG_KAPALI_KURALI = "\n\nEK KURAL: Kullanıcı bu mesaj için mevzuat arama "
            + "aracını (belgeAra) kapattı - bu aracı KULLANMA, sadece genel bilginle (belirterek) cevap ver.";
    private static final String TALEP_KAPALI_KURALI = "\n\nEK KURAL: Kullanıcı bu mesaj için talep araçlarını "
            + "(talepleriGetir, talebiMudurlugeAta, talepDurumGuncelle, talepOncelikGuncelle, talebeNotEkle) "
            + "kapattı - bunları KULLANMA.";
    private static final String KURUM_DIZIN_KAPALI_KURALI = "\n\nEK KURAL: Kullanıcı bu mesaj için kurum dizini "
            + "araçlarını (mudurlukIletisimGetir, personelAra) kapattı - bunları KULLANMA.";

    // "Araçlar" panelinden gelen kapaliAraclar, mod bazli sistem promptuna EK
    // kural olarak eklenir - ayni TALEP/IMAR/RUHSAT modlarinin belgeAra/talep
    // araclarini kisitlamasiyla ayni, kanitlanmis mekanizma (prompt tabanli,
    // sert bir tool-kaldirma degil). kapaliAraclar bossa mevcut davranis
    // (GENEL'de override yok, digerlerinde kendi mod promptu) aynen korunur.
    private String sistemPromptuOlustur(SohbetModu mod, Set<AracGrubu> kapaliAraclar) {
        String temelPrompt = switch (mod) {
            case GENEL -> ChatClientConfig.SISTEM_PROMPT;
            case TALEP -> ChatClientConfig.TALEP_MODU_SISTEM_PROMPTU;
            case IMAR -> ChatClientConfig.IMAR_MODU_SISTEM_PROMPTU;
            case RUHSAT -> ChatClientConfig.RUHSAT_MODU_SISTEM_PROMPTU;
            case OTOMATIK -> throw new IllegalStateException(
                    "OTOMATIK modu bir gercek moda cozulmeden buraya ulasmamali.");
        };
        if (kapaliAraclar == null || kapaliAraclar.isEmpty()) {
            return mod == SohbetModu.GENEL ? null : temelPrompt;
        }
        StringBuilder prompt = new StringBuilder(temelPrompt);
        if (kapaliAraclar.contains(AracGrubu.RAG)) {
            prompt.append(RAG_KAPALI_KURALI);
        }
        if (kapaliAraclar.contains(AracGrubu.TALEP)) {
            prompt.append(TALEP_KAPALI_KURALI);
        }
        if (kapaliAraclar.contains(AracGrubu.KURUM_DIZIN)) {
            prompt.append(KURUM_DIZIN_KAPALI_KURALI);
        }
        return prompt.toString();
    }

    private <T> List<T> bosMu(List<T> liste) {
        return liste.isEmpty() ? null : liste;
    }

    private <T> List<T> benzersiz(List<T> liste) {
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

    // Media + EkVerisi'yi bir arada tasir: Media LLM'e gonderilecek multimodal
    // icerik, EkVerisi ayni baytlarin sohbet_mesaji'na kalici kaydi (gecmiste
    // tekrar goruntulenebilmesi icin) - ikisi de dosya.getBytes() tek sefer
    // okunarak kurulur.
    private record EkIcerik(Media media, EkVerisi kayit) {
    }

    private EkIcerik ekIcerigiHazirla(MultipartFile dosya) {
        if (dosya == null || dosya.isEmpty()) {
            return null;
        }
        String mimeTipi = dosya.getContentType();
        if (mimeTipi == null || !IZIN_VERILEN_EK_TURLERI.contains(mimeTipi)) {
            throw new IllegalArgumentException(
                    "Desteklenmeyen dosya türü: %s. İzin verilenler: JPEG/PNG/WEBP görsel veya PDF."
                            .formatted(mimeTipi));
        }
        if (dosya.getSize() > EK_MAKS_BOYUT_BYTE) {
            throw new IllegalArgumentException("Dosya boyutu 8MB sınırını aşıyor.");
        }
        byte[] veri;
        try {
            veri = dosya.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Dosya okunamadı.", e);
        }
        Media media = Media.builder()
                .mimeType(MimeTypeUtils.parseMimeType(mimeTipi))
                .data(new ByteArrayResource(veri))
                .name(dosya.getOriginalFilename())
                .build();
        return new EkIcerik(media, new EkVerisi(veri, mimeTipi, dosya.getOriginalFilename()));
    }

    private void kullaniciMesajiOlustur(ChatClient.PromptUserSpec us, String mesaj, EkIcerik ekIcerik) {
        us.text(mesaj == null ? "" : mesaj);
        if (ekIcerik != null) {
            us.media(ekIcerik.media());
        }
    }

    private String metniCikar(ChatClientResponse yanit) {
        var cevap = yanit.chatResponse();
        if (cevap == null || cevap.getResults().isEmpty()) {
            return null;
        }
        return cevap.getResult().getOutput().getText();
    }
}
