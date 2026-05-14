# RetroAI — PM Brief

## Ürün Özeti

RetroAI, agile takımların retrospektifini tek platformda toplayan ve alınan aksiyonları **otomatik olarak Jira backlog'una** taşıyarak "unutulan aksiyon" problemini sistemik olarak çözen bir Retro & Action Tracker'dır. AI; tema kümeleme, SMART aksiyon üretimi, unutulma risk skoru ve ekip olgunluk skoru üretir. Atlassian Jira ile çift yönlü entegredir (REST API ile CRUD + MCP ile AI analizi).

---

## Özellikler (MVP)

### Retro Temel Akış
1. **Çoklu Ekip Desteği** — N tane ekip oluşturulabilir; her ekibin kendi retro geçmişi, aksiyon listesi ve Jira bağlantısı ayrıdır. Üst bar'da ekip seçici dropdown ile geçiş yapılır.
2. **Retro Oturumu Oluşturma** — Ekip + sprint adı + retro adı ile yeni retro açılır.
3. **Üç Sütunlu Kart Tahtası** — "İyi Giden / Geliştirilebilir / Tartışılacak" kolonları. Her kullanıcı serbest metin kart ekler, düzenler, siler.
4. **Manuel Mod (Hybrid)** — Jira bağlantısı olmadan da tüm temel özellikler (kart girişi, AI Analiz, Olgunluk Skoru) çalışır. Jira-bağımlı UI elementleri Jira yoksa gizlenir.

### Equal Voice — Katılım Paketi
5. **Anonim Kart Modu** — Varsayılan açık. Kart yazarı UI'da gözükmez, "Anonim üye" olarak görünür. Toggle ile kapatılabilir.
6. **Eşit Oy Kotası** — Her üyeye retro başına 3 oy hakkı. Top bar'da kalan oy sayısı gösterilir.
7. **Katılım Göstergesi** — Top bar'da her ekip üyesi için renkli nokta (yeşil: 2+ kart, sarı: 1 kart, gri: 0 kart). Polling tabanlı (5sn).
8. **Sessiz Katılımcı Dürtüsü (AI)** — 10 dakika sonra hâlâ 0 kart yazmış üyeye, yalnızca ona görünür AI-üretimi başlangıç sorusu gösterilir.

### Ekip Lideri Yetki Farkı
9. **Ekip Lideri Rolü** — Tek yetki farkı: **ekip lideri Jira bağlantısını kurar** (email + API token girer). Diğer ekip üyeleri retroya katılır, kart ekler, oy verir — başka yetki farkı yoktur.

### AI Analiz
10. **Tema Kümeleme + SMART Aksiyon Üretimi** — "AI Analiz Et" butonu retro kartlarını okur; benzer kartları kümeler, her kümeye başlık + moral skoru + aciliyet etiketi atar; SMART aksiyon önerileri üretir.
11. **Aksiyon Onay Ekranı** — AI önerilerini kullanıcı çekboks listesinde inceler; başlık ve assignee düzenler, istemediklerini işaretten kaldırır.

### Jira Aksiyon Yazma
12. **Aksiyonları Onayla & Jira'ya Gönder (Bulk)** — Onaylanan aksiyonların tamamı paralel olarak `POST /rest/api/3/issue` ile Jira'ya ticket olarak yazılır. Her ticket'a `retro-action` + `retro-sprint-N` label otomatik eklenir. Assignee retroda atandıysa Jira ticket'ına da set edilir.
13. **Tekil Manuel Ticket Yaratma** — Aksiyonlar sekmesinde sonradan eklenen manuel bir aksiyon için "+ Jira Ticket Yarat" butonu.
14. **Aksiyon Listesinde Jira Bağlantısı** — Jira'ya yazılan her aksiyonun yanında ticket key + "Jira'da Aç →" link.

### Action Risk Score (X-Factor 1)
15. **Unutulma Risk Skoru** — AI her aksiyona 1–5 arası risk skoru atar. Sinyaller: sahip yokluğu, deadline yokluğu, muğlak ifade, son güncelleme gecikmesi, deadline yaklaşması.
16. **Risk Radarı Paneli** — Aksiyonlar sekmesinin en üstünde kritik (risk 4-5) aksiyonlar kırmızı renkte; her birinin AI gerekçesi ("Sahibi yok, 5 gündür hareket yok, deadline yaklaşıyor") ve AI rewrite önerisi gösterilir.

### Otomatik Carry-Over (X-Factor 2)
17. **Onay-sız Otomatik Taşıma** — Yeni retro açıldığında aynı ekibin önceki retrosundaki kapanmamış aksiyonlar (status != "Tamam") **otomatik olarak** yeni retroya kopyalanır. Kullanıcıya sorulmaz. Üst bant'ta bilgilendirme banner'ı: "🔄 3 aksiyon önceki retrodan otomatik taşındı". Aksiyonlar "Sprint N'den devreden" etiketiyle gösterilir.

### Pattern Detector (X-Factor 3)
18. **Tekrar Eden Tema Uyarısı** — AI birden fazla retro üzerinde tema kümelerini karşılaştırır; aynı tema 2+ kez çıktığında retro açılış brifinginde uyarı gösterir: "Bu 3. sprint'te 'CI/CD yavaşlığı' konusu çıkıyor — kök neden analizi yapalım mı?"

