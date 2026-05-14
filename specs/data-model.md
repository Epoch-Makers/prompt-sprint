# RetroAI — Data Model

Bu dosya tüm entity'leri, alanlarını ve ilişkilerini tanımlar. Varsayılan veritabanı H2 in-memory.

---

## Entity Özet Listesi

1. **User** — sistemin kullanıcısı (LOCAL veya ATLASSIAN auth)
2. **Team** — ekip
3. **TeamMember** — User ↔ Team N:M ilişkisi (rol bilgisi taşır)
4. **RetroSession** — retro oturumu (currentPhase + guestJoinToken alanlarıyla)
5. **RetroCard** — retro kartı (GOOD / IMPROVE / DISCUSS / NEXT_STEPS)
6. **Vote** — kart oyu (user veya guest)
7. **Action** — retrodan çıkan aksiyon
8. **JiraConnection** — ekip Jira bağlantısı (PENDING_BOARD durumu desteklenir)
9. **JiraBoardCache** — son alınan board listesi (board seçimi için)
10. **TeamMaturityScore** — her retro sonrası hesaplanan olgunluk skoru
11. **AiThemeCluster** — AI'ın ürettiği tema kümeleri (Pattern Detector için persist)
12. **JiraInsight** — AI'ın Jira changelog analizinden çıkardığı insight
13. **GuestSession** — *(in-memory; persist edilmez)* misafir oturumu — token, displayName, retroId, kart sahipliği takibi için

---

## 1. User

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | Otomatik artan |
| email | VARCHAR(255) UNIQUE | ✓ | Giriş anahtarı |
| fullName | VARCHAR(120) | ✓ | Görünen ad |
| passwordHash | VARCHAR(255) | ✗ | BCrypt hash (LOCAL provider için zorunlu, ATLASSIAN için NULL) |
| authProvider | ENUM(`LOCAL`, `ATLASSIAN`) | ✓ | Default `LOCAL` |
| atlassianAccountId | VARCHAR(120) | ✗ | Atlassian'dan dönen `account_id` (ATLASSIAN provider için doldurulur) |
| createdAt | TIMESTAMP | ✓ | Kayıt tarihi |

**Constraint:** `authProvider=LOCAL` ise `passwordHash` NOT NULL. `authProvider=ATLASSIAN` ise `atlassianAccountId` NOT NULL.

---

## 2. Team

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| name | VARCHAR(120) | ✓ | Ekip adı |
| createdAt | TIMESTAMP | ✓ | |
| createdByUserId | BIGINT FK → User | ✓ | Oluşturan kullanıcı |

---

## 3. TeamMember

User ↔ Team N:M köprü tablosu. Rol bilgisi de buradadır.

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| teamId | BIGINT FK → Team | ✓ | |
| userId | BIGINT FK → User | ✓ | |
| role | ENUM(`LEADER`, `MEMBER`) | ✓ | Ekip lideri / üye |
| joinedAt | TIMESTAMP | ✓ | |

**Constraint:** `UNIQUE(teamId, userId)`

---

## 4. RetroSession

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| teamId | BIGINT FK → Team | ✓ | |
| sprintName | VARCHAR(120) | ✓ | "Sprint 24" |
| retroName | VARCHAR(200) | ✓ | "Sprint 24 Retro" |
| status | ENUM(`ACTIVE`, `CLOSED`) | ✓ | Default `ACTIVE` |
| currentPhase | ENUM(`WRITING`, `GROUPING`, `VOTING`, `DISCUSSION`, `CLOSED`) | ✓ | Default `WRITING` |
| phaseChangedAt | TIMESTAMP | ✗ | Son faz değişiklik zamanı |
| anonymousMode | BOOLEAN | ✓ | Default `true` |
| createdAt | TIMESTAMP | ✓ | |
| closedAt | TIMESTAMP | ✗ | Kapatıldığında doldurulur |
| createdByUserId | BIGINT FK → User | ✓ | **Board sahibi** — faz geçişi ve yönetim aksiyonları bu kullanıcıya bağlıdır |
| guestJoinToken | VARCHAR(36) UNIQUE | ✓ | UUID v4, misafir katılım linki için |

