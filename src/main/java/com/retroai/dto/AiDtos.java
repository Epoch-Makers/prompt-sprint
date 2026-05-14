package com.retroai.dto;

import com.retroai.enums.JiraSignalType;
import com.retroai.enums.MaturityLevel;
import com.retroai.enums.Urgency;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AiDtos {

    public static class AnalyzeRequest {
        @NotNull
        public Long retroId;
    }

    public static class Theme {
        public String title;
        public Integer moralScore;
        public Urgency urgency;
        public List<Long> cardIds;

        public Theme() {}
        public Theme(String title, Integer moralScore, Urgency urgency, List<Long> cardIds) {
            this.title = title; this.moralScore = moralScore; this.urgency = urgency; this.cardIds = cardIds;
        }
    }

    public static class SuggestedAction {
        public String title;
        public String description;
        public Long suggestedAssigneeUserId;
        public LocalDate suggestedDeadline;
        public String themeTitle;

        public SuggestedAction() {}
    }

    public static class AnalyzeResponse {
        public List<Theme> themes;
        public List<SuggestedAction> actions;

        public AnalyzeResponse() {}
        public AnalyzeResponse(List<Theme> themes, List<SuggestedAction> actions) {
            this.themes = themes; this.actions = actions;
        }
    }

    public static class RiskScoreRequest {
        @NotNull
        public List<Long> actionIds;
    }

    public static class RiskScoreItem {
        public Long actionId;
        public Integer riskScore;
        public String reason;
        public String rewriteSuggestion;

        public RiskScoreItem() {}
        public RiskScoreItem(Long actionId, Integer riskScore, String reason, String rewriteSuggestion) {
            this.actionId = actionId; this.riskScore = riskScore;
            this.reason = reason; this.rewriteSuggestion = rewriteSuggestion;
        }
    }

    public static class MaturityRequest {
        @NotNull
        public Long retroId;
    }

    public static class MaturityResponse {
        public Integer score;
        public MaturityLevel level;
        public Map<String, Double> components;
        public List<String> tips;

        public MaturityResponse() {}
    }

    public static class RecurringTheme {
        public String title;
        public Integer occurrenceCount;

        public RecurringTheme() {}
        public RecurringTheme(String title, Integer occurrenceCount) {
            this.title = title; this.occurrenceCount = occurrenceCount;
        }
    }

    public static class BriefingResponse {
        public String prevRetroSummary;
        public Integer doneCount;
        public Integer inProgressCount;
        public Integer atRiskCount;
        public List<RecurringTheme> recurringThemes;

        public BriefingResponse() {}
    }

    public static class SilentPromptResponse {
        public String prompt;
        public Boolean shouldShow;

        public SilentPromptResponse() {}
        public SilentPromptResponse(String prompt, Boolean shouldShow) {
            this.prompt = prompt; this.shouldShow = shouldShow;
        }
    }

    public static class JiraHistoryRequest {
        @NotNull
        public Long retroId;
    }

    public static class JiraInsightItem {
        public String ticketKey;
        public JiraSignalType signalType;
        public String description;
        public String suggestedCardTitle;

        public JiraInsightItem() {}
        public JiraInsightItem(String ticketKey, JiraSignalType signalType, String description, String suggestedCardTitle) {
            this.ticketKey = ticketKey; this.signalType = signalType;
            this.description = description; this.suggestedCardTitle = suggestedCardTitle;
        }
    }

    public static class JiraHistoryResponse {
        public List<JiraInsightItem> insights;

        public JiraHistoryResponse() {}
        public JiraHistoryResponse(List<JiraInsightItem> insights) { this.insights = insights; }
    }
}
