package com.retroai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retroai.ai.AiClient;
import com.retroai.dto.AiDtos;
import com.retroai.entity.Action;
import com.retroai.entity.AiThemeCluster;
import com.retroai.entity.JiraInsight;
import com.retroai.entity.RetroCard;
import com.retroai.entity.RetroSession;
import com.retroai.entity.TeamMaturityScore;
import com.retroai.enums.ActionStatus;
import com.retroai.enums.JiraSignalType;
import com.retroai.enums.MaturityLevel;
import com.retroai.enums.RetroColumn;
import com.retroai.enums.Urgency;
import com.retroai.exception.ApiException;
import com.retroai.repository.ActionRepository;
import com.retroai.repository.AiThemeClusterRepository;
import com.retroai.repository.JiraInsightRepository;
import com.retroai.repository.RetroCardRepository;
import com.retroai.repository.RetroSessionRepository;
import com.retroai.repository.TeamMaturityScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiClient aiClient;
    private final RetroSessionRepository retroRepo;
    private final RetroCardRepository cardRepo;
    private final ActionRepository actionRepo;
    private final AiThemeClusterRepository themeRepo;
    private final JiraInsightRepository jiraInsightRepo;
    private final TeamMaturityScoreRepository maturityRepo;
    private final TeamService teamService;

    public AiService(AiClient aiClient,
                     RetroSessionRepository retroRepo,
                     RetroCardRepository cardRepo,
                     ActionRepository actionRepo,
                     AiThemeClusterRepository themeRepo,
                     JiraInsightRepository jiraInsightRepo,
                     TeamMaturityScoreRepository maturityRepo,
                     TeamService teamService) {
        this.aiClient = aiClient;
        this.retroRepo = retroRepo;
        this.cardRepo = cardRepo;
        this.actionRepo = actionRepo;
        this.themeRepo = themeRepo;
        this.jiraInsightRepo = jiraInsightRepo;
        this.maturityRepo = maturityRepo;
        this.teamService = teamService;
    }

    @Transactional
    public AiDtos.AnalyzeResponse analyze(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());
        List<RetroCard> cards = cardRepo.findByRetroIdOrderByCreatedAtAsc(retroId);

        AiDtos.AnalyzeResponse aiOut = callAnalyzeLlm(cards);
        if (aiOut == null) {
            aiOut = stubAnalyze(cards);
        }

        // persist themes for pattern detector
        for (AiDtos.Theme t : aiOut.themes) {
            AiThemeCluster c = new AiThemeCluster();
            c.setRetroId(r.getId());
            c.setTeamId(r.getTeamId());
            c.setTitle(t.title);
            c.setMoralScore(t.moralScore == null ? 0 : t.moralScore.shortValue());
            c.setUrgency(t.urgency == null ? Urgency.MEDIUM : t.urgency);
            try {
                c.setCardIdsJson(MAPPER.writeValueAsString(t.cardIds == null ? List.of() : t.cardIds));
            } catch (Exception e) {
                c.setCardIdsJson("[]");
            }
            themeRepo.save(c);
        }
        return aiOut;
    }

    private AiDtos.AnalyzeResponse callAnalyzeLlm(List<RetroCard> cards) {
        if (!aiClient.isConfigured() || cards.isEmpty()) return null;
        StringBuilder prompt = new StringBuilder();
        prompt.append("Retro cards (id | column | content):\n");
        for (RetroCard c : cards) {
            prompt.append(c.getId()).append(" | ").append(c.getColumn()).append(" | ").append(c.getContent()).append("\n");
        }
        prompt.append("\nGroup cards into themes. Produce SMART action items. ")
                .append("Return JSON: { \"themes\": [{\"title\":\"...\",\"moralScore\":-3,\"urgency\":\"HIGH|MEDIUM|LOW\",\"cardIds\":[]}], ")
                .append("\"actions\":[{\"title\":\"...\",\"description\":\"...\",\"suggestedAssigneeUserId\":null,\"suggestedDeadline\":\"YYYY-MM-DD\",\"themeTitle\":\"...\"}] }");

        JsonNode root = aiClient.completeJson(
                "You are a Scrum master assistant clustering retrospective cards.",
                prompt.toString());
        if (root == null) return null;

        List<AiDtos.Theme> themes = new ArrayList<>();
        for (JsonNode t : root.path("themes")) {
            AiDtos.Theme th = new AiDtos.Theme();
            th.title = t.path("title").asText("Theme");
            th.moralScore = t.path("moralScore").asInt(0);
            try {
                th.urgency = Urgency.valueOf(t.path("urgency").asText("MEDIUM"));
            } catch (Exception e) { th.urgency = Urgency.MEDIUM; }
            List<Long> ids = new ArrayList<>();
            for (JsonNode id : t.path("cardIds")) ids.add(id.asLong());
            th.cardIds = ids;
            themes.add(th);
        }
        List<AiDtos.SuggestedAction> actions = new ArrayList<>();
        for (JsonNode a : root.path("actions")) {
            AiDtos.SuggestedAction sa = new AiDtos.SuggestedAction();
            sa.title = a.path("title").asText("");
            sa.description = a.path("description").asText("");
            if (a.hasNonNull("suggestedAssigneeUserId")) {
                sa.suggestedAssigneeUserId = a.get("suggestedAssigneeUserId").asLong();
            }
            String dl = a.path("suggestedDeadline").asText(null);
            if (dl != null && !dl.isBlank()) {
                try { sa.suggestedDeadline = LocalDate.parse(dl); } catch (Exception ignored) {}
            }
            sa.themeTitle = a.path("themeTitle").asText("");
            actions.add(sa);
        }
        return new AiDtos.AnalyzeResponse(themes, actions);
    }

    private AiDtos.AnalyzeResponse stubAnalyze(List<RetroCard> cards) {
        Map<RetroColumn, List<Long>> grouped = new HashMap<>();
        for (RetroCard c : cards) {
            grouped.computeIfAbsent(c.getColumn(), k -> new ArrayList<>()).add(c.getId());
        }
        List<AiDtos.Theme> themes = new ArrayList<>();
        if (grouped.containsKey(RetroColumn.IMPROVE)) {
            themes.add(new AiDtos.Theme("Geliştirilebilir alanlar", -2, Urgency.HIGH, grouped.get(RetroColumn.IMPROVE)));
        }
        if (grouped.containsKey(RetroColumn.DISCUSS)) {
            themes.add(new AiDtos.Theme("Tartışılacak konular", 0, Urgency.MEDIUM, grouped.get(RetroColumn.DISCUSS)));
        }
        if (grouped.containsKey(RetroColumn.GOOD)) {
            themes.add(new AiDtos.Theme("İyi giden noktalar", 3, Urgency.LOW, grouped.get(RetroColumn.GOOD)));
        }
        List<AiDtos.SuggestedAction> actions = new ArrayList<>();
        if (!themes.isEmpty()) {
            AiDtos.SuggestedAction sa = new AiDtos.SuggestedAction();
            sa.title = "Önemli temalar için ekip içi takip oturumu planla";
            sa.description = "Retroda öne çıkan başlıkları sahiplenip 2 hafta içinde follow-up yap.";
            sa.suggestedDeadline = LocalDate.now().plusWeeks(2);
            sa.themeTitle = themes.get(0).title;
            actions.add(sa);
        }
        return new AiDtos.AnalyzeResponse(themes, actions);
    }

    @Transactional
    public List<AiDtos.RiskScoreItem> riskScore(Long userId, List<Long> actionIds) {
        List<AiDtos.RiskScoreItem> out = new ArrayList<>();
        for (Long id : actionIds) {
            Action a = actionRepo.findById(id).orElse(null);
            if (a == null) continue;
            teamService.requireMember(userId, a.getTeamId());
            int score = computeRiskScore(a);
            String reason = buildRiskReason(a);
            String rewrite = buildRewrite(a);
            a.setRiskScore((short) score);
            a.setRiskReason(reason);
            a.setRewriteSuggestion(rewrite);
            actionRepo.save(a);
            out.add(new AiDtos.RiskScoreItem(a.getId(), score, reason, rewrite));
        }
        return out;
    }

    private int computeRiskScore(Action a) {
        int score = 1;
        if (a.getAssigneeUserId() == null) score += 2;
        if (a.getDeadline() == null) score += 1;
        if (a.getTitle() == null || a.getTitle().length() < 20) score += 1;
        if (a.getDeadline() != null && a.getDeadline().isBefore(LocalDate.now().plusDays(3))) score += 1;
        return Math.min(5, score);
    }

    private String buildRiskReason(Action a) {
        List<String> reasons = new ArrayList<>();
        if (a.getAssigneeUserId() == null) reasons.add("sahibi yok");
        if (a.getDeadline() == null) reasons.add("deadline yok");
        if (a.getTitle() != null && a.getTitle().length() < 20) reasons.add("başlık muğlak");
        if (a.getDeadline() != null && a.getDeadline().isBefore(LocalDate.now().plusDays(3))) reasons.add("deadline yaklaşıyor");
        return reasons.isEmpty() ? "Sinyal yok" : String.join(", ", reasons);
    }

    private String buildRewrite(Action a) {
        String base = a.getTitle() == null ? "" : a.getTitle();
        return base + " — sahip ata, ölçülebilir hedef belirle, son tarih ekle";
    }

    @Transactional
    public AiDtos.MaturityResponse maturity(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        List<Action> actions = actionRepo.findByRetroIdOrderByCreatedAtDesc(retroId);
        long done = actions.stream().filter(a -> a.getStatus() == ActionStatus.DONE).count();
        double completion = actions.isEmpty() ? 0.0 : (double) done / actions.size();

        long smartCount = actions.stream()
                .filter(a -> a.getDeadline() != null && a.getAssigneeUserId() != null
                        && a.getTitle() != null && a.getTitle().length() >= 20)
                .count();
        double smartness = actions.isEmpty() ? 0.0 : (double) smartCount / actions.size();

        List<AiThemeCluster> teamThemes = themeRepo.findByTeamId(r.getTeamId());
        Map<String, Integer> titleCount = new HashMap<>();
        for (AiThemeCluster c : teamThemes) {
            titleCount.merge(c.getTitle(), 1, Integer::sum);
        }
        long recurring = titleCount.values().stream().filter(v -> v >= 2).count();
        double recurringAbsence = Math.max(0.0, 1.0 - (recurring * 0.2));

        int score = (int) Math.round((completion * 40) + (smartness * 30) + (recurringAbsence * 30));
        MaturityLevel level =
                score >= 85 ? MaturityLevel.MASTERY :
                score >= 65 ? MaturityLevel.PERFORMING :
                score >= 45 ? MaturityLevel.NORMING : MaturityLevel.FORMING;

        List<String> tips = buildMaturityTips(completion, smartness, recurringAbsence);

        TeamMaturityScore stored = new TeamMaturityScore();
        stored.setTeamId(r.getTeamId());
        stored.setRetroId(r.getId());
        stored.setScore((short) score);
        stored.setLevel(level);
        stored.setActionCompletionRate(BigDecimal.valueOf(completion).setScale(2, RoundingMode.HALF_UP));
        stored.setSmartness(BigDecimal.valueOf(smartness).setScale(2, RoundingMode.HALF_UP));
        stored.setRecurringIssueAbsence(BigDecimal.valueOf(recurringAbsence).setScale(2, RoundingMode.HALF_UP));
        try {
            stored.setTipsJson(MAPPER.writeValueAsString(tips));
        } catch (Exception e) {
            stored.setTipsJson("[]");
        }
        maturityRepo.save(stored);

        AiDtos.MaturityResponse out = new AiDtos.MaturityResponse();
        out.score = score;
        out.level = level;
        Map<String, Double> components = new HashMap<>();
        components.put("actionCompletionRate", round2(completion));
        components.put("smartness", round2(smartness));
        components.put("recurringIssueAbsence", round2(recurringAbsence));
        out.components = components;
        out.tips = tips;
        return out;
    }

    private List<String> buildMaturityTips(double completion, double smartness, double recurringAbsence) {
        List<String> tips = new ArrayList<>();
        if (completion < 0.7) tips.add("Aksiyon tamamlama oranını yükselt — sahipsiz aksiyon bırakma");
        if (smartness < 0.7) tips.add("Aksiyon başlıklarına ölçülebilir hedef ekle");
        if (recurringAbsence < 0.7) tips.add("Tekrar eden temalara kök neden oturumu planla");
        while (tips.size() < 3) tips.add("Retro öncesi 5 dakikalık hazırlık yap");
        return tips.subList(0, 3);
    }

    private double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public AiDtos.BriefingResponse briefing(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        AiDtos.BriefingResponse out = new AiDtos.BriefingResponse();
        out.doneCount = 0;
        out.inProgressCount = 0;
        out.atRiskCount = 0;
        out.recurringThemes = new ArrayList<>();

        List<RetroSession> prevs = retroRepo.findByTeamIdAndIdNotOrderByCreatedAtDesc(r.getTeamId(), r.getId());
        if (prevs.isEmpty()) {
            out.prevRetroSummary = null;
            return out;
        }
        RetroSession prev = prevs.get(0);
        List<Action> prevActions = actionRepo.findByRetroIdOrderByCreatedAtDesc(prev.getId());
        int total = prevActions.size();
        for (Action a : prevActions) {
            if (a.getStatus() == ActionStatus.DONE) out.doneCount++;
            else if (a.getStatus() == ActionStatus.IN_PROGRESS) out.inProgressCount++;
            else if (a.getStatus() == ActionStatus.AT_RISK) out.atRiskCount++;
        }
        out.prevRetroSummary = "Geçen retroda " + total + " aksiyon vardı.";

        // recurring themes
        List<AiThemeCluster> teamThemes = themeRepo.findByTeamId(r.getTeamId());
        Map<String, Integer> counts = new HashMap<>();
        for (AiThemeCluster c : teamThemes) {
            if (!c.getRetroId().equals(r.getId())) {
                counts.merge(c.getTitle(), 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() >= 2) {
                out.recurringThemes.add(new AiDtos.RecurringTheme(e.getKey(), e.getValue()));
            }
        }
        return out;
    }

    public AiDtos.SilentPromptResponse silentPrompt(Long userId, Long retroId, Long targetUserId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        // Silent-prompt only fires during the WRITING phase. Other phases:
        // hide the nudge by returning shouldShow=false.
        if (r.getCurrentPhase() != com.retroai.enums.RetroPhase.WRITING) {
            return new AiDtos.SilentPromptResponse(null, false);
        }

        long cardCount = cardRepo.countByRetroIdAndAuthorUserId(retroId, targetUserId);
        if (cardCount > 0) {
            return new AiDtos.SilentPromptResponse(null, false);
        }
        String prompt = "Bu sprintte seni en çok ne mutlu etti? Tek cümleyle yazabilir misin?";
        return new AiDtos.SilentPromptResponse(prompt, true);
    }

    @Transactional
    public AiDtos.JiraHistoryResponse jiraHistory(Long userId, Long retroId, List<AiDtos.JiraInsightItem> externalInsights) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        List<AiDtos.JiraInsightItem> items = new ArrayList<>(
                externalInsights == null ? Collections.emptyList() : externalInsights);
        if (items.isEmpty()) {
            // baseline stub example to keep UI alive when Jira is offline
            items.add(new AiDtos.JiraInsightItem(
                    "DEMO-1",
                    JiraSignalType.LONG_IDLE,
                    "Ticket 7 gündür hareket etmiyor",
                    "DEMO-1: Sahipliği netleştir, status güncelle"));
        }
        for (AiDtos.JiraInsightItem it : items) {
            JiraInsight entity = new JiraInsight();
            entity.setRetroId(r.getId());
            entity.setTicketKey(it.ticketKey);
            entity.setSignalType(it.signalType);
            entity.setDescription(it.description);
            entity.setSuggestedCardTitle(it.suggestedCardTitle);
            jiraInsightRepo.save(entity);
        }
        return new AiDtos.JiraHistoryResponse(items);
    }
}
