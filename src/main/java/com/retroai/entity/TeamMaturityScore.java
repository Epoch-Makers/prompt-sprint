package com.retroai.entity;

import com.retroai.enums.MaturityLevel;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "team_maturity_scores")
public class TeamMaturityScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "retro_id", nullable = false)
    private Long retroId;

    @Column(nullable = false)
    private Short score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaturityLevel level;

    @Column(name = "action_completion_rate", nullable = false, precision = 3, scale = 2)
    private BigDecimal actionCompletionRate;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal smartness;

    @Column(name = "recurring_issue_absence", nullable = false, precision = 3, scale = 2)
    private BigDecimal recurringIssueAbsence;

    @Lob
    @Column(name = "tips_json", nullable = false)
    private String tipsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getRetroId() { return retroId; }
    public void setRetroId(Long retroId) { this.retroId = retroId; }
    public Short getScore() { return score; }
    public void setScore(Short score) { this.score = score; }
    public MaturityLevel getLevel() { return level; }
    public void setLevel(MaturityLevel level) { this.level = level; }
    public BigDecimal getActionCompletionRate() { return actionCompletionRate; }
    public void setActionCompletionRate(BigDecimal actionCompletionRate) { this.actionCompletionRate = actionCompletionRate; }
    public BigDecimal getSmartness() { return smartness; }
    public void setSmartness(BigDecimal smartness) { this.smartness = smartness; }
    public BigDecimal getRecurringIssueAbsence() { return recurringIssueAbsence; }
    public void setRecurringIssueAbsence(BigDecimal recurringIssueAbsence) { this.recurringIssueAbsence = recurringIssueAbsence; }
    public String getTipsJson() { return tipsJson; }
    public void setTipsJson(String tipsJson) { this.tipsJson = tipsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