### Jira'dan Veri Çekme (X-Factor 4)
19. **Sprint Bağlamı Bandı** — Retro açıldığında Jira'dan aktif sprint kartları çekilir; AI özet üretir: "12 story planlandı, 8 tamam, 2 bug açıldı, velocity %70, en çok bug payment-service'te". Retro tahtasının üstünde banner olarak gösterilir.
20. **Status & History AI Analizi** — AI sadece açık ticket'lara değil, **ticket geçmişine** bakar: status geçişleri (`expand=changelog`), yorum akışı, atama değişiklikleri, sprint atlamaları. Bunlardan aksiyon önerileri çıkarır:
    - "BUG-1453 3 kez 'In Progress'→'To Do' döndü — gereksinim netleştirme retroda ele alınmalı"
    - "STORY-892 son 4 sprint'tir carry-over oldu — backlog refinement sorgulanmalı"
    - "Bu sprintte 4 bug 'Done'→'Reopened' geçişi yaptı — QA gate sorgulanmalı"
21. **Jira-Kaynaklı Kart Önerileri** — AI'ın ürettiği bu öneriler AI Analiz panelinde "Jira-kaynaklı" etiketiyle ayrı listelenir. Kullanıcı kabul ederse kart "Geliştirilebilir" kolonuna 🤖 ikonuyla düşer.

### Ekip Olgunluk Skoru (X-Factor 5)
22. **0–100 Olgunluk Skoru** — Her retrodan sonra AI üç bileşene bakarak skor üretir: aksiyon tamamlama oranı, aksiyon SMART'lığı, tekrar eden sorun yokluğu.
23. **Olgunluk Seviyesi & AI Önerisi** — Skora göre seviye (Forming / Norming / Performing / Mastery) ve takıma özel 3 maddelik iyileştirme planı gösterilir.

### Açılış Brifingi
24. **Önceki Retro Özeti** — Yeni retro açıldığında AI önceki retronun aksiyon durumunu özetler: "Geçen retroda 4 aksiyon vardı. 2 tamam, 1 devam, 1 RİSK ALTINDA". Pattern Detector uyarısı varsa birlikte gösterilir.

### Demo Riskini Sıfırlama
25. **Mock Jira Fallback** — Jira REST/MCP bağlantısı kurulamazsa statik mock JSON ile aynı UI çalışır; demo akışı kesintisiz devam eder.

---

## Jira Entegrasyon Mimarisi

**Hibrit yapı:** İki katman, iki amaç.

| Katman | Teknoloji | Sorumluluk |
|--------|-----------|------------|
| **CRUD & Auth** | Atlassian REST API | Auth doğrulama, project/board/sprint listeleme, sprint kartı çekme, ticket oluşturma |
| **AI Analiz** | Atlassian MCP | LLM'in Jira verisini dinamik yorumlaması: status/history pattern tespiti, velocity korelasyonu, cross-sprint analiz |

**Auth yöntemi:** Basic Auth — `Authorization: Basic Base64(email:apiToken)`

**Kullanılan REST endpoint'leri:**
- `GET /rest/api/3/myself` — kimlik doğrulama testi
- `GET /rest/api/3/project` — erişilebilir projeleri listele
- `GET /rest/agile/1.0/board` — board listesi
- `GET /rest/agile/1.0/board/{boardId}/sprint?state=active` — aktif sprint(ler)
- `GET /rest/agile/1.0/sprint/{sprintId}/issue?expand=changelog,comments` — sprint kartları + geçmiş + yorumlar
- `POST /rest/api/3/issue` — aksiyondan Jira ticket oluştur (bulk akışında paralel çağrılır)

**MCP ne için:** Yalnızca AI'ın Jira verisini dinamik sorgulayarak yorumlamasını gerektiren senaryolar — velocity korelasyonu, cross-sprint pattern detection. Yazma işlemleri (ticket oluşturma) **REST üzerinden deterministik** yapılır, MCP üzerinden değil.

**Önemli ayrım:** Atlassian MCP'nin iki kullanım yeri var — (1) geliştirme sürecinde Claude Code'un Jira'ya erişimi, (2) ürün içindeki LLM analiz motoru. Kullanıcı akışındaki tüm CRUD ve auth her zaman REST üzerindendir.

**Token güvenliği:** Token UI'dan alınır, backend session memory'de tutulur, plaintext olarak DB'ye yazılmaz. `.env` zorunlu değildir.

**Fallback:** Bağlantı kurulamazsa Mock Jira modu (statik JSON) devreye girer; `retro-action` label simüle edilir, UI'da küçük "Mock Mode" etiketi gösterilir.

---

## Kapsam Dışı

- Gerçek zamanlı çoklu kullanıcı senkronizasyonu (WebSocket) — polling kullanılır
- Karmaşık rol/yetki sistemi (Admin paneli, organizasyon yönetimi, rol seçimi ekranı) — sadece "Ekip Lideri Jira bağlar" farkı var
- Yönetici Dashboard (cross-team) — post-MVP
- Git entegrasyonu (commit/PR/review verisi) — post-MVP
- Jira'dan gerçek zamanlı senkronizasyon (webhook)
- Jira ticket'larını RetroAI'dan düzenleme/silme — sadece oluşturma
- Jira dışı issue tracker'lar (Linear, Asana, ClickUp, Trello, GitHub Issues, Azure DevOps)
- Jira sprint oluşturma / sprint yönetimi — sadece okuma
- OAuth / SSO — basit email+parola yeterli
- Mobil uygulama
- Özelleştirilebilir tahta şablonları (Mad/Sad/Glad, Sailboat vb.)
- Push notification / e-posta bildirimi
- Aksiyon yorumları / thread'ler
- Otomatik 5 Whys kök neden analizi
- AI Moderatör Botu (gerçek zamanlı konuşma)
- Round-Robin tartışma modu
- Trend grafikleri / sparkline (tek retro için anlamsız, post-MVP)

---

TAMAMLANDI
