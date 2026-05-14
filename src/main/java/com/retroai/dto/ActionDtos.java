package com.retroai.dto;

import com.retroai.enums.ActionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ActionDtos {

    public static class ActionInputItem {
        public String title;
        public String description;
        public Long assigneeUserId;
        public LocalDate deadline;
    }

    public static class BulkCreateRequest {
        @NotNull
        public Long retroId;
        @NotNull
        public List<ActionInputItem> actions;
    }

    public static class UpdateActionRequest {
        public ActionStatus status;
        public String title;
        public String description;
        public Long assigneeUserId;
        public LocalDate deadline;
    }

    public static class ActionResponse {
        public Long id;
        public Long retroId;
        public String title;
        public String description;
        public Long assigneeUserId;
        public String assigneeName;
        public LocalDate deadline;
        public ActionStatus status;
        public Short riskScore;
        public String riskReason;
        public String rewriteSuggestion;
        public String jiraKey;
        public String jiraUrl;
        public String carriedFromSprint;
        public Instant createdAt;

        public ActionResponse() {}
    }
}
