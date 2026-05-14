# RetroAI — API Contract

**Base URL:** `http://localhost:8080`
**Content-Type:** `application/json`
**Auth:** Bearer token (JWT) header `Authorization: Bearer <token>` veya session cookie.

Tüm endpoint'ler — aşağıdaki istisnalar hariç — kimlik doğrulama gerektirir:
- `/api/auth/register`, `/api/auth/login`
- `/api/auth/atlassian`, `/api/auth/atlassian/callback`
- `GET /api/retros/{id}/join` (guest token ile, kimlik doğrulamadan)
- Misafir oturumu (`X-Guest-Session: <uuid>` veya `?guestSessionId=`) ile çalışan endpoint'ler — bkz. Bölüm 11.

---

## 1. Auth

### 1.1 POST /api/auth/register
Yeni kullanıcı oluştur.

**Request Body:**
```json
{
  "email": "ayse@example.com",
  "fullName": "Ayşe Yılmaz",
  "password": "secret123"
}
```

**Response 201:**
```json
{
  "id": 1,
  "email": "ayse@example.com",
  "fullName": "Ayşe Yılmaz"
}
```

**Hatalar:**
- `400` — email/fullName/password eksik veya geçersiz
- `409` — email zaten kayıtlı

---

### 1.2 POST /api/auth/login
Email + parola ile giriş yap.

**Request:**
```json
{ "email": "ayse@example.com", "password": "secret123" }
```

**Response 200:**
```json
{ "token": "eyJhbGciOi...", "user": { "id": 1, "email": "...", "fullName": "..." } }
```

**Hatalar:**
- `401` — kimlik bilgileri yanlış

---

### 1.3 GET /api/auth/me
Oturumdaki kullanıcıyı döner.

**Response 200:**
```json
{
  "id": 1,
  "email": "ayse@example.com",
  "fullName": "Ayşe Yılmaz",
  "authProvider": "LOCAL"
}
```

`authProvider` ∈ {`LOCAL`, `ATLASSIAN`}.

**Hatalar:**
- `401` — oturum yok

---

### 1.4 POST /api/auth/logout
Oturumu kapatır.

**Response 204:** (boş gövde)

---

### 1.5 GET /api/auth/atlassian
Atlassian OAuth 2.0 (3LO) authorize URL'sine 302 redirect yapar.

**Query Params:**
- `redirect` (opsiyonel) — login sonrası yönlendirilecek frontend path (default `/teams`)

**İşleyiş:**
- Backend `state` parametresini imzalı/random üretir, `oauth_state` cookie'sine yazar (HttpOnly, SameSite=Lax, 10dk).
- Atlassian authorize URL'sine `client_id`, `scope=read:me read:jira-work offline_access`, `redirect_uri`, `response_type=code`, `state` parametreleriyle 302 döner.

**Response 302:** `Location: https://auth.atlassian.com/authorize?...`

---

### 1.6 GET /api/auth/atlassian/callback
OAuth code'unu token'a takas eder, kullanıcıyı login eder.

**Query Params:**
- `code` (zorunlu)
- `state` (zorunlu)

