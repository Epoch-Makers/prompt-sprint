package com.retroai.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "votes",
        uniqueConstraints = @UniqueConstraint(name = "uq_vote_card_user", columnNames = {"card_id", "user_id"}),
        indexes = {
                @Index(name = "idx_vote_card_user", columnList = "card_id, user_id", unique = true),
                @Index(name = "idx_vote_user_retro", columnList = "user_id, retro_id")
        })
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "retro_id", nullable = false)
    private Long retroId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRetroId() { return retroId; }
    public void setRetroId(Long retroId) { this.retroId = retroId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
