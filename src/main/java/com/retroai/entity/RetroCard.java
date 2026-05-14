package com.retroai.entity;

import com.retroai.enums.CardSource;
import com.retroai.enums.RetroColumn;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "retro_cards",
        indexes = @Index(name = "idx_card_retro", columnList = "retro_id"))
public class RetroCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retro_id", nullable = false)
    private Long retroId;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Lob
    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "column_name", nullable = false, length = 20)
    private RetroColumn column;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardSource source = CardSource.USER;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRetroId() { return retroId; }
    public void setRetroId(Long retroId) { this.retroId = retroId; }
    public Long getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(Long authorUserId) { this.authorUserId = authorUserId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public RetroColumn getColumn() { return column; }
    public void setColumn(RetroColumn column) { this.column = column; }
    public CardSource getSource() { return source; }
    public void setSource(CardSource source) { this.source = source; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
