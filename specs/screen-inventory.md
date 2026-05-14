# RetroAI — Screen Inventory

Bu dosya tüm frontend ekranlarını, route'larını, içeriklerini ve navigasyonunu listeler.

---

## Global Layout

Tüm authenticated sayfalar aynı kabuğu kullanır:

- **Top Bar (sticky):**
  - Sol: Logo "RetroAI"
  - Orta: Ekip seçici dropdown (kullanıcının üyesi olduğu ekipler)
  - Sağ: Kullanıcı adı + avatar + "Çıkış" linki
- **Sub Bar (retro açıkken):**
  - Aktif retro adı + sprint
  - **PhaseProgressBar — 4 adım: Yazma · Gruplama · Oylama · Final Değerlendirme (aktif olan vurgulu, geçilen ✓)**
  - **Faz kontrol butonları** (sadece board sahibi için): "← Geri" + "İleri →"
  - Kalan oy: "Kalan oyunuz: 2/3" (yalnızca VOTING fazında görünür)
  - Katılım göstergesi: her üye için renkli nokta + adı
  - "Mock Mode" rozeti (Jira fallback aktifse)
  - Misafir rozetı: "Misafir: <displayName>" (guest oturumlarda)
- **Main content** alanı: route bazlı içerik
- **Footer**: Sürüm + build tarihi

---

## Ekran Listesi (route + dosya)

