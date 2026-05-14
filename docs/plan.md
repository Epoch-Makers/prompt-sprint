# RetroAI — Proje Planı

## Vizyon

Agile takımlarda **retro aksiyonlarının unutulması** problemini sistemik olarak çözmek. Retro tahtasında konuşulan her şeyin AI tarafından SMART aksiyona dönüştürülüp, tek tıkla Jira backlog'una taşınması.

## Hedef Kitle

- Agile takımlar (Scrum / Kanban)
- Atlassian Jira kullanıcıları
- 4–15 kişilik geliştirme ekipleri
- Retro yapan ama aksiyonları takip edemeyen takımlar (X-Factor #2: Carry-Over Intelligence)

## Başarı Kriterleri

| Metrik | Hedef |
|--------|-------|
| Retro açılışından bulk Jira ticket'a kadar süre | < 15 dakika |
| AI tema kümeleme doğruluğu | > %80 (manuel review ile) |
| Carry-over otomasyonu | %100 — kullanıcıya sorulmadan |
| Demo akışı kesintisiz çalışma | %100 (Jira down olsa bile mock fallback) |
| Atlassian OAuth giriş başarısı | 1 tık |

## Geliştirme Fazları (Multi-Agent Pipeline)

```
Faz 0     │ Triage (Orchestrator)
Faz 0.5   │ Product Manager (Opus)        → specs/pm-brief.md
Faz 1     │ Product Owner (Opus)          → specs/* + wireframes
Faz 2a ∥ 2b │ Backend Dev + Frontend Dev (Opus, paralel)
Faz 3     │ DevOps (Sonnet)               → build + health + spec conformance
Faz 4     │ QA (Sonnet)                   → smoke + regresyon
```

### Faz 0 — Triage
- Workspace dizini, takım, task'lar oluşturuldu.
- Figma URL ve dış API URL'leri toplandı.

### Faz 0.5 — Product Manager
- **Çıktı:** `specs/pm-brief.md`
- 25 MVP özelliği, 5 X-Factor (Risk Score, Carry-Over, Pattern Detector, Jira History, Maturity Score).
- Hibrit Jira mimarisi: REST (CRUD) + MCP (AI analizi).
- Kapsam dışı maddeler kesin tanımlandı (WebSocket, Admin paneli, vb.).

### Faz 1 — Product Owner
- **Çıktılar:** `specs/requirements.md` (33 feature), `specs/api-contract.md` (40+ endpoint), `specs/data-model.md`, `specs/design-tokens.md` (Figma'dan), `specs/screen-inventory.md`, `frontend/wireframes/*.html`.

### Faz 2 — Geliştirme (Paralel)
- **Backend:** Spring Boot 3.x + Java 17 + H2 + Anthropic API client + Jira REST integrasyonu + Mock fallback.
- **Frontend:** React 18 + Vite + Tailwind + design-token CSS variables + 5sn polling + faz-bazlı UI.
- Dosya sahipliği ayrımı sayesinde paralel koştu, çakışma olmadı.

### Faz 3 — DevOps
- `mvn clean package -DskipTests` → güncel jar
- `/actuator/health` UP doğrulaması
- `npm run build && npx serve -s build` → 200 doğrulaması
- Spec conformance: api-contract endpoint'leri ↔ controller; screen-inventory route'ları ↔ FE sayfaları.

### Faz 4 — QA
- Smoke test: tüm endpoint'lere curl, 200 + boş olmayan JSON.
- Regresyon: 400/401/403/404/423 senaryoları.
- Bug'lar backend-java-dev'e geri iletilir, max 3 tur döngü.

## Teknik Borçlar & Post-MVP

| Madde | Neden ertelendi |
|-------|-----------------|
| WebSocket real-time | Polling demo için yeterli, complexity yüksek |
| Admin paneli / cross-team dashboard | MVP scope dışı |
| Git entegrasyonu (commit/PR) | Jira'ya odaklanıldı |
| Jira webhook | Çift yönlü sync gerekmiyor |
| PostgreSQL geçişi | H2 in-memory hackathon için yeterli; `database-engineer` agent hazır |
| Mobil uygulama | Web-first |
| Push notification / e-posta | İlerleyen sprint |

## Risk Yönetimi

- **Jira erişilemezse?** → Mock JSON fallback, response'a `mock: true` flag, UI "Mock Mode" rozeti.
- **AI timeout?** → 30s sınır, 504 + frontend "AI meşgul" + retry butonu.
- **MCP bağlantı yok?** → JiraHistoryService devre dışı; ürün geri kalan kısmı çalışmaya devam eder.
- **Token sızıntısı?** → Jira API token plaintext olarak DB'ye yazılmaz, session memory'de tutulur.

## Demo Senaryosu (5 dakika)

1. Atlassian OAuth ile giriş (1 tık).
2. "Payment Squad" ekibini seç → yeni retro aç ("Sprint 24").
3. Carry-over banner: "🔄 3 aksiyon önceki retrodan otomatik taşındı".
4. Açılış brifingi modal: "Bu 3. sprint'te CI/CD yavaşlığı konusu çıkıyor".
5. WRITING fazında kart ekleme + sessiz katılımcı dürtüsü demonstrasyon.
6. GROUPING fazı → AI Analiz tetikle → tema kümeleri + SMART aksiyonlar.
7. VOTING fazı → 3 oy hakkı kullan.
8. DISCUSSION fazı → aksiyon onay listesi → bulk Jira'ya gönder → 4 ticket oluştu, ticket key'leri görünür.
9. Aksiyonlar sayfası → 2 aksiyon kırmızı (risk score 5) → AI rewrite önerisi.
10. Olgunluk skoru: 72/100 (PERFORMING) + 3 iyileştirme tavsiyesi.
