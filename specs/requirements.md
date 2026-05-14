# RetroAI — Requirements (User Stories)

Bu dosya pm-brief.md'deki tüm MVP özelliklerini user story formatında içerir.
Her özellik bir Feature ID'ye sahiptir ve kabul kriterleri ölçülebilir tanımlanmıştır.

---

## F-01 — Kullanıcı Kaydı ve Girişi
**User Story:** Bir ekip üyesi olarak email ve parola ile sisteme kayıt olmak ve giriş yapmak istiyorum, çünkü retroya katılabilmem için kimliğimin tanımlı olması gerekir.

**Kabul Kriterleri:**
- Kullanıcı email + ad-soyad + parola ile kayıt olabilir.
- Aynı email ile ikinci kez kayıt olunamaz; sistem 409 döner.
- Geçerli email + parola ile giriş yapıldığında JWT/Session token döner.
- `/api/auth/me` çağrısı oturum açmış kullanıcının id, email, ad-soyad bilgilerini döner.
- Parola minimum 6 karakter olmalıdır.

**Bağımlılıklar:** —

---

## F-02 — Ekip Oluşturma ve Yönetme
**User Story:** Bir ekip lideri olarak yeni bir ekip oluşturmak ve diğer üyeleri ekibe eklemek istiyorum, çünkü retroyu birlikte yapacağımız üyelerin tanımlı olması gerekir.

**Kabul Kriterleri:**
- Giriş yapmış kullanıcı yeni ekip oluşturabilir; ekip oluşturan otomatik olarak `LEADER` rolünü alır.
- Ekibe email ile üye eklenebilir (eklenen kullanıcı sistemde kayıtlıysa).
- Üye çıkarma yalnızca lider tarafından yapılabilir.
- Bir kullanıcı birden fazla ekibin üyesi olabilir.
- Üst bardaki ekip seçici dropdown ile aktif ekip değiştirilebilir.

**Bağımlılıklar:** F-01

---

## F-03 — Retro Oturumu Oluşturma
**User Story:** Bir ekip üyesi olarak yeni bir retro oturumu açmak istiyorum, çünkü sprint sonunda ekipçe değerlendirme yapacağız.

**Kabul Kriterleri:**
- Ekip + sprint adı + retro adı zorunlu alanlardır.
- Oluşturulan retro `ACTIVE` durumda başlar.
- Aynı ekipte aynı anda yalnızca bir aktif retro olabilir; ikincisi 409 döner.
- Retro kapatıldığında durumu `CLOSED` olur.
- Oluşturulan retro `GET /api/retros?teamId=` çağrısıyla listelenir.

**Bağımlılıklar:** F-02

---

## F-04 — Üç Sütunlu Kart Tahtası
**User Story:** Bir ekip üyesi olarak "İyi Giden / Geliştirilebilir / Tartışılacak" kolonlarına serbest metin kart eklemek istiyorum, çünkü gözlemlerimi paylaşmam gerekir.

**Kabul Kriterleri:**
- Üç sabit kolon vardır: `GOOD`, `IMPROVE`, `DISCUSS`.
- Her kart `content`, `column`, `retroId`, `authorId` alanlarına sahiptir.
- Kart sahibi kendi kartını düzenleyebilir ve silebilir.
- Diğer üyeler başkasının kartını düzenleyemez (403).
- Kartlar oluşturulma sırasına göre listelenir.

**Bağımlılıklar:** F-03

---

## F-05 — Manuel Mod (Hybrid)
**User Story:** Bir ekip üyesi olarak Jira bağlantısı kurmadan da tüm temel özellikleri kullanmak istiyorum, çünkü her ekibin Jira'sı olmayabilir.

**Kabul Kriterleri:**
- Jira bağlantısı yoksa kart girişi, AI Analiz, Olgunluk Skoru tam çalışır.
- Jira-bağımlı UI elementleri (Sprint Bağlamı Bandı, "Jira'ya Gönder" butonu) Jira yoksa gizlenir.
- `GET /api/jira/connections/active?teamId=` 404 dönüyorsa frontend Jira UI'sını gizler.

**Bağımlılıklar:** F-04

---