| # | Ekran | Route | Wireframe Dosyası |
|---|-------|-------|-------------------|
| 01 | Giriş | `/login` | `01-login.html` |
| 02 | Kayıt | `/register` | `02-register.html` |
| 03 | Ekip Dashboard | `/teams/:teamId` | `03-team-dashboard.html` |
| 04 | Ekip Üye Yönetimi | `/teams/:teamId/members` | `04-team-members.html` |
| 05 | Yeni Retro Aç | `/teams/:teamId/retros/new` | `05-new-retro.html` |
| 06 | Retro Tahtası (4 fazlı tek route) | `/retros/:retroId` | `06-retro-board.html` (faz görünümleri için ek HTML'ler aşağıda) |
| 06a | Retro Tahtası — Faz 1 WRITING | `/retros/:retroId` (currentPhase=WRITING) | `06a-phase-writing.html` |
| 06b | Retro Tahtası — Faz 2 GROUPING | `/retros/:retroId` (currentPhase=GROUPING) | `06b-phase-grouping.html` |
| 06c | Retro Tahtası — Faz 3 VOTING | `/retros/:retroId` (currentPhase=VOTING) | `06c-phase-voting.html` |
| 06d | Retro Tahtası — Faz 4 DISCUSSION | `/retros/:retroId` (currentPhase=DISCUSSION) | `06d-phase-discussion.html` |
| 09 | Aksiyonlar (Risk Radarı dahil) | `/teams/:teamId/actions` | `09-actions.html` |
| 10 | Jira Bağlantı Kurulumu | `/teams/:teamId/jira` | `10-jira-setup.html` |
| 11 | Olgunluk Skoru | `/teams/:teamId/maturity` | `11-maturity.html` |
| 12 | Açılış Brifingi (modal) | `/retros/:retroId` üstüne overlay | `12-briefing-modal.html` |
| 13 | Misafir Katılım — DisplayName Formu | `/join/:guestJoinToken` | `13-guest-join.html` |
| 14 | Jira Board Seçimi (Adım 2) | `/teams/:teamId/jira/board` | `14-jira-board-pick.html` |

**Not:** 07 ve 08 numaralı ekranlar **kaldırıldı** — AI Analiz ve Aksiyon Onayı artık ayrı route değil, retro tahtasının fazlarıdır.

---

## Ekran Detayları

### 01 — Giriş (`/login`)
**Ne gösterir:** Email + parola formu, "Kayıt ol" linki.

**Bileşenler:**
- LoginForm (email input, password input, submit button)
- AuthLayout (centered card)
- Link: "Hesabın yok mu? Kayıt ol →"

**API kullanır:** `POST /api/auth/login`

**Navigasyon:**
- → Başarılı login: `/teams/:teamId` (kullanıcının ilk ekibi yoksa "Ekip Oluştur" modal)
- → "Kayıt ol" linki: `/register`

---

### 02 — Kayıt (`/register`)
**Ne gösterir:** Email + ad-soyad + parola + parola tekrar formu.

**Bileşenler:**
- RegisterForm
- Validation messages
- Link: "Zaten hesabın var mı? Giriş yap →"

**API:** `POST /api/auth/register` → ardından otomatik `POST /api/auth/login`

**Navigasyon:**
- → Başarılı: `/teams/new` (henüz ekip yok)
- → "Giriş yap": `/login`

---

### 03 — Ekip Dashboard (`/teams/:teamId`)
**Ne gösterir:**
- Ekip özeti (üye sayısı, son retro tarihi, açık aksiyon sayısı)
- "Yeni Retro Aç" CTA butonu (büyük)
- Geçmiş retro listesi (tablo: sprint adı, tarih, durum, aksiyon sayısı)
- Son olgunluk skoru kartı (skor + seviye)
- Risk altındaki aksiyonlar özeti (sayı + "Aksiyonlara git" linki)

**Bileşenler:**
- TeamSummaryCard
- StartRetroCta
- RetroHistoryTable
- MaturityWidget
- ActiveActionsWidget

**API:** `GET /api/teams/:teamId`, `GET /api/retros?teamId=`, `GET /api/actions?teamId=&status=OPEN`, son maturity score

**Navigasyon:**
- → "Yeni Retro Aç": `/teams/:teamId/retros/new`
- → Tablo satırı tık: `/retros/:retroId`
- → Maturity widget: `/teams/:teamId/maturity`
- → Aksiyonlar widget: `/teams/:teamId/actions`
- → "Üyeler" sekmesi: `/teams/:teamId/members`
- → "Jira Bağlantısı" sekmesi (sadece LEADER): `/teams/:teamId/jira`

---

### 04 — Ekip Üye Yönetimi (`/teams/:teamId/members`)
**Ne gösterir:**
- Üye listesi (ad, email, rol, katılma tarihi)
- LEADER ise: "Üye Ekle" formu (email input) + her üye yanında "Çıkar" butonu

**Bileşenler:**
- MemberTable
- AddMemberForm (sadece LEADER)
- RemoveMemberButton (sadece LEADER)

**API:** `GET /api/teams/:teamId`, `POST /api/teams/:teamId/members`, `DELETE /api/teams/:teamId/members/:userId`

**Navigasyon:**
- ← `/teams/:teamId`

---

### 05 — Yeni Retro Aç (`/teams/:teamId/retros/new`)
**Ne gösterir:**
- Form: sprint adı, retro adı, anonim mod toggle (default açık)
- "Retroyu Başlat" butonu

**Bileşenler:**
- NewRetroForm
- AnonymousModeToggle

**API:** `POST /api/retros`

**Navigasyon:**
- → Başarılı: `/retros/:retroId` (Açılış Brifingi modal'ı ile)

---

### 06 — Retro Tahtası (`/retros/:retroId`)

**Tek route, 4 faz, faza göre değişen UI.** Retro Tahtası tek bir sayfadır ama `currentPhase` değerine göre farklı bileşenler render edilir. Üstte 4 adımlı PhaseProgressBar her zaman görünür.

#### Sabit Üst Çubuk (Tüm Fazlarda Görünür)

- **PhaseProgressBar** — 4 adım: "1. Yazma → 2. Gruplama → 3. Oylama → 4. Final Değerlendirme". Aktif faz vurgulanır, geçilen fazlar yeşil onay, gelecek fazlar gri.
- **Board sahibi için faz kontrol butonu:** "← Geri" + "İleri: <sonraki faz adı> →". Bu butonlar `POST /api/retros/:retroId/phase` çağırır.
- **Üye/misafir için faz kontrol butonu:** gizli. Sadece "Mevcut faz: ..." metni okunur.
- **Top bar:** Carry-over banner + Sprint Bağlamı Bandı (varsa) + Katılım göstergesi + Mock Mode rozeti (varsa)

#### Faz 1 — WRITING (Yazma)

**Ne gösterir:**
- 4 kolon: `GOOD` / `IMPROVE` / `DISCUSS` / `NEXT_STEPS` — her birinde kart listesi
- Her kolonun altında **"+ Kart Ekle" inline textarea** (aktif)
- Her kart üzerinde: içerik, kart yazarı (anonim mod açıksa "Anonim üye"), düzenle/sil ikonları (sadece sahibi için)
- Oy butonları **görünmez veya disabled** (henüz oylama açılmadı)
- Sessiz Katılımcı modal'ı sadece bu fazda tetiklenir
- AI Analiz butonu **görünmez** (bir sonraki fazda gelecek)

**Bileşenler:** PhaseProgressBar, CarryOverBanner, SprintContextBanner, BoardColumn × 4, RetroCard (read+edit mode), AddCardInlineForm, SilentPromptModal, ParticipationDots

**API çağrıları:**
- `GET /api/retros/:retroId` (3sn polling, faz değişimini yakalamak için)
- `GET /api/retros/:retroId/cards` (3sn polling)
- `GET /api/retros/:retroId/participation` (5sn polling)
- `GET /api/jira/sprint-context?retroId=` (sayfa açılışta tek sefer, Jira varsa)
- `GET /api/ai/silent-prompt?retroId=` (1dk polling)
- `POST /api/retros/:retroId/cards`
- `PATCH /api/cards/:cardId`, `DELETE /api/cards/:cardId`

**Board sahibi → "Gruplamaya Geç →" → faz `GROUPING`'e geçer.**

#### Faz 2 — GROUPING (Gruplama)

**Ne gösterir:**
- 4 kolon hâlâ görünür ama "+ Kart Ekle" inline form **gizlenir** (yeni kart eklenemez)
- Kartlar **drag-drop edilebilir** (kolonlar arası taşıma, sadece board sahibi için)
- Sağ panel: **"AI Analiz Et" CTA butonu** (board sahibi için aktif, üyeler için gizli)
- AI analizi çalıştırıldıktan sonra ekran ikiye bölünür:
  - Sol: kart tahtası (kartlarda tema etiketi rozet olarak gösterilir, örn. "CI/CD yavaşlığı")
  - Sağ: **ThemeClusterPanel** — tema kümeleri, moral skoru, aciliyet etiketi
- AI Analiz çıktısı **otomatik olarak NEXT_STEPS kolonuna kart düşürür** (her SMART aksiyon önerisi için bir kart, `source=AI_NEXT_STEP`, 🤖 ikonu ile gösterilir)
- Jira-kaynaklı içgörüler aynı panelde "Jira-kaynaklı" etiketiyle listelenir; "+ Kart Olarak Ekle" butonu var

**Bileşenler:** PhaseProgressBar, BoardColumn × 4 (drag-drop mode), RetroCard (read+drag, edit gizli), AnalyzeWithAiButton (board sahibi), ThemeClusterPanel, JiraInsightCard, LoadingOverlay (AI çalışırken)

**API çağrıları:**
- `GET /api/retros/:retroId`, `GET .../cards`, `GET .../participation` (polling devam)
- `POST /api/ai/analyze` (board sahibi tıklayınca)
- `POST /api/ai/jira-history` (varsa paralel)
- `PATCH /api/cards/:cardId` (kart taşıma — sadece kolon değişikliği)

**Board sahibi → "Oylamaya Geç →" → faz `VOTING`'e geçer.**

#### Faz 3 — VOTING (Oylama)

**Ne gösterir:**
- 4 kolon hâlâ görünür, kart ekleme/düzenleme/taşıma **tümü kapalı**
- Her kart üzerinde **belirgin oy butonu** + toplam oy sayısı (büyük rozet)
- Üst çubukta **"Kalan oyunuz: N/3"** sayacı her zaman önde
- Tema kümeleri paneli sağ tarafta kompakt görüntüde kalır (referans için)
- Oy verdikten sonra oy geri çekme butonu görünür
- 4 oy verme denemesi → toast: "Oy hakkın bitti"

**Bileşenler:** PhaseProgressBar, BoardColumn × 4 (read-only), RetroCard (vote mode), VoteButton, RemainingVotesIndicator, ThemeClusterSidebar

**API çağrıları:**
- `GET /api/retros/:retroId`, `GET .../cards` (oy sayıları canlı)
- `POST /api/cards/:cardId/vote`, `DELETE /api/cards/:cardId/vote`

**Board sahibi → "Final Değerlendirmeye Geç →" → faz `DISCUSSION`'a geçer.**

#### Faz 4 — DISCUSSION (Final Değerlendirme & Çıktılar)

**Ne gösterir:**
- Üstte **en çok oy alan ilk N kart** vurgulu listede (oyla sıralanmış, tartışılacaklar)
- 4 kolon hâlâ erişilebilir ama daraltılmış görünümde
- **NEXT_STEPS kolonu** vurgulu (kalın çerçeve, "Bu retronun çıktıları" başlığı altında)
- Sağ panel veya alt bölüm: **"Aksiyon Onaylama Paneli"** — F-12 ile aynı yapı
  - AI Analiz'in ürettiği SMART aksiyonlar + NEXT_STEPS kartlarından oluşturulanlar tek listede
  - Çekboks, başlık (edit), assignee (dropdown), deadline (date picker)
  - "Sadece Kaydet" + "Kaydet & Jira'ya Gönder" butonları (board sahibi için)
- Sağ alt: **"Olgunluk Skoru Hesapla"** butonu (board sahibi için)
- Olgunluk skoru hesaplandıktan sonra modal/inline kart: skor + seviye + 3 öneri

**Bileşenler:** PhaseProgressBar, TopVotedCardsList, BoardColumn × 4 (compact, read-only), NextStepsHighlight, ActionApprovalPanel (inline), MaturityCalcButton, MaturityResultCard

**API çağrıları:**
- `GET /api/retros/:retroId`, `GET .../cards`
- `POST /api/actions/bulk` (board sahibi)
- `POST /api/actions/from-card` (NEXT_STEPS kartını aksiyona çevirme)
- `POST /api/jira/bulk-create` (board sahibi)
- `POST /api/ai/maturity` (board sahibi)

**Board sahibi → "Retroyu Kapat" → `PATCH /api/retros/:retroId` body `{status: CLOSED}` → faz `CLOSED`. Tüm guest session'lar geçersiz.**

#### Faz 5 — CLOSED (Salt Okunur Arşiv)

**Ne gösterir:**
- Tüm kartlar, oylar, aksiyonlar salt okunur
- Üstte rozet: "Bu retro kapatıldı (DD.MM.YYYY)"
- "Aksiyonları Gör" + "Olgunluk Skorunu Gör" linkleri
- Yazma denemesi yapılırsa toast: "Bu retro kapatılmış"

**API:** sadece GET'ler

---

#### Faz Geçişi UI Davranışı (Önemli!)

- Frontend her 3 saniyede `GET /api/retros/:retroId` çağrısıyla `currentPhase`'i polling yapar.
- Faz değiştiğinde tüm sayfa yeniden render edilir — kart ekleme alanı, oy butonları, AI butonu vb. dinamik olarak gizlenir/açılır.
- Üyeye/misafire **toast bildirimi** gösterilir: "Board sahibi faz değiştirdi: VOTING başladı."
- Board sahibi olmayan biri faz kontrol butonunu görmez ve yanlış fazda yazma denerse 423 alır → toast: "Bu faz kapalı, oylama açıldı."

---

#### Misafir (Guest) Akışı — Tüm Fazlarda Geçerli

- Guest header gönderir, sınırlı yetkilerle aynı tahtayı görür.
- WRITING'de kart ekleyebilir, VOTING'de oy verebilir.
- GROUPING ve DISCUSSION'da pasif izleyici (kart taşıma, AI tetikleme, aksiyon onayı yok).
- Faz kontrol butonu **misafirde hiçbir zaman görünmez**.

---

### 07 — (Kaldırıldı) AI Analiz artık ayrı route değil

> **NOT:** Önceki tasarımdaki `/retros/:retroId/analyze` route'u kaldırılmıştır. AI Analiz **GROUPING fazının inline parçasıdır** — board sahibi "AI Analiz Et" butonuna basar, sonuçlar Retro Tahtası'nın sağ panelinde gösterilir. Ayrı bir sayfaya gidiş yoktur. Bkz. 06 → Faz 2.

---

### 08 — (Kaldırıldı) Aksiyon Onay Ekranı artık ayrı route değil

> **NOT:** Önceki tasarımdaki `/retros/:retroId/actions/approve` route'u kaldırılmıştır. Aksiyon onaylama **DISCUSSION fazının inline parçasıdır** — Retro Tahtası'nın altında veya sağ panelinde ActionApprovalPanel olarak görünür. Bkz. 06 → Faz 4.

**API:**
- `POST /api/actions/bulk`
- `POST /api/jira/bulk-create`

**Navigasyon:**
- → Başarılı: `/teams/:teamId/actions`
- ← `/retros/:retroId/analyze`

---

### 09 — Aksiyonlar (`/teams/:teamId/actions`)
**Ne gösterir:**
- En üst: **Risk Radarı Paneli** (kırmızı arka plan, riskScore ≥ 4 aksiyonlar)
  - Her aksiyon yanında: AI gerekçe, rewrite suggestion, "Rewrite'ı Uygula" buton
- Aşağıda: Aksiyon tablosu
  - Kolonlar: durum, başlık, atanan, deadline, risk skoru, Jira key + link, sprint kaynağı
  - Durum filtresi: OPEN / IN_PROGRESS / DONE / AT_RISK / hepsi
  - Her satırda inline status dropdown
  - "carriedFromSprint" değeri varsa "Sprint N'den devreden" rozeti
  - jiraKey yoksa "+ Jira Ticket Yarat" buton
- Sağ üst: "+ Manuel Aksiyon Ekle" buton

**Bileşenler:**
- RiskRadarPanel
- ActionFilterBar
- ActionTable
- ManualActionModal
- CarryOverBadge
- JiraLinkOrCreateButton

**API:**
- `GET /api/actions?teamId=&status=`
- `POST /api/ai/risk-score`
- `PATCH /api/actions/:actionId`
- `POST /api/jira/issue`
- `POST /api/actions/bulk` (manuel ekleme tek elemanlı bulk)

**Navigasyon:**
- ← `/teams/:teamId`

---

### 10 — Jira Bağlantı Kurulumu (`/teams/:teamId/jira`)
**Ne gösterir (yalnızca LEADER, MEMBER 403):**
- Form: Atlassian email, API token, Jira domain, project key, board id
- "Bağlantıyı Test Et & Kaydet" buton
- Eğer bağlı: bağlı email + domain + "Bağlantıyı Kaldır" buton
- "Mock Mode" rozeti (gerekirse)
- Yardım: "API token nasıl alınır?" linki Atlassian dokümanına

**Bileşenler:**
- JiraConnectionForm
- ActiveConnectionCard
- DisconnectButton
- HelpLink

**API:**
- `POST /api/jira/connections`
- `GET /api/jira/connections/active?teamId=`
- `DELETE /api/jira/connections/:connectionId`

**Navigasyon:**
- ← `/teams/:teamId`

---

### 11 — Olgunluk Skoru (`/teams/:teamId/maturity`)
**Ne gösterir:**
- Büyük skor göstergesi (0-100, dairesel)
- Seviye etiketi: FORMING / NORMING / PERFORMING / MASTERY
- Üç bileşen barı: Aksiyon tamamlama, SMART'lık, tekrar eden sorun yokluğu
- 3 maddelik iyileştirme planı (AI üretimi)
- Geçmiş retrolardaki skorlar listesi (tablo: retro adı, skor, seviye, tarih)

**Bileşenler:**
- MaturityGauge
- LevelBadge
- ComponentBar × 3
- TipsList
- HistoricalScoresTable

**API:** `POST /api/ai/maturity` (en son skoru hesapla & persist), `GET` ile geçmiş skorlar (`GET /api/teams/:teamId/maturity/history` — implicit, action listesi gibi son skorları döner)

**Navigasyon:**
- ← `/teams/:teamId`

---

### 12 — Açılış Brifingi Modal (overlay)
**Ne gösterir:**
- Retro Tahtası açılınca otomatik açılır
- "Önceki retro özeti": Geçen retroda 4 aksiyon vardı. 2 tamam, 1 devam, 1 RİSK ALTINDA
- Recurring themes uyarısı: "Bu 3. sprint'te 'CI/CD yavaşlığı' konusu çıkıyor — kök neden analizi yapalım mı?"
- "Anladım, devam et" buton (modal'ı kapatır)

**Bileşenler:**
- BriefingModal
- PrevRetroSummaryBlock
- RecurringThemeAlert

**API:** `GET /api/ai/briefing?retroId=`

**Navigasyon:**
- → Kapat: retro tahtası açık kalır (`/retros/:retroId`)

---

## Bileşen Katalogu

### Layout
- `AuthLayout` — login/register için ortalanmış kart
- `AppLayout` — top bar + sub bar + main + footer
- `Sidebar` (opsiyonel) — ekip seçici dropdown'ı host eder

### Form
- `TextField`, `PasswordField`, `Select`, `DatePicker`, `Toggle`, `Textarea`
- `SubmitButton`, `PrimaryButton`, `SecondaryButton`, `DangerButton`

### Retro — Faz / Step Kontrolü (YENİ)
- `PhaseProgressBar` — 4 adımlı yatay ilerleme çubuğu (WRITING / GROUPING / VOTING / DISCUSSION). Aktif faz vurgulu, geçilen fazlar ✓ ile işaretli
- `PhaseControlButtons` — sadece board sahibi görür: "← Geri" + "İleri: <sonraki faz> →". `POST /api/retros/:id/phase` çağırır
- `PhaseLockedToast` — yanlış fazda yazma denemesinde 423 yakalanır, kullanıcıya "Bu faz kapalı: ..." toast'ı gösterilir
- `PhaseChangeNotificationToast` — polling ile faz değişimi tespit edilince diğer kullanıcılara "Board sahibi faz değiştirdi: VOTING başladı" toast'ı

### Retro — Tahta Bileşenleri
- `BoardColumn` × 4 (GOOD / IMPROVE / DISCUSS / NEXT_STEPS)
- `RetroCard` — iki mode: (a) edit-mode (WRITING fazında, sahibi için), (b) read+vote mode (VOTING fazında), (c) read-only (GROUPING, DISCUSSION, CLOSED)
- `AddCardInlineForm` — sadece WRITING fazında render edilir
- `VoteButton` + `RemainingVotesIndicator` — sadece VOTING fazında render edilir
- `DragDropOverlay` — sadece GROUPING fazında, board sahibi için (kart kolonlar arası taşıma)
- `ParticipationDots`, `SilentPromptModal`
- `CarryOverBanner`, `SprintContextBanner`
- `TopVotedCardsList` — DISCUSSION fazında en oy alan kartları büyük gösterir
- `NextStepsHighlight` — DISCUSSION fazında NEXT_STEPS kolonu vurgulu çerçeve

### AI
- `AnalyzeWithAiButton` (board sahibi, GROUPING fazında)
- `ThemeClusterPanel`, `ThemeClusterCard`, `JiraInsightCard`
- `LoadingOverlay` — AI 30sn analiz süresince
- `MaturityGauge`, `MaturityResultCard`, `RiskBadge`, `RewriteCard`

### Action
- `ActionApprovalPanel` — DISCUSSION fazında inline, eski 08 ekranının içeriği
- `ActionTable`, `ActionRow`, `RiskRadarPanel`, `ManualActionModal`
- `JiraLinkOrCreateButton`, `CarryOverBadge`

### Guest
- `GuestJoinTokenLookup` — `/join/:token` route'unda token doğrulama
- `GuestDisplayNameForm` — displayName girişi (validation: 2-40 char, çakışma kontrolü)
- `GuestSessionBadge` — top bar'da "Misafir: Ahmet K." rozet

### Auth
- `AtlassianLoginButton` — `/login` ekranında "Atlassian ile Giriş Yap"
- `LocalLoginForm`, `LocalRegisterForm`

### Jira
- `JiraConnectionForm` — Adım 1 (email + token + domain)
- `JiraBoardPicker` — Adım 2 (board listesi, radio select)
- `ActiveConnectionCard` (boardName göstergesiyle), `MockModeBadge`

### Feedback
- `Toast` (success/error/info), `Spinner`, `ErrorBanner`, `EmptyState`

---

## Navigasyon Haritası (özet)

```
/login ──► /teams/:teamId ──┬─► /teams/:teamId/members
   │                        ├─► /teams/:teamId/jira (LEADER) ──► /teams/:teamId/jira/board
   │                        ├─► /teams/:teamId/actions
   │                        ├─► /teams/:teamId/maturity
   │                        └─► /teams/:teamId/retros/new
   │                                   │
   │                                   ▼
   │                          /retros/:retroId   (Brifing modal açılır)
   │                                   │
   │                       ┌───────────┼────────────┬────────────┐
   │                       ▼           ▼            ▼            ▼
   │                    Faz 1       Faz 2        Faz 3        Faz 4
   │                   WRITING ─► GROUPING ─►  VOTING  ─►  DISCUSSION ─► CLOSED
   │                       │ board sahibi "İleri →" ile geçer; üye/misafir pasif
   │                       ▼
   │                  /teams/:teamId/actions (DISCUSSION sonrası aksiyon yazılınca)
   │
   ├──► /api/auth/atlassian (Atlassian OAuth) ──► /api/auth/atlassian/callback ──► /teams
   │
   └──► /register ──► /login (otomatik) ──► /teams/:teamId

/join/:guestJoinToken (misafir) ──► displayName formu ──► /retros/:retroId (guest mode)
```

---

## Polling & Gerçek-Zamanlılık

| Endpoint | Polling Aralığı | Sayfa | Amaç |
|----------|-----------------|-------|------|
| `GET /api/retros/:retroId` | 3 sn | Retro Tahtası (tüm fazlar) | `currentPhase` değişimini yakala |
| `GET /api/retros/:retroId/cards` | 3 sn | Retro Tahtası (tüm fazlar) | Yeni kart / oy sayısı güncellemesi |
| `GET /api/retros/:retroId/participation` | 5 sn | Retro Tahtası (top bar) | Katılım göstergesi |
| `GET /api/ai/silent-prompt` | 1 dk | Retro Tahtası (yalnızca WRITING fazı) | Sessiz katılımcı dürtüsü |
