package com.retroai.entity;

import com.retroai.enums.JiraSignalType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "jira_insights")
public class JiraInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retro_id", nullable = false)
    private Long retroId;

    @Column(name = "ticket_key", nullable = false, length = 40)
    private String ticketKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 30)
    private JiraSignalType signalType;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(name = "suggested_card_title", nullable = false, length = 255)
    private String suggestedCardTitle;

    @Column(name = "accepted_as_card_id")
    private Long acceptedAsCardId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRetroId() { return retroId; }
    public void setRetroId(Long retroId) { this.retroId = retroId; }
    public String getTicketKey() { return ticketKey; }
    public void setTicketKey(String ticketKey) { this.ticketKey = ticketKey; }
    public JiraSignalType getSignalType() { return signalType; }
    public void setSignalType(JiraSignalType signalType) { this.signalType = signalType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSuggestedCardTitle() { return suggestedCardTitle; }
    public void setSuggestedCardTitle(String suggestedCardTitle) { this.suggestedCardTitle = suggestedCardTitle; }
    public Long getAcceptedAsCardId() { return acceptedAsCardId; }
    public void setAcceptedAsCardId(Long acceptedAsCardId) { this.acceptedAsCardId = acceptedAsCardId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