## F-06 — Anonim Kart Modu
**User Story:** Bir ekip üyesi olarak kartlarımı varsayılan olarak anonim yazmak istiyorum, çünkü açık geri bildirim verebilmek için kimliğimin gizlenmesi gerekir.

**Kabul Kriterleri:**
- Yeni retro açılırken `anonymousMode` varsayılan olarak `true`.
- Anonim modda `GET /api/retros/{id}/cards` endpoint'i `authorName` döndürmez, yerine "Anonim üye" döner.
- Lider toggle ile modu kapatabilir.
- Anonim mod kapalıyken kart üzerinde yazarın adı gösterilir.

**Bağımlılıklar:** F-04

---

## F-07 — Eşit Oy Kotası
**User Story:** Bir ekip üyesi olarak retro başına 3 oy hakkım olsun istiyorum, çünkü en önemli konulara odaklanmak için eşit söz hakkı gerekir.

**Kabul Kriterleri:**
- Her kullanıcının her retroda 3 oy hakkı vardır.
- 4. oy verme girişimi 400 döner.
- Aynı karta birden fazla oy verilemez (unique constraint).
- Kullanıcı verdiği oyu geri çekebilir; oy hakkı 1 artar.
- Top bar'da kalan oy sayısı gösterilir.

**Bağımlılıklar:** F-04

---

## F-08 — Katılım Göstergesi
**User Story:** Bir ekip lideri olarak hangi üyenin kaç kart yazdığını görmek istiyorum, çünkü katılım dengesini gözlemleyebilmem gerekir.

**Kabul Kriterleri:**
- Top bar'da her üye için renkli nokta gösterilir: yeşil (2+ kart), sarı (1 kart), gri (0 kart).
- Frontend 5 saniyede bir `GET /api/retros/{id}/participation` çağırır (polling).
- Endpoint her üye için `{userId, name, cardCount, status}` döner.
- Anonim modda dahi katılım göstergesi çalışır (sayım yapar, isim göstermez).

**Bağımlılıklar:** F-04

---

## F-09 — Sessiz Katılımcı Dürtüsü (AI)
**User Story:** Bir ekip üyesi olarak retroya 10 dakikadır kart yazamadıysam bana özel bir başlangıç sorusu görmek istiyorum, çünkü retroya katılımım kolaylaşır.

**Kabul Kriterleri:**
- Retro başlangıcından 10 dakika sonra hâlâ 0 kart yazmış üyeye `/api/ai/silent-prompt?retroId=&userId=` bir başlangıç sorusu döner.
- Prompt yalnızca o üyeye gösterilir, başka üyeler görmez.
- Prompt en fazla 1 defa gösterilir; üye kart yazdıktan sonra tekrar tetiklenmez.

**Bağımlılıklar:** F-04

---

## F-10 — Ekip Lideri Rolü
**User Story:** Bir ekip lideri olarak Jira bağlantısını ben kurmak istiyorum, çünkü API token gibi hassas bilgilerin tek noktadan yönetilmesi gerekir.

**Kabul Kriterleri:**
- Yalnızca `LEADER` rolündeki üye `POST /api/jira/connections` çağrısını yapabilir.
- Diğer endpoint'lerde lider/üye farkı yoktur.
- Üye Jira bağlantı sayfasına gitmeye çalışırsa 403 döner.

**Bağımlılıklar:** F-02

---

## F-11 — AI Tema Kümeleme + SMART Aksiyon Üretimi
**User Story:** Bir ekip üyesi olarak "AI Analiz Et" butonuna bastığımda kartlardan tema kümeleri ve SMART aksiyon önerileri görmek istiyorum, çünkü manuel olarak bunu yapmak vakit alır.

**Kabul Kriterleri:**
- `POST /api/ai/analyze` retro kartlarını alır, tema kümeleri + SMART aksiyon önerileri döner.
- Her kümede başlık, moral skoru (-5..+5), aciliyet etiketi (`LOW`/`MEDIUM`/`HIGH`) bulunur.
- Her aksiyon `title`, `description`, `suggestedAssignee`, `suggestedDeadline` alanlarına sahiptir.
- Analiz çağrısı maksimum 30 saniyede tamamlanır; aksi halde 504 döner.

