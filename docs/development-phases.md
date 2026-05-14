# RetroAI — Geliştirme Fazları

Bu proje Claude Code multi-agent pipeline'ı üzerinden 5 fazda geliştirildi. Her fazın çıktısı bir sonraki fazın girdisi.

## Faz 0 — Triage

**Aktör:** Orchestrator (`claude-sonnet-4-6`)

- `workspace/` dizini oluşturuldu.
- Takım kuruldu: `TeamCreate("dev-team")` + her faz için `TaskCreate`.
- Figma URL henüz yok → PM ajanı sorması için yönlendirildi.
- Dış API URL'i için placeholder bırakıldı (`AI_API_KEY` env'den, ATLASSIAN OAuth değişkenleri sonradan).

## Faz 0.5 — Product Manager

**Aktör:** `product-manager` (Opus)

**Çıktı:** `specs/pm-brief.md`

- RetroAI ürün vizyonu netleştirildi.
- 25 MVP özelliği listelendi (retro çekirdek, AI özellikleri, Jira entegrasyonu, x-factor'lar).
- 5 X-Factor seçildi: Action Risk Score, Carry-Over Intelligence, Pattern Detector, Jira History Analizi, Ekip Olgunluk Skoru.
- Kapsam dışı maddeler açıkça listelendi (WebSocket, Admin paneli, vb.).
- Hibrit Jira mimarisi belirlendi: REST (CRUD) + MCP (AI analizi).

PM tamamlanınca pipeline kullanıcı Figma URL'i sağlayana kadar bekledi. Figma URL geldikten sonra orchestrator:
1. `mcp__figma__get_figma_data` → `specs/figma-raw.txt`
2. `mcp__figma__download_figma_images` → `frontend/wireframes/figma-screens/`
3. parse script → `specs/figma-parsed.md`

## Faz 1 — Product Owner

**Aktör:** `product-owner` (Opus)

**Çıktılar:** `specs/` altında 5 markdown + `frontend/wireframes/` altında HTML wireframe'ler

- `requirements.md` — 33 feature, user story + kabul kriterleri.
- `api-contract.md` — 40+ endpoint, method/path/request/response/status codes.
- `data-model.md` — Entity şemaları, ilişkiler, enum'lar.
- `design-tokens.md` — Figma-parsed.md'den renk, tipografi, spacing token'ları.
- `screen-inventory.md` — Tüm ekranlar, navigasyon haritası, bileşen listesi.
- `frontend/wireframes/*.html` — her ekran için HTML wireframe.

Done-when kontrolü: 5 md > 1KB, wireframe'ler mevcut, hiçbir spec'te belirsiz ifade yok.

## Faz 2 — Geliştirme (Paralel)

PO biter bitmez `backend-java-dev` ve `frontend-dev` **aynı anda** başlatıldı.

### Faz 2a — Backend Dev

**Aktör:** `backend-java-dev` (Opus)

**Çıktı:** `src/main/java/...`

- Spring Boot 3.x + Java 17 + H2 in-memory
- Controller / Service / Repository / Entity / DTO paket yapısı
- `api-contract.md`'deki her endpoint için `@RestController` metodu
- JWT auth + Atlassian OAuth + Guest session yönetimi
- Jira REST entegrasyonu + Mock fallback
- Anthropic API client (Haiku 4.5)
- `mvn compile` başarılı

### Faz 2b — Frontend Dev

**Aktör:** `frontend-dev` (Opus)

**Çıktı:** `frontend/src/...`

- React 18 + Vite + Tailwind CSS
- `screen-inventory.md`'deki her route için sayfa komponenti
- `design-tokens.md` renkleri CSS custom property olarak
- API client modülü `VITE_API_URL` üzerinden
- 5sn polling (participation), faz-bazlı UI durumları
- `npm run build` başarılı

## Faz 3 — DevOps

**Aktör:** `devops` (Sonnet)

- `mvn clean package -DskipTests` → güncel jar
- Eski process durduruldu, yeni jar başlatıldı
- `/actuator/health` → `{"status":"UP"}` doğrulandı
- `npm run build && npx serve -s build -l 3000` → 200 doğrulandı
- Spec conformance: `api-contract.md`'deki endpoint'ler BE'de grep ile, `screen-inventory.md`'deki route'lar FE'de glob ile doğrulandı
- QA'e "BE hazır" mesajı atıldı

## Faz 4 — QA

**Aktör:** `qa-engineer` (Sonnet)

- **Smoke Test:** Tüm endpoint'lere `curl`, 200 + boş olmayan JSON
- **API Testleri:**
  - GET: response şeması kontrol
  - POST/PUT: geçersiz payload → 400, geçerli → 200/201
  - Hata durumları: olmayan ID → 404, yanlış faz → 423
- Bug'lar `backend-java-dev`'e geri iletildi, en fazla 3 tur regression döndü.

## Öğrenilenler

- **Dosya sahipliği yazma izinleriyle zorlandı** → ajanlar arası dosya çakışması olmadı.
- **Paralel BE+FE** süreyi yarıya indirdi; PO kontratı yeterince netti.
- **Mock fallback** demo riskini sıfırladı — Jira çalışmasa bile UI akar.
- **Haiku 4.5'in latency'si** retro akışı için kritikti; Opus tercih edilseydi 30s timeout'a sürekli takılırdı.
