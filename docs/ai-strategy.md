# RetroAI — AI Stratejisi

## Felsefe

RetroAI iki farklı bağlamda AI kullanır:

1. **Geliştirme sürecinde** — Claude Code multi-agent pipeline ile kod üretildi.
2. **Ürün içinde** — Çalışan uygulamada Claude Haiku 4.5 endpoint'leri retroya değer katar.

Her iki bağlamda da AI bir "asistan" değil, **iş akışının atomik adımı** olarak modellendi.

## Claude Code Multi-Agent Pipeline

Tek bir LLM oturumunda tüm projeyi yazmak yerine, her ajanın dar bir sorumluluğu olan bir pipeline kuruldu. Bu hem context window'u korur hem de ajanlar arası "kontrat" üzerinden ilerlemeyi mümkün kılar.

```
Product Manager (Opus)
   │   pm-brief.md, RICE skoru, X-Factor seçimi
   ▼
Product Owner (Opus)
   │   requirements.md, api-contract.md, data-model.md,
   │   design-tokens.md, screen-inventory.md, wireframes/
   ▼
Backend Dev (Opus) ∥ Frontend Dev (Opus)
   │   src/                  │  frontend/src/
   ▼                          ▼
       DevOps (Sonnet)
   │   mvn clean package, npm run build, spec conformance
   ▼
       QA (Sonnet)
       curl smoke + regression
```

### Kim Hangi Modeli Kullanır?

| Ajan | Model | Neden |
|------|-------|-------|
| Orchestrator | `claude-sonnet-4-6` | Hızlı karar verme, koordinasyon |
| Product Manager | `claude-opus-4-7` | Derin strateji & RICE analizi |
| Product Owner | `claude-opus-4-7` | Tutarlı spec + wireframe üretimi |
| Backend Dev | `claude-opus-4-7` | Karmaşık implementasyon kararları |
| Frontend Dev | `claude-opus-4-7` | Component mimarisi + Tailwind discipline |
| DevOps | `claude-sonnet-4-6` | Deterministik build & doğrulama |
| QA | `claude-sonnet-4-6` | Test scripting + raporlama |

### Dosya Sahipliği — Çakışmasız Paralellik

Pipeline'ın çalışabilmesi için her ajanın yazma izni dar tutuldu:

- PO → sadece `specs/` + `frontend/wireframes/`
- Backend → sadece `src/`
- Frontend → sadece `frontend/src/` (wireframe'lere dokunmaz)
- DevOps/QA → yazma yok, sadece okuma + build komutları

Bu sayede backend ve frontend ajanları **aynı anda** koşabildi.

## Ürün İçi AI Endpoint'leri

Tüm ürün-içi AI çağrıları Claude Haiku 4.5'e yapılır. Haiku seçimi bilinçli:

- **Düşük latency** — retro akışını blokladığı yer (analyze, briefing) çok kritik.
- **Maliyet** — bir retro oturumunda 5–10 AI çağrısı yapılıyor; Opus tutmazdı.
- **Yeterli kalite** — kümeleme + skor üretimi gibi yapılandırılmış görevler için Haiku 4.5 yeterli.

### Endpoint Bazlı Strateji

| Endpoint | Prompt Şekli | Çıktı Formatı | Validation |
|----------|--------------|---------------|------------|
| `/api/ai/analyze` | System + retro kartları (JSON) | Strict JSON: `themes[], actions[]` | Schema validation; başarısızsa retry 1 kez |
| `/api/ai/risk-score` | Aksiyon batch + sinyal listesi | `[{actionId, riskScore, reason, rewriteSuggestion}]` | `riskScore ∈ {1..5}` zorlanır |
| `/api/ai/maturity` | 3 metrik + son N retro özeti | `{score, level, components, tips[3]}` | `tips.length === 3` |
| `/api/ai/briefing` | Önceki retro + tema geçmişi | `{prevRetroSummary, counts, recurringThemes[]}` | İlk retro ise `null` field'lar |
| `/api/ai/silent-prompt` | Üye katılım durumu | `{prompt, shouldShow}` | Boş yanıtta varsayılan prompt |
| `/api/ai/jira-history` | Jira changelog + yorumlar | `[{ticketKey, signalType, description, suggestedCardTitle}]` | `signalType` enum sınırlı |

Tümünde 30s timeout, başarısızsa 504. JSON parse hatasında bir kez retry.

## Atlassian MCP Kullanımı

MCP'nin iki kullanım yeri var ve karıştırılmaması önemli:

1. **Geliştirme sürecinde** — Claude Code'un Jira'ya doğrudan erişimi (issue listeleme, ticket geçmişi okuma).
2. **Ürün içinde AI analiz motoru** — Çalışan uygulama, Atlassian MCP üzerinden LLM'in Jira verisini dinamik sorgulamasına izin verir.

**Yazma işlemleri (ticket oluşturma) her zaman REST üzerinden deterministik yapılır**, MCP üzerinden değil — çünkü MCP'nin dolaylı yazması istenmeyen ticket'lara yol açabilir.

## Demo Riskini Sıfırlama

AI ve Jira tarafında her şey "best effort":

- Jira REST 401/timeout → mock JSON fallback (`mock: true`).
- Anthropic API hata → 504 + frontend "AI şu an meşgul" mesajı; tekrar dene butonu.
- MCP bağlantı yok → JiraHistoryService devre dışı, UI panel gizlenir.

Demo'da hiçbir hata kullanıcıyı dead-end'e götürmez.