**Bağımlılıklar:** F-04

---

## F-12 — Aksiyon Onay Ekranı
**User Story:** Bir ekip üyesi olarak AI'ın ürettiği aksiyon önerilerini onaylamadan önce inceleyip düzenlemek istiyorum, çünkü kendi bağlamımızı katmamız gerekir.

**Kabul Kriterleri:**
- Aksiyon onay ekranı çekboks listesi gösterir; her aksiyon varsayılan olarak işaretlidir.
- Kullanıcı `title` ve `assignee` alanlarını düzenleyebilir.
- "Onayla" butonu yalnızca işaretli olan aksiyonları `POST /api/actions/bulk` çağrısıyla kalıcılaştırır.
- Onaylanan her aksiyon `status=OPEN` ile başlar.

**Bağımlılıklar:** F-11

---

## F-13 — Aksiyonları Bulk Jira'ya Gönderme
**User Story:** Bir ekip lideri olarak onaylanan aksiyonların tamamını tek tıkla Jira'ya ticket olarak yazdırmak istiyorum, çünkü manuel tekil ticket açmak zaman kaybıdır.

**Kabul Kriterleri:**
- `POST /api/jira/bulk-create` retroId alır, tüm `OPEN` aksiyonları paralel olarak `POST /rest/api/3/issue` çağrısıyla Jira'ya yazar.
- Her ticket'a `retro-action` ve `retro-sprint-N` label'ları otomatik eklenir.
- Aksiyonda `assignee` varsa Jira ticket'ına da set edilir.
- Yazılan her aksiyonun `jiraKey` alanı güncellenir.
- Tek tek hatalar response içinde rapor edilir; bir ticket fail olsa bile diğerleri kaydedilir.

**Bağımlılıklar:** F-12, F-21

---

## F-14 — Tekil Manuel Ticket Yaratma
**User Story:** Bir ekip üyesi olarak retro sonrası eklenen manuel bir aksiyon için "+ Jira Ticket Yarat" butonu görmek istiyorum, çünkü her aksiyonun Jira'da takibi gerekir.

**Kabul Kriterleri:**
- Manuel eklenmiş bir aksiyon için `POST /api/jira/issue` tekil ticket yaratır.
- Ticket başarıyla oluşturulursa aksiyonun `jiraKey` alanı set edilir.
- Aksiyonda zaten `jiraKey` varsa 409 döner.

**Bağımlılıklar:** F-21

---

## F-15 — Aksiyon Listesinde Jira Bağlantısı
**User Story:** Bir ekip üyesi olarak aksiyon listesinde Jira ticket key'ini ve linkini görmek istiyorum, çünkü Jira'ya kolayca geçebilmem gerekir.

**Kabul Kriterleri:**
- Aksiyon listesinde `jiraKey` varsa yanında "Jira'da Aç →" linki gösterilir.
- Link `https://{jiraDomain}/browse/{jiraKey}` formatındadır.
- `jiraKey` yoksa yerine "+ Jira Ticket Yarat" butonu gösterilir.

**Bağımlılıklar:** F-13, F-14

---

## F-16 — Unutulma Risk Skoru (AI)
**User Story:** Bir ekip üyesi olarak her aksiyon için 1-5 arası unutulma risk skoru görmek istiyorum, çünkü hangi aksiyonların unutulma tehlikesi olduğunu önceden bilmem gerekir.

**Kabul Kriterleri:**
- `POST /api/ai/risk-score` aksiyon id'lerini alır, her biri için 1-5 risk skoru + gerekçe metni + rewrite önerisi döner.
- Sinyaller: sahip yokluğu, deadline yokluğu, muğlak ifade, son güncelleme gecikmesi, deadline yaklaşması.
- Skor aksiyonun `riskScore` alanına yazılır.
- Aksiyon listesinde 4-5 skorlu aksiyonlar kırmızı renkte gösterilir.

**Bağımlılıklar:** F-12

---

## F-17 — Risk Radarı Paneli
**User Story:** Bir ekip lideri olarak aksiyon sayfasının en üstünde kritik aksiyonları kırmızı bir panelde görmek istiyorum, çünkü acil aksiyonları gözden kaçıramam.