**İşleyiş:**
1. `state` cookie ile karşılaştırılır; uymazsa 400.
2. `POST https://auth.atlassian.com/oauth/token` ile `code` → `access_token` + `refresh_token` takası yapılır.
3. `GET https://api.atlassian.com/me` çağrısı ile email, ad bilgisi alınır.
4. Email sistemde varsa `User` güncellenir; yoksa `passwordHash=NULL`, `authProvider=ATLASSIAN` ile yeni kullanıcı yaratılır.
5. JWT üretilir, `auth_token` cookie'sine yazılır.
6. Frontend `redirect` parametresine (veya `/teams`'e) 302 yönlendirilir.

**Response 302:** `Location: <frontend redirect URL>`

**Hatalar:**
- `400` — `state` uyuşmuyor veya `code` geçersiz
- `502` — Atlassian token endpoint hatası

---

## 2. Team

### 2.1 POST /api/teams
Yeni ekip oluştur. Oluşturan otomatik `LEADER` olur.

**Request:**
```json
{ "name": "Payment Squad" }
```

**Response 201:**
```json
{ "id": 1, "name": "Payment Squad", "createdAt": "2026-05-14T10:00:00Z", "myRole": "LEADER" }
```

---

### 2.2 GET /api/teams
Oturumdaki kullanıcının üyesi olduğu ekipleri listeler.

**Response 200:**
```json
[
  { "id": 1, "name": "Payment Squad", "memberCount": 5, "myRole": "LEADER" },
  { "id": 2, "name": "Search Team", "memberCount": 4, "myRole": "MEMBER" }
]
```

---

### 2.3 GET /api/teams/{teamId}
Ekip detayı + üye listesi.

**Response 200:**
```json
{
  "id": 1,
  "name": "Payment Squad",
  "members": [
    { "userId": 1, "fullName": "Ayşe", "email": "...", "role": "LEADER" },
    { "userId": 2, "fullName": "Burak", "email": "...", "role": "MEMBER" }
  ]
}
```

**Hatalar:**
- `403` — kullanıcı ekip üyesi değil
- `404` — ekip yok

---

### 2.4 POST /api/teams/{teamId}/members
Ekibe üye ekle (yalnızca `LEADER`).

**Request:**
```json
{ "email": "yeni@example.com" }
```

**Response 201:**
```json
{ "userId": 3, "fullName": "...", "email": "...", "role": "MEMBER" }
```

**Hatalar:**
- `403` — çağıran lider değil
- `404` — email'e sahip kullanıcı yok
- `409` — kullanıcı zaten ekipte

---

### 2.5 DELETE /api/teams/{teamId}/members/{userId}
Üyeyi ekipten çıkar (yalnızca `LEADER`).

**Response 204**

**Hatalar:** `403`, `404`

---

## 3. Retro Session

### 3.1 POST /api/retros
Yeni retro aç. Otomatik carry-over tetiklenir.

**Request:**
```json
{
  "teamId": 1,
  "sprintName": "Sprint 24",
  "retroName": "Sprint 24 Retro",
  "anonymousMode": true
}
```

**Response 201:**
```json
{
  "id": 10,
  "teamId": 1,
  "sprintName": "Sprint 24",
  "retroName": "Sprint 24 Retro",
  "status": "ACTIVE",
  "currentPhase": "WRITING",
  "anonymousMode": true,
  "createdAt": "2026-05-14T10:00:00Z",
  "createdByUserId": 1,
  "carriedOverActionCount": 3,
  "guestJoinToken": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "guestJoinUrl": "http://localhost:3000/join/f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

`createdByUserId` board sahibini gösterir (faz geçişi ve yönetim aksiyonları için yetki bu kullanıcıya verilir, bkz. F-33).
`guestJoinToken` her retroya özel UUID v4. Bu link paylaşılarak hesapsız katılım sağlanır (bkz. 3.6 ve 3.7).

**Hatalar:**
- `409` — ekipte zaten aktif retro var

---

### 3.2 GET /api/retros?teamId={id}
Ekibin retro listesini döner (yeniden eskiye sıralı).

**Response 200:**
```json
[
  { "id": 10, "sprintName": "Sprint 24", "status": "ACTIVE", "createdAt": "..." },
  { "id": 9, "sprintName": "Sprint 23", "status": "CLOSED", "createdAt": "..." }
]
```

---

### 3.3 GET /api/retros/{retroId}
Retro detayı + kart sayıları + üye katılımı.

**Response 200:**
```json
{
  "id": 10,
  "teamId": 1,
  "sprintName": "Sprint 24",
  "retroName": "Sprint 24 Retro",
  "status": "ACTIVE",
  "currentPhase": "WRITING",
  "anonymousMode": true,
  "createdByUserId": 1,
  "iAmBoardOwner": true,
  "cardCounts": { "GOOD": 4, "IMPROVE": 6, "DISCUSS": 2, "NEXT_STEPS": 3 },
  "myRemainingVotes": 2,
  "carriedOverActionCount": 3,
  "guestJoinUrl": "http://localhost:3000/join/f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

`cardCounts` 4 kolonun tümünü içerir: `GOOD`, `IMPROVE`, `DISCUSS`, `NEXT_STEPS`.
`iAmBoardOwner` çağırana göre hesaplanır; misafir çağrılarda `false`.

---

### 3.4 PATCH /api/retros/{retroId}
Retro alanlarını güncelle (anonim mod toggle, kapatma). **Yalnızca board sahibi.**

**Request:**
```json
{ "anonymousMode": false, "status": "CLOSED" }
```

`status` değeri `CLOSED` olarak set edilirse `currentPhase` otomatik `CLOSED` olur ve tüm guest oturumları geçersiz hale gelir.
Faz değişikliği için bu endpoint değil, `3.8 POST /api/retros/{id}/phase` kullanılmalıdır.

**Response 200:** Güncellenmiş retro objesi.

**Hatalar:**
- `403` — board sahibi değil veya misafir oturumu

---

### 3.5 GET /api/retros/{retroId}/participation
Katılım göstergesi için üye başına kart sayısı (polling).

**Response 200:**
```json
[
  { "userId": 1, "fullName": "Ayşe", "cardCount": 3, "status": "GREEN" },
  { "userId": 2, "fullName": "Burak", "cardCount": 1, "status": "YELLOW" },
  { "userId": 3, "fullName": "Can", "cardCount": 0, "status": "GREY" }
]
```

`status` ∈ {`GREEN` (≥2), `YELLOW` (=1), `GREY` (=0)}.

Misafir oturumları da bu listede yer alır; `userId` yerine `guestSessionId` döner ve `fullName` join sırasında misafirin girdiği `displayName` değerini taşır:
```json
[
  { "guestSessionId": "9b1f...", "fullName": "Ahmet K.", "cardCount": 2, "status": "GREEN", "isGuest": true }
]
```

---

### 3.6 GET /api/retros/{retroId}/join/lookup?token={guestJoinToken}
**Authentication: gerekmez.** Token geçerliliğini ve retro özetini döndürür — henüz session oluşturmaz. Misafire önce "Görünen adınızı girin" formu gösterilir.

**Response 200:**
```json
{
  "retroId": 10,
  "sprintName": "Sprint 24",
  "retroName": "Sprint 24 Retro",
  "teamName": "Payment Squad",
  "status": "ACTIVE",
  "currentPhase": "WRITING",
  "anonymousMode": true,
  "tokenValid": true
}
```

**Hatalar:**
- `404` — token geçersiz veya retro yok
- `410` — retro `CLOSED`

---

### 3.7 POST /api/retros/{retroId}/join
**Authentication: gerekmez.** Misafir displayName girer ve session başlatır.

**Request Body:**
```json
{
  "token": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "displayName": "Ahmet K."
}
```

**Validation:**
- `displayName` 2–40 karakter, harf/rakam/boşluk/`._-` izinli.
- Aynı retroda aynı `displayName` ikinci kez kayıt olamaz.

**İşleyiş:**
- Token doğrulanır; retro `status=ACTIVE` ve `currentPhase != CLOSED` olmalı.
- Yeni `guestSessionId` (UUID v4) üretilir, in-memory tabloya yazılır (TTL = retro `CLOSED` olana kadar).
- Frontend dönen `guestSessionId`'yi `sessionStorage`'da saklar; sonraki tüm yazma çağrılarında `X-Guest-Session: <uuid>` header'ı gönderir.

**Response 201:**
```json
{
  "retroId": 10,
  "guestSessionId": "9b1f2e4c-3b8e-4d9a-b1c2-78bd9c01ab23",
  "displayName": "Ahmet K.",
  "currentPhase": "WRITING",
  "myRemainingVotes": 3
}
```

**Hatalar:**
- `400` — `displayName` validation hatası
- `404` — token geçersiz
- `409` — aynı retroda aynı displayName kayıtlı (`{"error":"DISPLAY_NAME_TAKEN"}`)
- `410` — retro `CLOSED`

---

### 3.8 POST /api/retros/{retroId}/phase
**Yalnızca board sahibi** (retro `createdByUserId` = oturumdaki kullanıcı). Faz geçişi.

**Request:**
```json
{ "targetPhase": "GROUPING" }
```

`targetPhase` ∈ {`WRITING`, `GROUPING`, `VOTING`, `DISCUSSION`}. Geri dönüş (örn. `VOTING` → `GROUPING`) izinlidir.

**Response 200:**
```json
{
  "id": 10,
  "currentPhase": "GROUPING",
  "previousPhase": "WRITING",
  "changedAt": "2026-05-14T10:25:00Z"
}
```

**Hatalar:**
- `400` — geçersiz `targetPhase` (örn. doğrudan `CLOSED` set edilemez; bunun için `PATCH /api/retros/{id}` body `status=CLOSED` kullanılır)
- `403` — çağıran board sahibi değil veya misafir oturumu
- `404` — retro yok

---

## 4. Retro Card

### 4.1 POST /api/retros/{retroId}/cards
Yeni kart ekle.

**Request:**
```json
{
  "content": "Deploy süresi 40 dakikaya çıktı",
  "column": "IMPROVE",
  "source": "USER"
}
```

`column` ∈ {`GOOD`, `IMPROVE`, `DISCUSS`, `NEXT_STEPS`}.
`source` ∈ {`USER`, `JIRA_AI`, `AI_NEXT_STEP`}. Default `USER`.

**Faz kontrolü:** Bu endpoint yalnızca `currentPhase=WRITING` iken çalışır. Başka fazlarda 423 `PHASE_LOCKED` döner.

**Auth alternatifi:** Login'siz misafir oturumu için `X-Guest-Session: <guestSessionId>` header'ı yeterlidir (bkz. 3.7).

**Response 201:**
```json
{
  "id": 100,
  "retroId": 10,
  "content": "Deploy süresi 40 dakikaya çıktı",
  "column": "IMPROVE",
  "authorId": 1,
  "guestSessionId": null,
  "authorName": "Anonim üye",
  "source": "USER",
  "voteCount": 0,
  "createdAt": "..."
}
```

Misafir tarafından yaratılan kartlarda `authorId: null` ve `guestSessionId: "<uuid>"` döner.

---

### 4.2 GET /api/retros/{retroId}/cards
Retro'nun tüm kartlarını döner. Anonim mod açıksa `authorName` "Anonim üye".

**Response 200:**
```json
[
  {
    "id": 100,
    "content": "...",
    "column": "GOOD",
    "authorId": 1,
    "authorName": "Anonim üye",
    "source": "USER",
    "voteCount": 2,
    "myVoted": true,
    "createdAt": "..."
  }
]
```

---

### 4.3 PATCH /api/cards/{cardId}
Kart içeriğini güncelle (yalnızca sahibi — login'li kullanıcı veya kartın oluşturulduğu `guestSessionId`).

**Faz kontrolü:** Yalnızca `currentPhase=WRITING` veya `GROUPING` iken çalışır. `VOTING` ve `DISCUSSION` fazlarında 423 döner. Board sahibi `GROUPING` fazında kartı başka kolona (örneğin `NEXT_STEPS`'e) taşıyabilir.

**Request:**
```json
{ "content": "Güncellenmiş metin", "column": "DISCUSS" }
```

`column` değeri `NEXT_STEPS` olabilir (kartı 4. kolona taşımak).

**Response 200:** Güncellenmiş kart.

**Hatalar:** `403` — kart sahibi değil

---

### 4.4 DELETE /api/cards/{cardId}
Kartı sil (yalnızca sahibi — login'li veya guest).

**Response 204**

**Hatalar:** `403`

---

### 4.5 POST /api/actions/from-card
"Next Steps" kolonundaki bir karttan aksiyon oluştur (F-29).

**Request:**
```json
{
  "cardId": 142,
  "assigneeUserId": 2,
  "deadline": "2026-05-28"
}
```

`cardId` mutlaka `column=NEXT_STEPS` olan bir kart olmalıdır.

**Response 201:**
```json
{
  "id": 55,
  "retroId": 10,
  "title": "<kartın content değeri>",
  "description": null,
  "assigneeUserId": 2,
  "deadline": "2026-05-28",
  "status": "OPEN",
  "source": "MANUAL",
  "createdAt": "..."
}
```

**Hatalar:**
- `400` — kart `NEXT_STEPS` kolonunda değil
- `404` — kart bulunamadı

---

## 5. Vote

### 5.1 POST /api/cards/{cardId}/vote
Karta oy ver. Login'li kullanıcı veya `X-Guest-Session` header'ı kullanılabilir.

**Faz kontrolü:** Yalnızca `currentPhase=VOTING` iken çalışır. Diğer fazlarda 423 `PHASE_LOCKED` döner.

**Response 201:**
```json
{ "cardId": 100, "voteCount": 3, "myRemainingVotes": 1 }
```

**Hatalar:**
- `400` — oy hakkı kalmadı (`{"error":"NO_VOTES_LEFT"}`)
- `409` — bu karta zaten oy verildi
- `401` — ne login ne de geçerli guest session var
- `423` — faz `VOTING` değil

---

### 5.2 DELETE /api/cards/{cardId}/vote
Oyu geri çek. Guest session header'ı desteklenir. Faz `VOTING` olmalı.

**Response 200:**
```json
{ "cardId": 100, "voteCount": 2, "myRemainingVotes": 2 }
```

---

## 6. Action

### 6.1 POST /api/actions/bulk
AI'ın ürettiği aksiyonlardan onaylananları kaydet. **Yalnızca board sahibi.** Faz `DISCUSSION` olmalıdır (423 aksi halde).

**Request:**
```json
{
  "retroId": 10,
  "actions": [
    {
      "title": "Deploy pipeline optimizasyonu",
      "description": "...",
      "assigneeUserId": 2,
      "deadline": "2026-05-28"
    }
  ]
}
```

**Response 201:**
```json
[
  {
    "id": 50,
    "retroId": 10,
    "title": "...",
    "description": "...",
    "assigneeUserId": 2,
    "deadline": "2026-05-28",
    "status": "OPEN",
    "riskScore": null,
    "jiraKey": null,
    "carriedFromSprint": null,
    "createdAt": "..."
  }
]
```

---

### 6.2 GET /api/actions?teamId={id}&status={status}
Aksiyonları filtrele.

**Query Params:**
- `teamId` (zorunlu)
- `status` (opsiyonel) — `OPEN` / `IN_PROGRESS` / `DONE` / `AT_RISK`
- `retroId` (opsiyonel)

**Response 200:**
```json
[
  {
    "id": 50,
    "title": "...",
    "description": "...",
    "assigneeUserId": 2,
    "assigneeName": "Burak",
    "deadline": "2026-05-28",
    "status": "OPEN",
    "riskScore": 4,
    "riskReason": "Sahibi yok, 5 gündür hareket yok",
    "rewriteSuggestion": "Burak: Pipeline ı 2 dakikaya indir, deadline 28 Mayıs",
    "jiraKey": "PAY-1234",
    "carriedFromSprint": "Sprint 23",
    "createdAt": "..."
  }
]
```

---

### 6.3 PATCH /api/actions/{actionId}
Aksiyon güncelle (status, başlık, atama).

**Request:**
```json
{ "status": "DONE", "title": "Yeni başlık", "assigneeUserId": 3 }
```

**Response 200:** Güncellenmiş aksiyon.

---

### 6.4 DELETE /api/actions/{actionId}
Aksiyonu sil.

**Response 204**

---

## 7. AI

### 7.1 POST /api/ai/analyze
Retro kartlarını analiz et — tema kümeleri + SMART aksiyon önerileri. **Yalnızca board sahibi.**

**Faz kontrolü:** Yalnızca `currentPhase=GROUPING` iken çalışır.
**Yan etki:** Üretilen her aksiyon önerisi için `NEXT_STEPS` kolonuna `source=AI_NEXT_STEP` etiketli bir kart da otomatik oluşturulur (F-29).

**Request:**
```json
{ "retroId": 10 }
```

**Response 200:**
```json
{
  "themes": [
    {
      "title": "CI/CD yavaşlığı",
      "moralScore": -3,
      "urgency": "HIGH",
      "cardIds": [100, 102, 105]
    }
  ],
  "actions": [
    {
      "title": "Deploy pipeline'ı 10 dakikaya indir",
      "description": "...",
      "suggestedAssigneeUserId": 2,
      "suggestedDeadline": "2026-05-28",
      "themeTitle": "CI/CD yavaşlığı"
    }
  ]
}
```

**Hatalar:**
- `504` — analiz 30 saniyede tamamlanamadı

---

### 7.2 POST /api/ai/risk-score
Aksiyonlar için unutulma risk skoru hesapla.

**Request:**
```json
{ "actionIds": [50, 51, 52] }
```

**Response 200:**
```json
[
  {
    "actionId": 50,
    "riskScore": 4,
    "reason": "Sahibi yok, deadline yaklaşıyor",
    "rewriteSuggestion": "Burak: ..."
  }
]
```

---

### 7.3 POST /api/ai/maturity
Ekip olgunluk skoru hesapla. **Yalnızca board sahibi.** Faz `DISCUSSION` veya `CLOSED` olmalı.

**Request:**
```json
{ "retroId": 10 }
```

**Response 200:**
```json
{
  "score": 72,
  "level": "PERFORMING",
  "components": {
    "actionCompletionRate": 0.8,
    "smartness": 0.65,
    "recurringIssueAbsence": 0.7
  },
  "tips": [
    "Aksiyon başlıklarına ölçülebilir hedef ekle",
    "Sahipsiz aksiyon bırakma",
    "Tekrar eden CI/CD temasına kök neden oturumu planla"
  ]
}
```

---

### 7.4 GET /api/ai/briefing?retroId={id}
Açılış brifingi — önceki retro özeti + tekrar eden temalar.

**Response 200:**
```json
{
  "prevRetroSummary": "Geçen retroda 4 aksiyon vardı.",
  "doneCount": 2,
  "inProgressCount": 1,
  "atRiskCount": 1,
  "recurringThemes": [
    { "title": "CI/CD yavaşlığı", "occurrenceCount": 3 }
  ]
}
```

Eğer ilk retro ise `prevRetroSummary: null`, diğer alanlar 0.

---

### 7.5 GET /api/ai/silent-prompt?retroId={id}&userId={uid}
Sessiz katılımcıya gösterilecek başlangıç sorusu.

**Response 200:**
```json
{
  "prompt": "Bu sprintte seni en çok ne mutlu etti? Tek cümleyle yazabilir misin?",
  "shouldShow": true
}
```

`shouldShow: false` ise üye zaten kart yazmış demektir.

---

### 7.6 POST /api/ai/jira-history
Jira changelog + yorum + sprint geçmişi analizi.

**Request:**
```json
{ "retroId": 10 }
```

**Response 200:**
```json
{
  "insights": [
    {
      "ticketKey": "BUG-1453",
      "signalType": "STATUS_BOUNCING",
      "description": "3 kez 'In Progress'→'To Do' döndü",
      "suggestedCardTitle": "BUG-1453 gereksinim netleştirme retroda ele alınmalı"
    }
  ]
}
```

`signalType` ∈ {`STATUS_BOUNCING`, `LONG_CARRYOVER`, `REOPENED_BUG`, `LONG_IDLE`}.

---

## 8. Jira

Jira bağlantısı iki adımda yapılır (F-30):
1. `POST /api/jira/connect` — credential doğrula + board listesi al
2. `POST /api/jira/connect/board` — seçilen board'u kaydet

Geriye dönük uyumluluk için tek-adımlı `POST /api/jira/connections` endpoint'i de korunmuştur.

---

### 8.1 POST /api/jira/connect
**Adım 1:** Atlassian credential'ı doğrula ve erişilebilir board'ları döndür (yalnızca `LEADER`).

**Request:**
```json
{
  "teamId": 1,
  "email": "lead@company.com",
  "apiToken": "ATATT3xFfGF0...",
  "jiraDomain": "mycompany.atlassian.net"
}
```

**İşleyiş:**
1. Backend `GET https://{jiraDomain}/rest/api/3/myself` çağrısı ile token'ı doğrular.
2. `GET https://{jiraDomain}/rest/agile/1.0/board` ile erişilebilir board'ları listeler.
3. Geçici (board'sız) bağlantı kaydı oluşturur, `status=PENDING_BOARD`.

**Response 200:**
```json
{
  "connectionId": 1,
  "status": "PENDING_BOARD",
  "boards": [
    { "id": 42, "name": "Payment Board", "type": "scrum", "projectKey": "PAY" },
    { "id": 51, "name": "Search Board", "type": "scrum", "projectKey": "SRC" },
    { "id": 77, "name": "Platform Kanban", "type": "kanban", "projectKey": "PLT" }
  ]
}
```

**Hatalar:**
- `401` — Atlassian `/myself` 401 döndü
- `403` — çağıran lider değil
- `502` — Atlassian ulaşılamadı

---

### 8.2 POST /api/jira/connect/board
**Adım 2:** Bağlantıya kullanılacak board'u seç.

**Request:**
```json
{
  "connectionId": 1,
  "boardId": 42
}
```

**İşleyiş:**
- `boardId`, Adım 1'de dönen board listesinde mevcut olmalı; aksi halde 400.
- `projectKey` board kaydından otomatik doldurulur.
- Bağlantı `status=CONNECTED` durumuna geçer.

**Response 200:**
```json
{
  "id": 1,
  "teamId": 1,
  "email": "lead@company.com",
  "jiraDomain": "mycompany.atlassian.net",
  "projectKey": "PAY",
  "boardId": 42,
  "boardName": "Payment Board",
  "status": "CONNECTED"
}
```

**Hatalar:**
- `400` — `boardId` listede yok
- `403` — çağıran lider değil
- `404` — `connectionId` bulunamadı

---

### 8.3 GET /api/jira/boards?teamId={id}
Kayıtlı bağlantıdaki güncel board listesini Jira'dan canlı çekerek döndürür (board değiştirme akışı için).

**Response 200:**
```json
{
  "currentBoardId": 42,
  "boards": [
    { "id": 42, "name": "Payment Board", "type": "scrum", "projectKey": "PAY" },
    { "id": 51, "name": "Search Board", "type": "scrum", "projectKey": "SRC" }
  ]
}
```

**Hatalar:**
- `404` — ekibin Jira bağlantısı yok

---

### 8.4 POST /api/jira/connections (Geriye Dönük — Tek Adımlı)
Bağlantıyı tek adımda kur (board id önceden biliniyorsa). Yalnızca `LEADER`.

**Request:**
```json
{
  "teamId": 1,
  "email": "lead@company.com",
  "apiToken": "ATATT3xFfGF0...",
  "jiraDomain": "mycompany.atlassian.net",
  "projectKey": "PAY",
  "boardId": 42
}
```

**Response 201:**
```json
{
  "id": 1,
  "teamId": 1,
  "email": "lead@company.com",
  "jiraDomain": "mycompany.atlassian.net",
  "projectKey": "PAY",
  "boardId": 42,
  "status": "CONNECTED"
}
```

**Hatalar:**
- `401` — Jira `/myself` 401 döndü
- `403` — çağıran lider değil

---

### 8.5 GET /api/jira/connections/active?teamId={id}
Ekibin aktif Jira bağlantısı (token döndürmez).

**Response 200:**
```json
{
  "id": 1,
  "teamId": 1,
  "email": "lead@company.com",
  "jiraDomain": "mycompany.atlassian.net",
  "projectKey": "PAY",
  "boardId": 42,
  "boardName": "Payment Board",
  "status": "CONNECTED"
}
```

`status` ∈ {`CONNECTED`, `PENDING_BOARD`, `DISCONNECTED`, `MOCK`}.

**Hatalar:** `404` — bağlantı yok

---

### 8.6 DELETE /api/jira/connections/{connectionId}
Bağlantıyı kaldır (yalnızca `LEADER`).

**Response 204**

---

### 8.7 GET /api/jira/sprint-context?retroId={id}
Aktif sprint özeti.

**Response 200:**
```json
{
  "sprintName": "Sprint 24",
  "plannedStories": 12,
  "doneStories": 8,
  "openBugs": 2,
  "velocityPct": 70,
  "topBugService": "payment-service",
  "mock": false
}
```

`mock: true` döndüğünde frontend "Mock Mode" rozeti gösterir.

---

### 8.8 POST /api/jira/issue
Tekil aksiyondan Jira ticket yarat.

**Request:**
```json
{ "actionId": 50 }
```

**Response 201:**
```json
{ "actionId": 50, "jiraKey": "PAY-1234", "jiraUrl": "https://mycompany.atlassian.net/browse/PAY-1234" }
```

**Hatalar:**
- `409` — aksiyon zaten Jira'ya yazılmış (`jiraKey` dolu)

---

### 8.9 POST /api/jira/bulk-create
Retroda onaylanan tüm `OPEN` aksiyonları paralel olarak Jira'ya yaz. **Yalnızca board sahibi.** Faz `DISCUSSION` olmalıdır.

**Request:**
```json
{ "retroId": 10 }
```

**Response 200:**
```json
{
  "results": [
    { "actionId": 50, "jiraKey": "PAY-1234", "status": "SUCCESS" },
    { "actionId": 51, "jiraKey": null, "status": "FAILED", "error": "Jira 401" }
  ],
  "successCount": 1,
  "failedCount": 1,
  "mock": false
}
```

Her ticket'a `retro-action` + `retro-sprint-N` label otomatik eklenir. Aksiyonda `assigneeUserId` varsa Jira ticket'ına da set edilir.

---

## 9. Hata Şeması

Tüm hata response'ları aynı şemada döner:

```json
{
  "timestamp": "2026-05-14T10:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "email zorunludur",
  "path": "/api/auth/register"
}
```

## 10. HTTP Durum Kodları (özet)

| Kod | Anlam |
|-----|-------|
| 200 | OK |
| 201 | Created |
| 204 | No Content |
| 302 | Redirect (Atlassian OAuth) |
| 400 | Bad Request (validation, oy kotası, state mismatch) |
| 401 | Unauthorized |
| 403 | Forbidden (rol/sahiplik) |
| 404 | Not Found |
| 409 | Conflict (duplicate) |
| 410 | Gone (kapatılmış retroya guest katılım girişimi) |
| 423 | Locked (yanlış fazda yazma/oylama — `PHASE_LOCKED`) |
| 502 | Bad Gateway (Atlassian token endpoint) |
| 504 | Gateway Timeout (AI > 30s) |

---

## 11. Guest Access (Misafir Oturumu — Özet)

Login'siz katılım için kullanılan yardımcı bölüm. Detaylar her endpoint'in kendi başlığında.

**Akış:**
1. Board sahibi yeni retro açar → backend `guestJoinToken` (UUID v4) üretir, response'da `guestJoinUrl` döner (örn. `http://localhost:3000/join/<token>`).
2. Board sahibi linki paylaşır.
3. Misafir tarayıcısında `/join/<token>` route'una gelir → frontend `GET /api/retros/{id}/join/lookup?token=...` çağırarak token'ı doğrular ve retro özetini alır.
4. Misafire **"Görünen adınızı girin"** formu gösterilir (sadece `displayName` alanı, 2–40 karakter).
5. Form submit edilince `POST /api/retros/{id}/join` body `{ token, displayName }` çağrılır → backend `guestSessionId` (UUID v4) üretir, in-memory tabloya yazar.
6. Frontend `guestSessionId`'yi `sessionStorage`'da saklar; sonraki tüm yazma çağrılarında `X-Guest-Session: <uuid>` header'ı ekler.
7. Retro `CLOSED` duruma geçince tüm `guestSessionId` kayıtları geçersiz olur (sonraki yazma çağrıları 410).

**Guest oturumu kabul eden endpoint'ler:**

| Endpoint | Method | Notlar |
|----------|--------|--------|
| `/api/retros/{id}/join/lookup` | GET | Auth gerekmez, token query param |
| `/api/retros/{id}/join` | POST | Auth gerekmez, body içinde `displayName` |
| `/api/retros/{id}` | GET | Misafir okuma yapabilir |
| `/api/retros/{id}/cards` | GET, POST | Guest header ile kart oluşturma (faz `WRITING`) |
| `/api/cards/{cardId}` | PATCH, DELETE | Yalnızca kartın oluşturulduğu guest session |
| `/api/cards/{cardId}/vote` | POST, DELETE | Guest oy kotası: retro başına 3 (faz `VOTING`) |
| `/api/retros/{id}/participation` | GET | Guest da listelenir, `isGuest:true` |
| `/api/ai/silent-prompt` | GET | `userId` yerine `guestSessionId` query param |

**Guest oturumunda kesinlikle yapılamayanlar (403):**
- Retro fazını ilerletme (`POST /api/retros/{id}/phase`) — sadece board sahibi
- Retro alanlarını güncelleme (`PATCH /api/retros/{id}`) — sadece board sahibi
- Aksiyon onaylama (`POST /api/actions/bulk`) — sadece board sahibi
- AI Analiz tetikleme (`POST /api/ai/analyze`) — sadece board sahibi
- Olgunluk skoru hesaplama (`POST /api/ai/maturity`) — sadece board sahibi
- Jira bağlantısı (`POST /api/jira/connect`, `connect/board`, `connections`) — sadece `LEADER`
- Jira bulk-create / sprint-context / issue — sadece board sahibi (Jira'lı retroda)
- Ekip yönetimi (`/api/teams/...`)
- Yeni retro açma (`POST /api/retros`)

**Yetki Matrisi (özet):**

| İşlem | Misafir | Üye | Board Sahibi | LEADER |
|-------|---------|------|--------------|---------|
| Kart oku | ✓ | ✓ | ✓ | ✓ |
| Kart yaz (faz=WRITING) | ✓ | ✓ | ✓ | ✓ |
| Kendi kartını düzenle/sil | ✓ | ✓ | ✓ | ✓ |
| Oy ver (faz=VOTING) | ✓ | ✓ | ✓ | ✓ |
| Faz geçişi | ✗ | ✗ | ✓ | ✗ (lider olmak yetmez) |
| Anonim mod toggle | ✗ | ✗ | ✓ | ✗ |
| Retroyu kapat | ✗ | ✗ | ✓ | ✗ |
| AI Analiz tetikle | ✗ | ✗ | ✓ | ✗ |
| Aksiyon onayla (bulk) | ✗ | ✗ | ✓ | ✗ |
| Olgunluk skoru hesapla | ✗ | ✗ | ✓ | ✗ |
| Jira bulk-create | ✗ | ✗ | ✓ | ✗ |
| Jira bağlantısı kur | ✗ | ✗ | ✗ | ✓ |
| Üye ekle/çıkar | ✗ | ✗ | ✗ | ✓ |

**Header tanımı:**
- `X-Guest-Session: <uuid>` — yazma çağrılarında zorunlu (alternatif olarak `?guestSessionId=` query param).
- Auth Bearer token ile aynı çağrıda gönderilirse Bearer token önceliklidir.

**Hata kodları:**
- `403` — yetki yetersiz (örn. misafir faz geçişi yapmaya çalıştı)
- `410` — retro `CLOSED` olduğu için guest session geçersiz
- `423` — yanlış fazda yazma/oylama denemesi
