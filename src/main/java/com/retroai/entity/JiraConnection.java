package com.retroai.entity;

import com.retroai.enums.JiraConnectionStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "jira_connections",
        uniqueConstraints = @UniqueConstraint(name = "uq_jira_team", columnNames = "team_id"),
        indexes = @Index(name = "idx_jira_team", columnList = "team_id", unique = true))
public class JiraConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "jira_domain", nullable = false, length = 255)
    private String jiraDomain;

    @Column(name = "project_key", nullable = false, length = 40)
    private String projectKey;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "selected_board_id")
    private Long selectedBoardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JiraConnectionStatus status = JiraConnectionStatus.CONNECTED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getJiraDomain() { return jiraDomain; }
    public void setJiraDomain(String jiraDomain) { this.jiraDomain = jiraDomain; }
    public String getProjectKey() { return projectKey; }
    public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
    public Long getBoardId() { return boardId; }
    public void setBoardId(Long boardId) { this.boardId = boardId; }
    public Long getSelectedBoardId() { return selectedBoardId; }
    public void setSelectedBoardId(Long selectedBoardId) { this.selectedBoardId = selectedBoardId; }
    public JiraConnectionStatus getStatus() { return status; }
    public void setStatus(JiraConnectionStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
