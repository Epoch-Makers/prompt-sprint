package com.retroai.entity;

import com.retroai.enums.ActionSource;
import com.retroai.enums.ActionStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "actions",
        indexes = {
                @Index(name = "idx_action_team_status", columnList = "team_id, status"),
                @Index(name = "idx_action_retro", columnList = "retro_id")
        })
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retro_id", nullable = false)
    private Long retroId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column
    private String description;

    @Column(name = "assignee_user_id")
    private Long assigneeUserId;

    @Column
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionStatus status = ActionStatus.OPEN;

    @Column(name = "risk_score")
    private Short riskScore;

    @Lob
    @Column(name = "risk_reason")
    private String riskReason;

    @Lob
    @Column(name = "rewrite_suggestion")
    private String rewriteSuggestion;

    @Column(name = "jira_key", length = 40)
    private String jiraKey;

    @Column(name = "jira_url", length = 500)
    private String jiraUrl;

    @Column(name = "carried_from_retro_id")
    private Long carriedFromRetroId;

    @Column(name = "carried_from_sprint", length = 120)
    private String carriedFromSprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionSource source = ActionSource.AI_SUGGESTED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRetroId() { return retroId; }
    public void setRetroId(Long retroId) { this.retroId = retroId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAssigneeUserId() { return assigneeUserId; }
    public void setAssigneeUserId(Long assigneeUserId) { this.assigneeUserId = assigneeUserId; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public ActionStatus getStatus() { return status; }
    public void setStatus(ActionStatus status) { this.status = status; }
    public Short getRiskScore() { return riskScore; }
    public void setRiskScore(Short riskScore) { this.riskScore = riskScore; }
    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }
    public String getRewriteSuggestion() { return rewriteSuggestion; }
    public void setRewriteSuggestion(String rewriteSuggestion) { this.rewriteSuggestion = rewriteSuggestion; }
    public String getJiraKey() { return jiraKey; }
    public void setJiraKey(String jiraKey) { this.jiraKey = jiraKey; }
    public String getJiraUrl() { return jiraUrl; }
    public void setJiraUrl(String jiraUrl) { this.jiraUrl = jiraUrl; }
    public Long getCarriedFromRetroId() { return carriedFromRetroId; }
    public void setCarriedFromRetroId(Long carriedFromRetroId) { this.carriedFromRetroId = carriedFromRetroId; }
    public String getCarriedFromSprint() { return carriedFromSprint; }
    public void setCarriedFromSprint(String carriedFromSprint) { this.carriedFromSprint = carriedFromSprint; }
    public ActionSource getSource() { return source; }
    public void setSource(ActionSource source) { this.source = source; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