**Kabul Kriterleri:**
- Aksiyonlar ekranının üstünde `riskScore >= 4` olan aksiyonlar listelenir.
- Her aksiyonun yanında AI gerekçesi ve rewrite önerisi gösterilir.
- "Rewrite'ı Uygula" butonu aksiyonun `title` alanını günceller.
- Hiç kritik aksiyon yoksa panel "Risk altında aksiyon yok" yazar.

**Bağımlılıklar:** F-16

---

## F-18 — Otomatik Carry-Over
**User Story:** Bir ekip üyesi olarak yeni retro açtığımda önceki retrodaki kapanmamış aksiyonların otomatik taşınmasını istiyorum, çünkü her seferinde manuel kopyalamak hatalıdır.

**Kabul Kriterleri:**
- Yeni retro `POST /api/retros` ile oluşturulduğunda aynı ekibin önceki retrosundaki `status != DONE` aksiyonları otomatik kopyalanır.
- Kullanıcıya onay sorulmaz.
- Yeni retro açılışında bir banner: "🔄 N aksiyon önceki retrodan otomatik taşındı".
- Taşınan aksiyonların `carriedFromSprint` alanı doldurulur, UI'da "Sprint N'den devreden" etiketi gösterilir.

**Bağımlılıklar:** F-12

---

## F-19 — Tekrar Eden Tema Uyarısı (Pattern Detector)
**User Story:** Bir ekip lideri olarak aynı tema 2+ retroda tekrarladığında uyarı almak istiyorum, çünkü kök neden analizi yapmamız gereken kalıpları kaçırmamak gerekir.

**Kabul Kriterleri:**
- `GET /api/ai/briefing?retroId=` önceki retrolardaki tema kümelerini karşılaştırır.
- Aynı tema 2+ retroda çıktıysa `recurringThemes` listesinde döner.
- Briefing açılış banner'ında uyarı gösterilir: "Bu N. sprint'te '...' konusu çıkıyor."

**Bağımlılıklar:** F-11

---

## F-20 — Sprint Bağlamı Bandı (Jira'dan)
**User Story:** Bir ekip üyesi olarak retro açıldığında Jira'dan aktif sprint özeti görmek istiyorum, çünkü konuşacaklarımızı veri ile destekleyebilmemiz gerekir.

**Kabul Kriterleri:**
- Retro açılınca `GET /api/jira/sprint-context?retroId=` çağrılır.
- Response: `{plannedStories, doneStories, openBugs, velocityPct, topBugService}`.
- Retro tahtasının üstünde banner gösterilir.
- Jira yoksa banner gizlenir.

**Bağımlılıklar:** F-21

---

## F-21 — Jira Bağlantısı Kurma
**User Story:** Bir ekip lideri olarak ekibimin Jira hesabını email + API token ile bağlamak istiyorum, çünkü retro aksiyonlarının Jira'ya akması için bağlantı kurulması gerekir.

**Kabul Kriterleri:**
- `POST /api/jira/connections` email, API token, Jira domain (örn. `mycompany.atlassian.net`), projectKey, boardId alır.
- Backend `GET /rest/api/3/myself` ile token'ı doğrular; başarısızsa 401 döner.
- Token plaintext olarak DB'ye yazılmaz, session memory'de tutulur.
- Bağlantı bilgisi `GET /api/jira/connections/active?teamId=` ile sorgulanabilir (token döndürülmez, sadece email + domain).

**Bağımlılıklar:** F-10

---

## F-22 — Status & History AI Analizi
**User Story:** Bir ekip üyesi olarak AI'ın Jira ticket geçmişindeki kalıpları (sık status değişimi, sprint atlamaları, reopened bug'lar) analiz etmesini istiyorum, çünkü gizli kalmış sorunları görmemiz gerekir.

**Kabul Kriterleri:**
- `POST /api/ai/jira-history` retroId alır, aktif sprint ticket'larının changelog + yorumlarına bakar.
- Çıktı `JiraInsight` listesi: `{ticketKey, signalType, description, suggestedCardTitle}`.
- `signalType` ∈ {`STATUS_BOUNCING`, `LONG_CARRYOVER`, `REOPENED_BUG`, `LONG_IDLE`}.
- Insight'lar AI Analiz panelinde "Jira-kaynaklı" etiketiyle listelenir.