**Constraint:**
- Bir ekipte aynı anda yalnızca bir `ACTIVE` retro olabilir (uygulama düzeyinde 409).
- `status=CLOSED` set edilirse `currentPhase` otomatik `CLOSED` olur.
- `guestJoinToken` UNIQUE index.

---

## 5. RetroCard

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| retroId | BIGINT FK → RetroSession | ✓ | |
| authorUserId | BIGINT FK → User | ✗ | Kart sahibi (login'li üye); misafir kart için NULL |
| authorGuestSessionId | VARCHAR(36) | ✗ | Misafir oturumu UUID (login'li üye için NULL) |
| authorDisplayName | VARCHAR(120) | ✓ | Kart sahibinin gösterilen adı (login'li için User.fullName, misafir için join sırasında girilen displayName); anonim mod açıkken UI "Anonim üye" gösterir |
| content | TEXT | ✓ | Kart metni |
| column | ENUM(`GOOD`, `IMPROVE`, `DISCUSS`, `NEXT_STEPS`) | ✓ | 4. kolon `NEXT_STEPS` Devam Adımları |
| source | ENUM(`USER`, `JIRA_AI`, `AI_NEXT_STEP`) | ✓ | Default `USER`. `AI_NEXT_STEP` AI Analiz'in NEXT_STEPS'e düşürdüğü kartlar |
| createdAt | TIMESTAMP | ✓ | |
| updatedAt | TIMESTAMP | ✓ | |

**Constraint:**
- `authorUserId` ve `authorGuestSessionId` alanlarından **tam olarak biri** NOT NULL olmalı (XOR).
- Sahiplik kontrolünde her iki alan da kullanılır: login'li üye için `authorUserId == currentUserId`, misafir için `authorGuestSessionId == X-Guest-Session header`.

---

## 6. Vote

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| cardId | BIGINT FK → RetroCard | ✓ | |
| userId | BIGINT FK → User | ✗ | Oy veren (login'li üye); misafir için NULL |
| guestSessionId | VARCHAR(36) | ✗ | Misafir oy için UUID (login'li üye için NULL) |
| retroId | BIGINT FK → RetroSession | ✓ | Kota hesabı için (cache) |
| createdAt | TIMESTAMP | ✓ | |

**Constraint:**
- `userId` ve `guestSessionId` alanlarından **tam olarak biri** NOT NULL olmalı (XOR).
- `UNIQUE(cardId, userId)` — login'li üye aynı karta ikinci oy veremez (userId NOT NULL ise).
- `UNIQUE(cardId, guestSessionId)` — misafir aynı karta ikinci oy veremez.

**İş kuralı:** Kota = retro başına 3.
- Login'li üye: `COUNT(*) WHERE userId=X AND retroId=Y` ≤ 3.
- Misafir: `COUNT(*) WHERE guestSessionId=X AND retroId=Y` ≤ 3.

---

## 7. Action

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| retroId | BIGINT FK → RetroSession | ✓ | Hangi retroda doğdu |
| teamId | BIGINT FK → Team | ✓ | Hangi ekibe ait (denormalize, query kolaylığı) |
| title | VARCHAR(255) | ✓ | SMART aksiyon başlığı |
| description | TEXT | ✗ | Detay |
| assigneeUserId | BIGINT FK → User | ✗ | Atanan kişi |
| deadline | DATE | ✗ | Son tarih |
| status | ENUM(`OPEN`, `IN_PROGRESS`, `DONE`, `AT_RISK`) | ✓ | Default `OPEN` |
| riskScore | SMALLINT (1-5) | ✗ | AI tarafından hesaplanır |
| riskReason | TEXT | ✗ | AI gerekçesi |
| rewriteSuggestion | TEXT | ✗ | AI rewrite önerisi |
| jiraKey | VARCHAR(40) | ✗ | Jira'ya yazıldıysa key (örn. `PAY-1234`) |
| jiraUrl | VARCHAR(500) | ✗ | Tıklanabilir Jira URL |
| carriedFromRetroId | BIGINT FK → RetroSession | ✗ | Carry-over kaynak retro |
| carriedFromSprint | VARCHAR(120) | ✗ | Sprint adı (UI etiketi için) |
| source | ENUM(`AI_SUGGESTED`, `MANUAL`) | ✓ | Default `AI_SUGGESTED` |
| createdAt | TIMESTAMP | ✓ | |
| updatedAt | TIMESTAMP | ✓ | |

---

## 8. JiraConnection

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| teamId | BIGINT FK → Team | ✓ | |
| createdByUserId | BIGINT FK → User | ✓ | Bağlantıyı kuran lider |
| email | VARCHAR(255) | ✓ | Atlassian hesabı email |
| jiraDomain | VARCHAR(255) | ✓ | `mycompany.atlassian.net` |
| projectKey | VARCHAR(40) | ✗ | `PAY` (board seçilmeden önce NULL olabilir) |
| boardId | BIGINT | ✗ | Agile board id (Adım 2 tamamlanınca doldurulur) |
| boardName | VARCHAR(120) | ✗ | Board görünen adı (Adım 2 tamamlanınca doldurulur) |
| status | ENUM(`PENDING_BOARD`, `CONNECTED`, `DISCONNECTED`, `MOCK`) | ✓ | Adım 1 sonrası `PENDING_BOARD`, Adım 2 sonrası `CONNECTED` |
| createdAt | TIMESTAMP | ✓ | |

**Token persistence:** `apiToken` plaintext DB'ye yazılmaz. Session memory cache (`Map<connectionId, encryptedToken>`) backend süreç boyunca tutulur. Süreç restart sonrası lider yeniden token girer.

**Constraint:**
- `UNIQUE(teamId)` — bir ekibin yalnızca bir aktif bağlantısı olabilir.
- `status=CONNECTED` için `boardId` ve `projectKey` NOT NULL olmalı.

---

## 9. JiraBoardCache (yardımcı, opsiyonel persist)

Adım 1 (`POST /api/jira/connect`) sonrası dönen board listesi, kullanıcı Adım 2'yi tetikleyene kadar geçici olarak saklanır.

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| connectionId | BIGINT FK → JiraConnection | ✓ | |
| boardId | BIGINT | ✓ | Jira board id |
| boardName | VARCHAR(120) | ✓ | |
| boardType | VARCHAR(20) | ✓ | `scrum` / `kanban` |
| projectKey | VARCHAR(40) | ✓ | |
| fetchedAt | TIMESTAMP | ✓ | |

**Not:** Bu tablo persist edilse de yenilenir; her `POST /api/jira/connect` çağrısında ilgili connectionId için kayıtlar silinip yeniden eklenir.

---

## 10. TeamMaturityScore

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| teamId | BIGINT FK → Team | ✓ | |
| retroId | BIGINT FK → RetroSession | ✓ | Hangi retro sonrası |
| score | SMALLINT (0-100) | ✓ | |
| level | ENUM(`FORMING`, `NORMING`, `PERFORMING`, `MASTERY`) | ✓ | |
| actionCompletionRate | DECIMAL(3,2) | ✓ | 0.00 - 1.00 |
| smartness | DECIMAL(3,2) | ✓ | 0.00 - 1.00 |
| recurringIssueAbsence | DECIMAL(3,2) | ✓ | 0.00 - 1.00 |
| tipsJson | TEXT | ✓ | JSON array, 3 öneri |
| createdAt | TIMESTAMP | ✓ | |

---

## 11. AiThemeCluster

Pattern Detector ileride aynı tema'yı tespit edebilsin diye persist edilir.

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| retroId | BIGINT FK → RetroSession | ✓ | |
| teamId | BIGINT FK → Team | ✓ | |
| title | VARCHAR(255) | ✓ | "CI/CD yavaşlığı" |
| moralScore | SMALLINT (-5..+5) | ✓ | |
| urgency | ENUM(`LOW`, `MEDIUM`, `HIGH`) | ✓ | |
| cardIdsJson | TEXT | ✓ | JSON array of cardId |
| createdAt | TIMESTAMP | ✓ | |

---

## 12. JiraInsight

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| id | BIGINT PK | ✓ | |
| retroId | BIGINT FK → RetroSession | ✓ | |
| ticketKey | VARCHAR(40) | ✓ | "BUG-1453" |
| signalType | ENUM(`STATUS_BOUNCING`, `LONG_CARRYOVER`, `REOPENED_BUG`, `LONG_IDLE`) | ✓ | |
| description | TEXT | ✓ | Doğal dil açıklama |
| suggestedCardTitle | VARCHAR(255) | ✓ | "Geliştirilebilir" kolonuna düşecek kart başlığı |
| acceptedAsCardId | BIGINT FK → RetroCard | ✗ | Kullanıcı kabul ettiyse oluşan kart |
| createdAt | TIMESTAMP | ✓ | |

---

## 13. GuestSession (in-memory; persist edilmez)

Backend süreç boyunca tutulan bellek tablosudur. Süreç restart edilirse misafirler tekrar `POST /api/retros/{id}/join` çağrısı ile session açar.

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| guestSessionId | UUID (PK) | ✓ | Bir misafirin oturumunu benzersiz tanımlar |
| retroId | BIGINT | ✓ | Hangi retro için açıldı |
| displayName | VARCHAR(120) | ✓ | Misafirin join sırasında girdiği görünen ad |
| createdAt | TIMESTAMP | ✓ | |
| lastActivityAt | TIMESTAMP | ✓ | Son istek zamanı (idle takibi) |
| ipAddress | VARCHAR(45) | ✗ | Audit için |

**Constraint:**
- `UNIQUE(retroId, displayName)` — aynı retroda aynı isim ikinci kez kayıt olamaz.
- Retro `CLOSED` olunca o retro'ya ait tüm GuestSession kayıtları silinir veya invalid işaretlenir.

**Yaşam döngüsü:**
- `POST /api/retros/{id}/join` → yeni session yaratılır
- Retro `CLOSED` → tüm session'lar geçersiz, sonraki yazma çağrıları 410

---

## İlişki Diyagramı (Mermaid-stil)

```
User 1 ──< TeamMember >── N Team
User 1 ─── 1 (boardOwner) RetroSession   [createdByUserId]

Team 1 ──< RetroSession ──< RetroCard ──< Vote >── User
                  │              │              └─ guestSessionId (opt)
                  │              ├── source ∈ {USER, JIRA_AI, AI_NEXT_STEP}
                  │              ├── column ∈ {GOOD, IMPROVE, DISCUSS, NEXT_STEPS}
                  │              └── (authorUserId XOR authorGuestSessionId)
                  │
                  ├──< Action (assignee → User, jiraKey, riskScore)
                  ├──< AiThemeCluster
                  ├──< JiraInsight
                  ├──< GuestSession (in-memory)
                  └──< TeamMaturityScore

Team 1 ─── 1 JiraConnection ──< JiraBoardCache
```

### İlişkiler

| İlişki | Kardinalite |
|--------|-------------|
| User ↔ Team | N:M (TeamMember üzerinden) |
| User → RetroSession (board owner) | 1:N (RetroSession.createdByUserId) |
| Team → RetroSession | 1:N |
| RetroSession → RetroCard | 1:N |
| RetroCard → Vote | 1:N |
| User → Vote | 1:N |
| GuestSession → Vote | 1:N (in-memory bağlantı) |
| GuestSession → RetroCard | 1:N (in-memory bağlantı) |
| RetroSession → GuestSession | 1:N (in-memory) |
| RetroSession → Action | 1:N |
| User → Action (assignee) | 1:N (opsiyonel) |
| Team → JiraConnection | 1:1 |
| JiraConnection → JiraBoardCache | 1:N |
| RetroSession → AiThemeCluster | 1:N |
| RetroSession → JiraInsight | 1:N |
| Team → TeamMaturityScore | 1:N |
| RetroSession → TeamMaturityScore | 1:1 |
| RetroSession → RetroSession (carriedFrom) | self-reference (Action.carriedFromRetroId) |

---

## İndeksler

| Tablo | İndeks | Sebep |
|-------|--------|-------|
| User | `idx_user_email` (UNIQUE) | Login lookup |
| User | `idx_user_atlassian` (atlassianAccountId) | OAuth callback lookup |
| TeamMember | `idx_team_user` (teamId, userId) UNIQUE | Üyelik kontrolü |
| RetroSession | `idx_retro_team_status` (teamId, status) | Aktif retro araması |
| RetroSession | `idx_retro_guest_token` (guestJoinToken) UNIQUE | Misafir join lookup |
| RetroSession | `idx_retro_owner` (createdByUserId) | Board sahibi sorgusu |
| RetroCard | `idx_card_retro` (retroId) | Kart listesi |
| RetroCard | `idx_card_retro_column` (retroId, column) | Kolon bazlı filtre |
| RetroCard | `idx_card_guest_session` (authorGuestSessionId) | Misafir kart sahipliği |
| Vote | `idx_vote_card_user` (cardId, userId) UNIQUE (userId NOT NULL) | Login üye tek oy |
| Vote | `idx_vote_card_guest` (cardId, guestSessionId) UNIQUE (guestSessionId NOT NULL) | Misafir tek oy |
| Vote | `idx_vote_user_retro` (userId, retroId) | Login üye kalan oy |
| Vote | `idx_vote_guest_retro` (guestSessionId, retroId) | Misafir kalan oy |
| Action | `idx_action_team_status` (teamId, status) | Aksiyon filtre |
| Action | `idx_action_retro` (retroId) | Retro detayında listeleme |
| AiThemeCluster | `idx_theme_team_title` (teamId, title) | Pattern detector |
| JiraConnection | `idx_jira_team` (teamId) UNIQUE | Aktif bağlantı |
| JiraBoardCache | `idx_board_connection` (connectionId) | Board listeleme |

---

## Enum Özeti

| Enum | Değerler |
|------|----------|
| `AuthProvider` | LOCAL, ATLASSIAN |
| `TeamRole` | LEADER, MEMBER |
| `RetroStatus` | ACTIVE, CLOSED |
| `RetroPhase` | WRITING, GROUPING, VOTING, DISCUSSION, CLOSED |
| `RetroColumn` | GOOD, IMPROVE, DISCUSS, NEXT_STEPS |
| `CardSource` | USER, JIRA_AI, AI_NEXT_STEP |
| `ActionStatus` | OPEN, IN_PROGRESS, DONE, AT_RISK |
| `ActionSource` | AI_SUGGESTED, MANUAL |
| `JiraConnectionStatus` | PENDING_BOARD, CONNECTED, DISCONNECTED, MOCK |
| `MaturityLevel` | FORMING, NORMING, PERFORMING, MASTERY |
| `Urgency` | LOW, MEDIUM, HIGH |
| `JiraSignalType` | STATUS_BOUNCING, LONG_CARRYOVER, REOPENED_BUG, LONG_IDLE |
| `ParticipationStatus` | GREEN, YELLOW, GREY |
