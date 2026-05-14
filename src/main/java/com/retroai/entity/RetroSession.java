package com.retroai.entity;

import com.retroai.enums.RetroPhase;
import com.retroai.enums.RetroStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "retro_sessions",
        indexes = @Index(name = "idx_retro_team_status", columnList = "team_id, status"))
public class RetroSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "sprint_name", nullable = false, length = 120)
    private String sprintName;

    @Column(name = "retro_name", nullable = false, length = 200)
    private String retroName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetroStatus status = RetroStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", nullable = false, length = 20)
    private RetroPhase currentPhase = RetroPhase.WRITING;

    @Column(name = "anonymous_mode", nullable = false)
    private boolean anonymousMode = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "guest_token", length = 64, unique = true)
    private String guestToken;

    public String getGuestToken() { return guestToken; }
    public void setGuestToken(String guestToken) { this.guestToken = guestToken; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getSprintName() { return sprintName; }
    public void setSprintName(String sprintName) { this.sprintName = sprintName; }
    public String getRetroName() { return retroName; }
    public void setRetroName(String retroName) { this.retroName = retroName; }
    public RetroStatus getStatus() { return status; }
    public void setStatus(RetroStatus status) { this.status = status; }
    public RetroPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(RetroPhase currentPhase) { this.currentPhase = currentPhase; }
    public boolean isAnonymousMode() { return anonymousMode; }
    public void setAnonymousMode(boolean anonymousMode) { this.anonymousMode = anonymousMode; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
}
