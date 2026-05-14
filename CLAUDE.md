# RetroAI — Claude Code Konfigürasyonu

Bu proje Claude Code multi-agent pipeline ile geliştirilmiştir.

## AI Geliştirme Pipeline

- **Product Manager (Opus)** → `specs/pm-brief.md` (kapsam, RICE, X-Factor)
- **Product Owner (Opus)** → `specs/` (requirements, api-contract, data-model, design-tokens, screen-inventory, wireframes)
- **Backend Dev (Opus)** → `src/` (Spring Boot REST API)
- **Frontend Dev (Opus)** → `frontend/src/` (React + Vite + Tailwind)
- **DevOps (Sonnet)** → BE build + FE build + spec conformance
- **QA (Sonnet)** → smoke testler + API regresyon

## Kullanılan Modeller

- **Orchestrator:** `claude-sonnet-4-6`
- **Geliştirme ajanları:** `claude-opus-4-7`
- **AI endpoint'leri (ürün içi):** `claude-haiku-4-5-20251001`

## MCP Sunucuları

- **Atlassian MCP:** `https://mcp.atlassian.com/v1/mcp`
  - Geliştirme sürecinde Claude Code'un Jira'ya erişimi
  - Ürün içindeki AI analiz motorunun changelog/yorum/sprint geçmişini dinamik yorumlaması

## Dosya Sahipliği

| Dizin | Sahip |
|-------|-------|
| `specs/` | Sadece Product Owner yazar |
| `frontend/wireframes/` | Sadece Product Owner yazar |
| `src/` | Sadece Backend Dev yazar |
| `frontend/src/` | Sadece Frontend Dev yazar |
| `docs/` | Orchestrator / dokümantasyon |

## Workspace Yapısı

```
workspace/
├── specs/              ← PO çıktıları (pm-brief, requirements, api-contract, data-model, design-tokens, screen-inventory)
├── src/                ← Java backend
├── frontend/
│   ├── src/            ← React frontend
│   └── wireframes/     ← HTML wireframe'ler + figma-screens/
├── docs/               ← Mimari + AI stratejisi + faz dokümantasyonu
└── target/             ← Maven build çıktıları
```
