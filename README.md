# RetroAI — Retro & Action Tracker

## Proje Özeti

RetroAI, agile takımların sprint retrospektifini tek platformda toplayan ve alınan aksiyonları **otomatik olarak Jira backlog'una** taşıyarak "unutulan aksiyon" problemini sistemik olarak çözen bir Retro & Action Tracker'dır. AI; tema kümeleme, SMART aksiyon üretimi, unutulma risk skoru, ekip olgunluk skoru ve Jira ticket geçmişi analizi üretir. Atlassian Jira ile çift yönlü entegredir (REST API ile CRUD + MCP ile AI analizi).

## Özellikler

- **Çoklu Ekip Desteği** — N tane ekip, her birinin ayrı retro geçmişi ve Jira bağlantısı.
- **Retro Oturumu** — Ekip + sprint adı + retro adı ile yeni retro açma.
- **4 Sütunlu Kart Tahtası** — `İyi Giden / Geliştirilebilir / Tartışılacak / Devam Adımları` kolonları.
- **4 Fazlı Akış** — `WRITING → GROUPING → VOTING → DISCUSSION → CLOSED`.
- **Misafir Katılımı** — Login'siz, `displayName` ile UUID guest token üzerinden katılım.
- **Anonim Kart Modu** — Varsayılan açık; kart yazarı "Anonim üye" görünür.
- **Eşit Oy Kotası** — Üye başına retro başına 3 oy.
- **Katılım Göstergesi** — Renkli noktalarla üye katkı durumu (5sn polling).
- **Ekip Lideri Rolü** — Jira bağlantısını yalnızca lider kurar.
- **Board Sahibi Yetkileri** — Faz geçişi, AI analiz tetikleme, bulk Jira gönderim yalnızca retroyu açan kişi.
- **Aksiyon Onay Ekranı** — AI önerilerini çekboks listesinde düzenle/onayla.
- **Bulk Jira Ticket Yaratma** — Onaylanan aksiyonların tamamı paralel olarak Jira'ya yazılır.
- **Tekil Manuel Ticket Yaratma** — Sonradan eklenen aksiyon için tek-tık ticket.
- **Otomatik Carry-Over** — Önceki retrodaki kapanmamış aksiyonlar yeni retroya otomatik taşınır.
- **Açılış Brifingi** — Önceki retro özeti + tekrar eden tema uyarısı.
- **Mock Jira Fallback** — Jira bağlantısı kurulamadığında statik mock JSON ile akış kesintisiz.
- **Atlassian OAuth ile Giriş** — Email/parola yerine Atlassian hesabıyla tek-tık giriş.

## AI Özellikleri & X-Factor

| # | Özellik | Açıklama |
|---|---------|----------|
| 1 | **Tema Kümeleme + SMART Aksiyon** | Kartları okur, benzerlerini kümeler, başlık + moral skoru + aciliyet atar; SMART aksiyon önerileri üretir. |
| 2 | **Action Risk Score** | Her aksiyona 1–5 unutulma risk skoru; gerekçe + AI rewrite önerisi. |
| 3 | **Carry-Over Intelligence** | Kapanmamış aksiyonları otomatik taşır, "Sprint N'den devreden" etiketi koyar. |
| 4 | **Pattern Detector** | Tema 2+ retroda tekrarladığında "kök neden analizi" uyarısı. |
| 5 | **Jira Ticket History Analizi** | `expand=changelog` ile status bouncing, long carryover, reopened bug, long idle sinyalleri çıkarır. |
| 6 | **Ekip Olgunluk Skoru** | 0–100 skor + `Forming/Norming/Performing/Mastery` seviye + 3 maddelik iyileştirme planı. |
| 7 | **Sessiz Katılımcı Dürtüsü** | 10 dakikada 0 kart yazan üyeye, yalnızca ona görünür AI-üretimi başlangıç sorusu. |

## Kullanılan AI Araçları

- **Claude Code (claude-sonnet-4-6)** — orkestrasyon, multi-agent pipeline yönetimi, kod üretimi.
- **Claude Opus 4.7 (claude-opus-4-7)** — Product Manager, Product Owner, Backend Dev, Frontend Dev agent'larında derin tasarım & implementasyon kararları.
- **Claude Haiku 4.5 (claude-haiku-4-5-20251001)** — AI analiz endpoint'leri (tema kümeleme, risk skoru, olgunluk skoru, silent-prompt, jira-history).
- **Atlassian MCP** — Jira veri analizi (changelog/yorum/sprint geçmişi yorumlama).

## Mimari

