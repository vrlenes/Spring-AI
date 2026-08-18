# CLAUDE.md

## Proje amacı

Karatay Belediyesi personeli için kurum içi AI asistanı. İki somut iş yapar:
1. Mevzuat/prosedür sorularını yüklenen PDF'lerden kaynak göstererek cevaplar (RAG).
2. Vatandaş taleplerini listeler, arar, sınıflandırır, müdürlüğe atar, durum günceller (tool calling).

Tam spesifikasyon: `claude_dosya/belediye-ai-asistan-proje-dokumani.md`. Fazlar sırayla
uygulanır, her faz kendi başına sunulabilir olmalı.

## Paket yapısı

```
tr.gov.karatay.asistan
├── AsistanApplication.java
├── config/          # ChatClient bean'i, advisor zinciri (Faz 1+)
├── chat/            # REST + SSE endpoint'leri, ChatService (Faz 1+)
├── rag/             # PDF ingest, doküman entity/repository (Faz 2+)
├── talep/           # Talep, TalepNotu entity/repository, TalepService, TalepTools (Faz 3+)
├── mudurluk/        # Mudurluk entity/repository/service
└── common/          # GlobalExceptionHandler, enums (TalepDurumu, TalepOnceligi)
```

## Frontend

`frontend/` — React + Vite + TypeScript + Tailwind + shadcn/ui. Proje dokümanı Thymeleaf
öngörüyordu, mentör önerisiyle (daha zengin bir arayüz için) React'e geçildi — bu
dokümandan bilinçli bir sapma. `npm run build` çıktısı doğrudan
`src/main/resources/static`'e yazılır (`vite.config.ts`), Spring Boot bunu tek süreçte
API'yle birlikte sunar; ayrı deploy/CORS yok. `npm run dev` sırasında `/api/*` istekleri
aynı config'teki proxy ile `localhost:8080`'e yönlendirilir.

- `src/lib/chatStream.ts` — `/api/chat/stream` SSE gövdesini fetch + ReadableStream ile
  elle ayrıştırır (native `EventSource` POST desteklemediği için).
- `src/components/chat/` — sunum bileşenleri, iş mantığı içermez.
- `src/components/ui/` — shadcn/ui'nin ürettiği temel bileşenler; sadece gerçekten
  kullanılanlar tutulur, kullanılmayan bileşen eklenmez.

## Mimari kuralları (zorunlu)

- **`*Tools` sınıfları iş mantığı içermez.** `@Tool` anotasyonlu metotlar sadece ilgili
  `*Service` sınıfını çağıran ince bir sarmalayıcı olmalı. Bu, iş mantığının LLM'den
  bağımsız test edilebilmesini ve aynı mantığın hem chat'ten hem REST API'den
  kullanılabilmesini sağlar.
- **Sağlayıcıya özel sınıf import etme.** Sadece Spring AI'ın `ChatClient`, `ChatModel`,
  `EmbeddingModel` arayüzleri kullanılır — `OllamaChatModel` gibi somut sınıflar asla
  doğrudan kullanılmaz. Sebep: model sağlayıcısı ileride sadece config değişikliğiyle
  değişebilmeli. Proje Faz 5'i (Ollama'ya geçiş) planlanandan erken, baştan itibaren
  uyguluyor — hem chat hem embedding Ollama üzerinden, tamamen yerel ve ücretsiz, KVKK
  açısından güvenli. `application.yml` içindeki `spring.ai.ollama.*` ayarları istisna —
  o config, kod değil.
- **Tool ID yerine `takipNo` kullanır.** Entity döndürmez, sade DTO döndürür. Liste dönen
  tool'lara sert limit (max 20) koyar. Hata durumunda exception fırlatmak yerine anlamlı
  bir metin döndürür.
- **Yazma işlemleri (atama, durum güncelleme) önce kullanıcı onayı gerektirir**, okuma
  işlemleri (listeleme, arama) gerektirmez. Bu kural system prompt'ta uygulanır.
- Kaynak gösterimi (RAG) **koddan** üretilir, LLM'e "kaynak söyle" demekle değil —
  hallüsinasyon riskini kapatmak için retrieve edilen doküman metadata'sı kullanılır.

## Çalıştırma komutları (Windows / PowerShell)

```powershell
docker compose up -d
cd frontend && npm run build && cd ..   # backend'in sunacagi statigi uretir
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

## Diğer notlar

- Dosyalar UTF-8. Türkçe karakter sorunlarına dikkat.
- Spring AI API'si sürümler arası ciddi değişiyor. Kod yazmadan önce
  `https://docs.spring.io/spring-ai/reference/` üzerinden güncel dokümantasyonu kontrol et,
  bu dokümandaki örnekleri değil.
- Bir tasarım kararı gerekirse önce kullanıcıya sor, tek başına yön değiştirme.
