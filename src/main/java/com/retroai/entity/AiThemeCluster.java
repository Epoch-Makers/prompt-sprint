package com.retroai.entity;

import com.retroai.enums.Urgency;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_theme_clusters",
        indexes = @Index(name = "idx_theme_team_title", columnList = "team_id, title"))
public class AiThemeCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retro_id", nullable = false)
    private Long retroId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "moral_score", nullable = false)
    private Short moralScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgency urgency;

    @Lob
    @Column(name = "card_ids_json", nullable = false)
    private String cardIdsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRetroId() { return retroId; }
    public void setRetroId(Long retroId) { this.retroId = retroId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Short getMoralScore() { return moralScore; }
    public void setMoralScore(Short moralScore) { this.moralScore = moralScore; }
    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
    public String getCardIdsJson() { return cardIdsJson; }
    public void setCardIdsJson(String cardIdsJson) { this.cardIdsJson = cardIdsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