**Bağımlılıklar:** F-20

---

## F-23 — Jira-Kaynaklı Kart Önerileri
**User Story:** Bir ekip üyesi olarak AI'ın Jira analizinden çıkardığı insight'ları "Geliştirilebilir" kolonuna kart olarak ekleyebilmek istiyorum, çünkü manuel kart yazmak yerine AI'ın gözlemini kullanmak istiyorum.

**Kabul Kriterleri:**
- Her Jira insight'ın yanında "Kart Olarak Ekle" butonu vardır.
- Butona basıldığında `POST /api/retros/{id}/cards` çağrılır; `source=JIRA_AI` alanı set edilir.
- Kart "Geliştirilebilir" kolonunda 🤖 ikonuyla gösterilir.

**Bağımlılıklar:** F-22

---

## F-24 — Ekip Olgunluk Skoru
**User Story:** Bir ekip lideri olarak her retro sonrası ekibimin 0-100 arası olgunluk skorunu görmek istiyorum, çünkü ekibin zaman içindeki gelişimini ölçmem gerekir.

**Kabul Kriterleri:**
- `POST /api/ai/maturity` retroId alır, üç bileşene bakar: aksiyon tamamlama oranı, aksiyon SMART'lığı, tekrar eden sorun yokluğu.
- Çıktı: `{score: 0-100, level: 'FORMING'|'NORMING'|'PERFORMING'|'MASTERY', tips: [3 madde]}`.
- Skor `team_maturity_score` tablosunda persist edilir.
- Ekip ana sayfasında son skor gösterilir.

**Bağımlılıklar:** F-12

---

## F-25 — Olgunluk Seviyesi & Öneriler
**User Story:** Bir ekip lideri olarak olgunluk skoruyla birlikte ekibe özel 3 maddelik iyileştirme planı görmek istiyorum, çünkü skorun ne anlama geldiğini bilmek yetmez, ne yapılacağı da gerekir.

**Kabul Kriterleri:**
- `POST /api/ai/maturity` çıktısında `tips: string[3]` döner.
- Her tip "kısa eylem cümlesi" formatındadır.
- Frontend skor + seviye + 3 tip'i kartta gösterir.

**Bağımlılıklar:** F-24

---

## F-26 — Açılış Brifingi (Önceki Retro Özeti)
**User Story:** Bir ekip üyesi olarak yeni retro açtığımda önceki retronun aksiyon durumunu özetleyen bir brifing görmek istiyorum, çünkü neyin tamamlandığını / risk altında olduğunu hatırlamamız gerekir.

**Kabul Kriterleri:**
- `GET /api/ai/briefing?retroId=` çağrılır.
- Response: `{prevRetroSummary, doneCount, inProgressCount, atRiskCount, recurringThemes: []}`.
- Retro açılış ekranında modal olarak gösterilir.
- İlk retro ise `prevRetroSummary` null döner; UI uygun mesaj gösterir.

**Bağımlılıklar:** F-11, F-19

---

## F-32 — Retro Fazları (Phases / Steps)
**User Story:** Bir ekip üyesi olarak retro oturumunun yapılandırılmış fazlardan ilerlemesini istiyorum (yaz → grupla → oyla → final değerlendirme), çünkü herkes aynı anda farklı şey yaparsa retro etkinliği dağılır.

**Kabul Kriterleri:**
- `RetroSession` her zaman 4 fazdan birinde + kapanış olur:
  1. `WRITING` — Yazma fazı. Yalnızca bu fazda kart ekleme/güncelleme açıktır.
  2. `GROUPING` — Gruplama fazı. AI Analiz (`POST /api/ai/analyze`) bu fazda tetiklenir; tema kümeleri oluşur. Kart ekleme bu fazda **kapalıdır** (423 Locked döner).
  3. `VOTING` — Oylama fazı. Yalnızca bu fazda oy verme/geri çekme açıktır. Kart ekleme/silme kapalıdır.
  4. `DISCUSSION` — Tartışma & Final Değerlendirme. Aksiyon onay ekranı, Jira'ya bulk gönderim, olgunluk skoru ve "Devam Adımları" tartışması bu fazda yapılır. Kart ekleme/oylama kapalıdır.
  5. `CLOSED` — Kapanmış retro. Sadece okuma; hiçbir yazma çalışmaz.
