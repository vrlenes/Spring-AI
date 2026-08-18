# Belediye AI Asistanı

Karatay Belediyesi personeli için Spring Boot + Spring AI tabanlı kurum içi AI asistanı.
Mevzuat/prosedür sorularını yüklenen dokümanlardan kaynak göstererek cevaplar (RAG) ve
vatandaş taleplerini araçlar (tool calling) üzerinden yönetir.

Detaylı proje spesifikasyonu için `claude_dosya/belediye-ai-asistan-proje-dokumani.md` dosyasına bakın.

## Teknoloji

- Java 21, Spring Boot 3.5.x, Spring AI 1.1.x
- PostgreSQL 16 + pgvector, Flyway, Spring Data JPA
- Frontend: React + Vite + TypeScript + Tailwind + shadcn/ui (`frontend/`), derlenip
  `src/main/resources/static`'e gömülür — proje dokümanındaki Thymeleaf planından
  sonradan (mentör önerisiyle, daha zengin bir arayüz için) buna geçildi. Backend tek
  süreçte hem API'yi hem derlenmiş frontend'i sunar, ayrı deploy/CORS gerekmez.

## Gereksinimler

- JDK 21+
- Node.js 20+ ve npm (frontend için)
- Docker Desktop (Windows'ta çalışır durumda olmalı)
- Bir chat API anahtarı (Faz 1'den itibaren gerekli; Faz 0'da gerekmez). Geliştirmede
  varsayılan olarak ücretsiz katmanı olan **Groq** kullanılıyor (kredi kartı gerekmiyor):
  https://console.groq.com/keys adresinden anahtar alın.

## Kurulum (Windows / PowerShell)

1. Ortam değişkenlerini hazırlayın:

   ```powershell
   Copy-Item .env.example .env
   # .env dosyasını açıp OPENAI_API_KEY değerini girin (Faz 1'den itibaren, Groq anahtarı)
   ```

2. Veritabanını ayağa kaldırın:

   ```powershell
   docker compose up -d
   ```

3. Frontend'i derleyin (backend'in sunacağı statik dosyaları üretir):

   ```powershell
   cd frontend
   npm install
   npm run build
   cd ..
   ```

4. Uygulamayı çalıştırın:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

5. Doğrulayın:

   ```powershell
   curl http://localhost:8080/actuator/health
   ```

   `{"status":"UP"}` dönmeli. Flyway migration'ları otomatik çalışır; tabloları
   DBeaver / pgAdmin ile `localhost:5433/belediye_asistan` (postgres/postgres) adresinden
   görebilirsiniz.

   Not: Postgres host portu **5433** olarak ayarlandı (standart 5432 yerine), çünkü bu
   makinede başka bir projenin Postgres konteyneri 5432'yi zaten kullanıyor. Kendi
   makinenizde çakışma yoksa `compose.yaml` ve `.env`'deki `DB_URL`'i 5432'ye çevirebilirsiniz.

## Frontend geliştirme (hot-reload)

Sadece frontend üzerinde çalışırken her seferinde `npm run build` + backend'i yeniden
başlatmak yerine:

```powershell
cd frontend
npm run dev
```

`http://localhost:5173` üzerinde açılır, kod değiştikçe anında güncellenir. `/api/*`
istekleri `vite.config.ts`'deki proxy ayarıyla otomatik olarak `localhost:8080`'e
yönlendirilir — backend'in (`.\mvnw.cmd spring-boot:run`) ayrıca çalışıyor olması yeterli.

## Testler

```powershell
.\mvnw.cmd test
```

Repository testleri Testcontainers ile geçici bir pgvector'lü Postgres konteyneri açar;
Docker'ın çalışıyor olması gerekir.

## Embedding modeli değiştirirseniz

`spring.ai.vectorstore.pgvector.dimensions` ayarı embedding modeline göre değişir
(`text-embedding-3-small` → 1536, `bge-m3` → 1024). Model değiştirdiğinizde vektör
tablosunu sıfırlayıp yüklü dokümanları yeniden ingest etmeniz gerekir.

## Mimari kuralları

Bkz. `CLAUDE.md`.

## Faz durumu

- [x] Faz 0 — İskelet
- [x] Faz 1 — Düz chat + hafıza (`/`, `/api/chat`, `/api/chat/stream`)
- [ ] Faz 2 — RAG
- [ ] Faz 3 — Tool calling
- [ ] Faz 4 — İyileştirme
- [ ] Faz 5 — Ollama geçişi (opsiyonel)
