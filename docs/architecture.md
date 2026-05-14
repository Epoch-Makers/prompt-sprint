# RetroAI — Mimari

## Genel Bakış

RetroAI dört ana katmandan oluşur: React frontend, Spring Boot REST backend, Anthropic Claude API (AI motor) ve Atlassian Jira (REST + MCP). Backend ve frontend birbirinden bağımsız deploy edilir; iletişim REST/JSON üzerinden gider.

```
┌────────────────────────┐      ┌────────────────────────┐
│   React 18 + Vite      │◄────►│  Spring Boot 3.x       │
│   Tailwind CSS         │ HTTP │  Java 17               │
│   localhost:3000       │ JSON │  localhost:8081        │
└────────────────────────┘      └────────────────────────┘
                                    │             │
                                    ▼             ▼
                         ┌──────────────┐  ┌───────────────┐
                         │ Anthropic    │  │ Atlassian     │
                         │ Claude API   │  │ Jira REST     │
                         │ (Haiku 4.5)  │  │ + MCP         │
                         └──────────────┘  └───────────────┘
```

## Backend Katmanı

- **Framework:** Spring Boot 3.x (Web, Security, Data JPA, Validation)
- **Dil:** Java 17 (records, pattern matching, sealed classes)
- **Veritabanı:** H2 in-memory (varsayılan); şema PostgreSQL-uyumlu, migration ile geçişe hazır.
- **Auth:** JWT (HS256) + Atlassian OAuth 2.0 (3LO) + Guest session UUID (in-memory map).
- **Paket yapısı:** `controller / service / repository / entity / dto / config / security`.
- **Token güvenliği:** Jira API token plaintext olarak DB'ye yazılmaz; session memory'de tutulur.
- **Yapı:** Stateless REST; her endpoint kendi yetki kontrolünü yapar (faz kilidi, board sahibi, lider).

## Frontend Katmanı

- **Framework:** React 18 + Vite 5
- **Stil:** Tailwind CSS + design-tokens.md'den gelen CSS custom property'leri (hardcoded renk yok).
- **Routing:** React Router v6; korunan route'lar `RequireAuth` HOC ile sarmalanır.
- **State:** Lokal component state + lightweight context (auth, aktif ekip). Real-time için 5sn polling.
- **API çağrıları:** Tek bir `apiClient` modülü `VITE_API_URL` üzerinden. Bearer token + `X-Guest-Session` header'ları merkezi olarak set edilir.

## AI Katmanı

Anthropic API üzerinden Claude Haiku 4.5 çağrılır. Şu endpoint'ler AI çağrısı yapar:

| Endpoint | Görev |
|----------|-------|
| `POST /api/ai/analyze` | Tema kümeleme + SMART aksiyon üretimi |
| `POST /api/ai/risk-score` | 1–5 unutulma risk skoru |
| `POST /api/ai/maturity` | 0–100 ekip olgunluk skoru |
| `GET /api/ai/briefing` | Önceki retro özeti + tekrar eden tema |
| `GET /api/ai/silent-prompt` | Sessiz katılımcıya özel soru |
| `POST /api/ai/jira-history` | Jira changelog/yorum/sprint analizi |

Tüm AI çağrıları 30 saniyelik timeout'a sahiptir; aşıldığında 504 döner. Sonuçlar mümkün olduğunda DB'ye persist edilir (örn. risk skoru, olgunluk skoru) ve tekrar hesaplama gerektirmez.

## Jira Entegrasyon Katmanı

İki paralel kanal:

1. **REST kanalı (CRUD + Auth)**
   - `GET /rest/api/3/myself` — token doğrulama
   - `GET /rest/agile/1.0/board` — board listesi
   - `GET /rest/agile/1.0/board/{id}/sprint?state=active` — aktif sprint
   - `GET /rest/agile/1.0/sprint/{id}/issue?expand=changelog,comments` — sprint kartları + geçmiş
   - `POST /rest/api/3/issue` — aksiyon → Jira ticket

2. **MCP kanalı (AI analizi)**
   - `https://mcp.atlassian.com/v1/mcp` üzerinden LLM'in Jira verisini dinamik yorumlaması.
   - Velocity korelasyonu, cross-sprint pattern detection.
   - Yazma işlemleri MCP'den yapılmaz — sadece okuma & yorumlama.

**Fallback:** REST çağrısı 401/timeout aldığında statik mock JSON döner; response'a `mock: true` flag eklenir, frontend "Mock Mode" rozeti gösterir. Demo kesintisiz devam eder.

## Veri Modeli (Özet)

`User → TeamMembership → Team → RetroSession → Card / Vote / Action → JiraConnection`

- `RetroSession.currentPhase` ∈ `{WRITING, GROUPING, VOTING, DISCUSSION, CLOSED}`
- `Card.column` ∈ `{GOOD, IMPROVE, DISCUSS, NEXT_STEPS}`
- `Card.source` ∈ `{USER, JIRA_AI, AI_NEXT_STEP}`
- `Action.status` ∈ `{OPEN, IN_PROGRESS, DONE, AT_RISK}`
- `Action.riskScore` ∈ `{1..5}` (AI tarafından doldurulur)
- `JiraConnection.status` ∈ `{PENDING_BOARD, CONNECTED, DISCONNECTED, MOCK}`

Detay için `specs/data-model.md`.

## Deploy Topolojisi

- Frontend → `https://test.epochmakers.us` (statik dist)
- Backend → `https://api.epochmakers.us` (jar + reverse proxy)
- H2 default; PostgreSQL geçişi `database-engineer` agent'ı ile yapılır.