- Yeni retro `currentPhase=WRITING` ile başlar.
- Faz geçişi yalnızca `POST /api/retros/{id}/phase` endpoint'i ile yapılır; ileri-yön zorunlu değildir, board sahibi geri de gidebilir (örn. VOTING'den GROUPING'e dönüş).
- `GET /api/retros/{id}` response'unda `currentPhase` döner. Frontend her fazda farklı UI gösterir (kart ekleme alanı vs. oy butonları vs. tartışma paneli).
- Yanlış fazda yapılan yazma çağrıları `423 LOCKED` döner ve response'da `{ "error": "PHASE_LOCKED", "requiredPhase": "WRITING" }`.
- Misafir kullanıcılar (`X-Guest-Session`) **faz geçişi yapamaz**; faz geçiş endpoint'ine guest istek 403 döner.
- Frontend retro tahtasının üstünde 4 adımlı progress bar gösterir; aktif fazı vurgular.

**Bağımlılıklar:** F-04, F-07, F-11, F-12, F-28

---

## F-33 — Board Sahibi Yetkileri
**User Story:** Bir retro açan kişi (board sahibi) olarak retro fazlarını ilerletmek ve oturum üzerindeki yönetimsel aksiyonları yapmak istiyorum, çünkü oturumu ben yönetiyorum ve diğer katılımcıların aynı anda kontrol kavgası vermesi retroyu bozar.

**Kabul Kriterleri:**
- "Board sahibi" = retro kaydındaki `createdByUserId` ile eşleşen oturumdaki kullanıcı.
- Aşağıdaki işlemler **sadece board sahibi** tarafından yapılabilir; diğer ekip üyeleri 403 alır:
  - Faz geçişi (`POST /api/retros/{id}/phase`)
  - Anonim mod toggle (`PATCH /api/retros/{id}` body: `anonymousMode`)
  - Retroyu kapatma (`PATCH /api/retros/{id}` body: `status=CLOSED`)
  - AI Analiz tetikleme (`POST /api/ai/analyze`)
  - Olgunluk skoru hesaplama (`POST /api/ai/maturity`)
  - Aksiyonları bulk Jira'ya gönderme (`POST /api/jira/bulk-create`)
  - Aksiyon onay listesini kalıcılaştırma (`POST /api/actions/bulk`)
- Aşağıdaki işlemler **tüm ekip üyeleri + misafirler** tarafından yapılabilir:
  - Kart ekleme/düzenleme/silme (kendi kartı)
  - Oy verme/geri çekme
  - "Next Steps" kolonundaki kartı görme, oy verme
  - "Sessiz katılımcı dürtüsü" prompt'unu görme
- Misafir kullanıcı asla board sahibi olamaz (retro misafir tarafından açılamaz).
- Ekip lideri (`LEADER`) ile board sahibi farklı kavramlardır:
  - `LEADER` rolü Jira bağlantısı kurma yetkisini taşır (F-10, F-21, F-30).
  - Board sahibi tek bir retroya özel yetkiyi taşır.
  - Aynı kişi her ikisi de olabilir, ancak board sahipliği retroya özeldir; lider olmayan üye de retro açtığında o retronun board sahibi olur.

**Bağımlılıklar:** F-03, F-10, F-32

---

## F-27 — Mock Jira Fallback
**User Story:** Bir geliştirici olarak Jira REST API'sine erişilemediğinde statik mock JSON üzerinden aynı UI'ın çalışmasını istiyorum, çünkü demo akışı kesintisiz devam etmelidir.

**Kabul Kriterleri:**
- Backend Jira REST çağrısı 401/timeout aldığında mock JSON döner; response'a `mock: true` flag eklenir.
- Frontend `mock: true` görürse top bar'da "Mock Mode" rozeti gösterir.
- Tüm endpoint'ler (sprint-context, bulk-create, issue) mock'ta çalışır.