- **Backend:** Java 17 + Spring Boot 3.x, H2 in-memory DB (PostgreSQL'e geçişe hazır)
- **Frontend:** React 18 + Vite + Tailwind CSS
- **AI:** Anthropic API (Claude Haiku 4.5)
- **Jira Entegrasyonu:** Atlassian REST API (CRUD) + Atlassian MCP (AI analiz)
- **Auth:** JWT (email+parola) + Atlassian OAuth 2.0 (3LO) + Guest session UUID

## Kurulum

### Gereksinimler
- Java 17+
- Node.js 18+
- Maven 3.8+

### Backend

```bash
cd workspace
AI_API_KEY=your_anthropic_key mvn spring-boot:run
# Çalışır: http://localhost:8081
```

### Frontend

```bash
cd workspace/frontend
cp .env.example .env  # VITE_API_URL düzenle
npm install
npm run dev
# Çalışır: http://localhost:3000
```

### .env.example (frontend)
```
VITE_API_URL=http://localhost:8081
```

### .env.example (backend)
```
AI_API_KEY=your_anthropic_api_key
ATLASSIAN_CLIENT_ID=your_atlassian_client_id
ATLASSIAN_CLIENT_SECRET=your_atlassian_client_secret
ATLASSIAN_REDIRECT_URI=https://your-domain/api/auth/atlassian/callback
ATLASSIAN_FRONTEND_SUCCESS_URL=https://your-frontend/auth/callback
```

## API Endpoint'leri (Özet)

| Grup | Endpoint | Method | Açıklama |
|------|----------|--------|----------|
| Auth | `/api/auth/register` | POST | Kullanıcı kaydı |
| Auth | `/api/auth/login` | POST | Email + parola giriş |
| Auth | `/api/auth/me` | GET | Oturumdaki kullanıcı |
| Auth | `/api/auth/logout` | POST | Çıkış |
| Auth | `/api/auth/atlassian` | GET | Atlassian OAuth başlat (302) |
| Auth | `/api/auth/atlassian/callback` | GET | OAuth callback |
| Team | `/api/teams` | GET/POST | Ekip listele / oluştur |
| Team | `/api/teams/{id}` | GET | Ekip detayı + üyeler |
| Team | `/api/teams/{id}/members` | POST | Üye ekle (LEADER) |
| Team | `/api/teams/{id}/members/{uid}` | DELETE | Üye çıkar (LEADER) |
| Retro | `/api/retros` | GET/POST | Retro listele / aç |
| Retro | `/api/retros/{id}` | GET/PATCH | Detay / güncelle (board sahibi) |
| Retro | `/api/retros/{id}/participation` | GET | Katılım göstergesi |
| Retro | `/api/retros/{id}/phase` | POST | Faz geçişi (board sahibi) |
| Retro | `/api/retros/{id}/join/lookup` | GET | Misafir token doğrulama |
| Retro | `/api/retros/{id}/join` | POST | Misafir displayName + session |
| Card | `/api/retros/{id}/cards` | GET/POST | Kart listele / ekle |
| Card | `/api/cards/{id}` | PATCH/DELETE | Kart düzenle / sil |
| Action | `/api/actions/from-card` | POST | Next-Steps kartından aksiyon |
| Action | `/api/actions/bulk` | POST | Aksiyon onayla (board sahibi) |
| Action | `/api/actions` | GET | Aksiyonları filtrele |
| Action | `/api/actions/{id}` | PATCH/DELETE | Aksiyon güncelle / sil |
| Vote | `/api/cards/{id}/vote` | POST/DELETE | Oy ver / geri çek |
| AI | `/api/ai/analyze` | POST | Tema kümeleme + SMART aksiyon |
| AI | `/api/ai/risk-score` | POST | Unutulma risk skoru |
| AI | `/api/ai/maturity` | POST | Ekip olgunluk skoru |
| AI | `/api/ai/briefing` | GET | Açılış brifingi |
| AI | `/api/ai/silent-prompt` | GET | Sessiz katılımcı dürtüsü |
| AI | `/api/ai/jira-history` | POST | Jira ticket geçmiş analizi |
| Jira | `/api/jira/connect` | POST | Adım 1: credential + board listesi |
| Jira | `/api/jira/connect/board` | POST | Adım 2: board seç |
| Jira | `/api/jira/boards` | GET | Aktif bağlantıdaki board'lar |
| Jira | `/api/jira/connections` | POST | Tek-adımlı bağlantı (geriye dönük) |
| Jira | `/api/jira/connections/active` | GET | Aktif bağlantı bilgisi |
| Jira | `/api/jira/connections/{id}` | DELETE | Bağlantıyı kaldır |
| Jira | `/api/jira/sprint-context` | GET | Aktif sprint özeti |
| Jira | `/api/jira/issue` | POST | Tekil ticket yarat |
| Jira | `/api/jira/bulk-create` | POST | Bulk ticket yarat (board sahibi) |

Detaylı kontrat için: [`specs/api-contract.md`](./specs/api-contract.md)

## Demo Credentials

| Alan | Değer |
|------|-------|
| Email | `ayse@example.com` |
| Şifre | `demo1234` |

## Deploy URL

- **Frontend:** https://test.epochmakers.us
- **Backend API:** https://api.epochmakers.us

## AI Araçları & MCP

- **Claude Code CLI** (`claude-sonnet-4-6`) — multi-agent orchestrator
- **Atlassian MCP** — `https://mcp.atlassian.com/v1/mcp` (Jira AI analizi için)

## Ekran Görüntüleri

`screenshots/` klasörü altında demo ekranları yer alır (placeholder).

## Geliştirme Süreci

Bu proje **AI-first** bir yaklaşımla geliştirildi. Claude Code multi-agent pipeline kullanıldı:

```
Product Manager → Product Owner → (Backend Dev ∥ Frontend Dev) → DevOps → QA
```

Her ajan, önceki ajanın çıktısını okuyarak kendi rolünü icra etti. Detaylar için [`docs/development-phases.md`](./docs/development-phases.md) ve [`docs/ai-strategy.md`](./docs/ai-strategy.md).