**Bağımlılıklar:** F-21

---

## F-28 — Misafir Retro Katılımı (Guest Token + Display Name)
**User Story:** Bir misafir katılımcı olarak hesap oluşturmadan, paylaşılan retro linkine girip yalnızca görünen adımı (display name) yazarak retroya katılmak istiyorum, çünkü hesap açmak istemiyorum ancak ekipteki diğer kişilerin beni hangi adla göreceğini de ben belirlemek istiyorum.

**Kabul Kriterleri:**
- Her `RetroSession` oluşturulduğunda backend `guestJoinToken` (UUID v4) üretir ve persist eder.
- Misafir `/join/<token>` sayfasına geldiğinde önce bir **"Görünen Adınız" formu** görür; sadece tek alan: `displayName` (zorunlu, 2–40 karakter, harf/rakam/boşluk).
- Form gönderildiğinde `POST /api/retros/{id}/join` çağrılır; body `{ token, displayName }`. Backend doğrular ve yeni bir `guestSessionId` (UUID v4, in-memory) üretir.
- Aynı retroda aynı `displayName` ile ikinci kayıt 409 döner (UI "bu isim alınmış" mesajı gösterir).
- Misafir kart yazdığında / oy verdiğinde diğer üyeler kart sahibi olarak girdiği `displayName`'i görür (anonim mod açıksa yine "Anonim üye" gösterilir).
- Kart oluşturma, kart güncelleme, kart silme ve oy verme endpoint'leri `X-Guest-Session: <uuid>` header'ı ile çalışır; oturum açılmış bir kullanıcı kimliği yoksa misafir oturumu kullanılır.
- Misafirin oy kotası ekibin diğer üyeleriyle aynı: retro başına 3 oy.
- Misafir kendi oluşturduğu kart üzerinde düzenleme/silme hakkına sahiptir; başkasının kartına dokunamaz (403).
- Geçersiz / kapatılmış retro token'ı 404 veya 410 döner.
- Retro `CLOSED` duruma geçtiğinde tüm misafir session'ları geçersiz hale gelir (sonraki yazma çağrıları 410).
- Misafir email, parola, telefon vb. başka kişisel bilgi girmez — yalnızca displayName.

**Bağımlılıklar:** F-03, F-04, F-07

---

## F-29 — Devam Adımları (Next Steps) Sütunu
**User Story:** Bir ekip üyesi olarak retro tahtasında dördüncü bir sütunda "Devam Adımları" görmek istiyorum, çünkü tartıştığımız konulardan çıkan somut adımları kart olarak tahtanın üstünde işaretleyip aksiyona dönüştürebilmem gerekir.

**Kabul Kriterleri:**
- `RetroColumn` enum değerlerine `NEXT_STEPS` eklenir; toplam 4 kolon olur: `GOOD`, `IMPROVE`, `DISCUSS`, `NEXT_STEPS`.
- `GET /api/retros/{retroId}` response'undaki `cardCounts` objesinde `NEXT_STEPS` sayısı da döner.
- `GET /api/retros/{retroId}/cards` "Next Steps" kartlarını diğerleri gibi listeler; her kart `column: "NEXT_STEPS"` taşır.
- Kullanıcı manuel olarak kart ekleyebilir.
- AI Analiz çıktısı (F-11), önerilen SMART aksiyonları "Next Steps" kolonuna kart olarak da düşürür (her aksiyon önerisi için bir kart, `source: AI_NEXT_STEP`).
- "Next Steps" kolonundaki herhangi bir karttan tek tıkla aksiyon oluşturulabilir: `POST /api/actions/from-card` çağrısı kart başlığını title olarak kullanır.
- "Next Steps" kolonundaki kartlara da oy verilebilir.

**Bağımlılıklar:** F-04, F-11, F-12

---

## F-30 — Jira Board Seçimi (İki Adımlı Bağlantı)
**User Story:** Bir ekip lideri olarak Jira bağlantısı kurarken erişimim olan board listesini görüp hangi board'la çalışacağımı seçmek istiyorum, çünkü tek bir Atlassian hesabı birden fazla board'a erişebilir ve doğru board'u seçmem gerekir.

**Kabul Kriterleri:**
- Bağlantı iki adımda tamamlanır:
  1. `POST /api/jira/connect` — email + apiToken + jiraDomain alır, `GET /rest/api/3/myself` ile doğrular, `GET /rest/agile/1.0/board` ile erişilebilir board'ları listeler ve geçici (henüz `boardId` belirsiz) bir bağlantı kaydı yaratır. Response: `{connectionId, boards: [{id, name, type, projectKey}]}`.
  2. `POST /api/jira/connect/board` — `{connectionId, boardId}` alır, seçilen board ile bağlantıyı `CONNECTED` duruma geçirir.
- `GET /api/jira/boards?teamId=` kayıtlı bağlantıdaki güncel board listesini (Jira'dan canlı çekerek) döner; bağlantı yoksa 404.
- Eski tek-adımlı `POST /api/jira/connections` endpoint'i geriye dönük uyumluluk için kalır; doğrudan boardId alır ve tek adımda `CONNECTED` durumuna geçer.
- `boardId` seçimi yapılmamış bir bağlantı `PENDING_BOARD` durumdadır; bu durumda Jira-bağımlı çağrılar (sprint-context, bulk-create) 409 döner.
- Lider istediği zaman `POST /api/jira/connect/board` ile board'u değiştirebilir.

**Bağımlılıklar:** F-21

---

## F-31 — Atlassian OAuth ile Giriş
**User Story:** Bir kullanıcı olarak email/parola yerine Atlassian hesabımla tek tıkla giriş yapmak istiyorum, çünkü hem ayrı bir parola yönetmek istemiyorum hem de Jira bağlantısının altyapısı zaten Atlassian üzerinden geliyor.

**Kabul Kriterleri:**
- Giriş ekranında "Atlassian ile Giriş Yap" butonu vardır.
- `GET /api/auth/atlassian` çağrısı Atlassian OAuth 2.0 (3LO) authorize URL'sine 302 redirect yapar; `state` parametresi CSRF için imzalı/random üretilir ve cookie'de saklanır.
- `GET /api/auth/atlassian/callback?code=&state=` çağrısı:
  - `state` cookie ile eşleşmiyorsa 400 döner.
  - Code'u Atlassian token endpoint'ine takas eder; access token alır.
  - `GET https://api.atlassian.com/me` ile kullanıcı email + ad bilgisini çeker.
  - Email sistemde varsa `User` kaydını günceller, yoksa `passwordHash=NULL` ve `authProvider=ATLASSIAN` ile yeni kullanıcı yaratır.
  - JWT üretip `Set-Cookie` ile döner ve frontend'e `/teams` (veya hedef) yönlendirmesi yapar.
- Mevcut email+parola endpoint'leri (`/api/auth/register`, `/api/auth/login`) çalışmaya devam eder (fallback).
- Atlassian üzerinden gelen kullanıcı `/api/auth/me` çağrısında `authProvider: "ATLASSIAN"` alanını görür.
- Aynı kullanıcı sonradan local parola atayabilir (post-MVP, kapsam dışı); şimdilik provider sabit.

**Bağımlılıklar:** F-01

---

## Kapsam Dışı (pm-brief'ten)
Aşağıdaki maddeler MVP kapsamında değildir, endpoint veya ekran yazılmaz:
- WebSocket gerçek zamanlı senkronizasyon
- Admin paneli / organizasyon yönetimi / rol seçimi
- Yönetici Dashboard (cross-team)
- Git entegrasyonu
- Jira webhook (push)
- Jira ticket düzenleme/silme
- Linear / Asana / ClickUp / Trello / GitHub Issues / Azure DevOps
- Jira sprint oluşturma / yönetimi
- OAuth / SSO **dışındaki** sağlayıcılar (Google, GitHub, Microsoft). Atlassian OAuth desteklenir (bkz. F-31).
- Mobil uygulama
- Mad/Sad/Glad, Sailboat şablonları
- Push notification / e-posta
- Aksiyon yorum thread'leri
- Otomatik 5 Whys
- AI Moderatör Botu (gerçek zamanlı konuşma)
- Round-Robin tartışma modu
- Trend / sparkline grafikleri
