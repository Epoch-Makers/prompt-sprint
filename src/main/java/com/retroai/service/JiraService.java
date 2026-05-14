package com.retroai.service;

import com.retroai.dto.JiraDtos;
import com.retroai.entity.Action;
import com.retroai.entity.JiraConnection;
import com.retroai.entity.RetroSession;
import com.retroai.enums.ActionStatus;
import com.retroai.enums.JiraConnectionStatus;
import com.retroai.exception.ApiException;
import com.retroai.jira.JiraClient;
import com.retroai.jira.JiraTokenStore;
import com.retroai.repository.ActionRepository;
import com.retroai.repository.JiraConnectionRepository;
import com.retroai.repository.RetroSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class JiraService {

    private final JiraConnectionRepository connRepo;
    private final RetroSessionRepository retroRepo;
    private final ActionRepository actionRepo;
    private final TeamService teamService;
    private final JiraClient jiraClient;
    private final JiraTokenStore tokenStore;

    public JiraService(JiraConnectionRepository connRepo,
                       RetroSessionRepository retroRepo,
                       ActionRepository actionRepo,
                       TeamService teamService,
                       JiraClient jiraClient,
                       JiraTokenStore tokenStore) {
        this.connRepo = connRepo;
        this.retroRepo = retroRepo;
        this.actionRepo = actionRepo;
        this.teamService = teamService;
        this.jiraClient = jiraClient;
        this.tokenStore = tokenStore;
    }

    @Transactional
    public JiraDtos.ConnectionResponse connect(Long userId, JiraDtos.ConnectRequest req) {
        teamService.requireLeader(userId, req.teamId);

        boolean valid = jiraClient.verifyCredentials(req.jiraDomain, req.email, req.apiToken);
        if (!valid) {
            throw ApiException.unauthorized("Jira credentials invalid (/myself returned 401)");
        }
        JiraConnection existing = connRepo.findByTeamId(req.teamId).orElse(null);
        JiraConnection c = existing == null ? new JiraConnection() : existing;
        c.setTeamId(req.teamId);
        c.setCreatedByUserId(userId);
        c.setEmail(req.email);
        c.setJiraDomain(req.jiraDomain);
        c.setProjectKey(req.projectKey);
        c.setBoardId(req.boardId);
        c.setStatus(JiraConnectionStatus.CONNECTED);
        connRepo.save(c);
        tokenStore.put(c.getId(), req.apiToken);
        return toResponse(c);
    }

    public JiraDtos.ConnectionResponse getActive(Long userId, Long teamId) {
        teamService.requireMember(userId, teamId);
        JiraConnection c = connRepo.findByTeamId(teamId)
                .orElseThrow(() -> ApiException.notFound("No Jira connection for team"));
        return toResponse(c);
    }

    @Transactional
    public void disconnect(Long userId, Long connectionId) {
        JiraConnection c = connRepo.findById(connectionId)
                .orElseThrow(() -> ApiException.notFound("Connection not found"));
        teamService.requireLeader(userId, c.getTeamId());
        connRepo.delete(c);
        tokenStore.remove(connectionId);
    }

    public JiraDtos.SprintContextResponse sprintContext(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());
        Optional<JiraConnection> connOpt = connRepo.findByTeamId(r.getTeamId());
        if (connOpt.isEmpty()) {
            return mockSprintContext(r.getSprintName());
        }
        // Real Jira call would go here. For demo: return mock with mock=false
        // when a connection exists (would need full board/sprint API integration).
        JiraDtos.SprintContextResponse out = mockSprintContext(r.getSprintName());
        out.mock = false;
        return out;
    }

    private JiraDtos.SprintContextResponse mockSprintContext(String sprintName) {
        JiraDtos.SprintContextResponse out = new JiraDtos.SprintContextResponse();
        out.sprintName = sprintName;
        out.plannedStories = 12;
        out.doneStories = 8;
        out.openBugs = 2;
        out.velocityPct = 70;
        out.topBugService = "payment-service";
        out.mock = true;
        return out;
    }

    @Transactional
    public JiraDtos.IssueResponse createIssue(Long userId, Long actionId) {
        Action a = actionRepo.findById(actionId)
                .orElseThrow(() -> ApiException.notFound("Action not found"));
        teamService.requireMember(userId, a.getTeamId());
        if (a.getJiraKey() != null && !a.getJiraKey().isBlank()) {
            throw ApiException.conflict("Action already has Jira ticket: " + a.getJiraKey());
        }
        JiraConnection conn = connRepo.findByTeamId(a.getTeamId()).orElse(null);
        String key;
        String url;
        if (conn == null || tokenStore.get(conn.getId()) == null) {
            // mock fallback
            key = (conn != null ? conn.getProjectKey() : "MOCK") + "-" + (1000 + a.getId());
            url = "https://example.atlassian.net/browse/" + key;
        } else {
            RetroSession r = retroRepo.findById(a.getRetroId()).orElseThrow();
            String labelSprint = "retro-sprint-" + r.getSprintName().replaceAll("\\s+", "");
            String created = jiraClient.createIssue(conn.getJiraDomain(), conn.getEmail(),
                    tokenStore.get(conn.getId()), conn.getProjectKey(),
                    a.getTitle(), a.getDescription(), null,
                    List.of("retro-action", labelSprint));
            if (created == null) {
                key = conn.getProjectKey() + "-MOCK-" + a.getId();
                url = "https://" + conn.getJiraDomain() + "/browse/" + key;
            } else {
                key = created;
                url = "https://" + conn.getJiraDomain() + "/browse/" + key;
            }
        }
        a.setJiraKey(key);
        a.setJiraUrl(url);
        actionRepo.save(a);
        return new JiraDtos.IssueResponse(a.getId(), key, url);
    }

    public JiraDtos.BulkCreateResponse bulkCreate(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        List<Action> openActions = actionRepo.findByRetroIdAndStatus(retroId, ActionStatus.OPEN);
        List<CompletableFuture<JiraDtos.BulkResultItem>> futures = new ArrayList<>();
        for (Action a : openActions) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    if (a.getJiraKey() != null) {
                        return new JiraDtos.BulkResultItem(a.getId(), a.getJiraKey(), "SKIPPED", "already linked");
                    }
                    JiraDtos.IssueResponse iss = createIssue(userId, a.getId());
                    return new JiraDtos.BulkResultItem(a.getId(), iss.jiraKey, "SUCCESS", null);
                } catch (Exception e) {
                    return new JiraDtos.BulkResultItem(a.getId(), null, "FAILED", e.getMessage());
                }
            }));
        }

        List<JiraDtos.BulkResultItem> results = new ArrayList<>();
        int success = 0;
        int failed = 0;
        for (CompletableFuture<JiraDtos.BulkResultItem> f : futures) {
            try {
                JiraDtos.BulkResultItem item = f.get();
                results.add(item);
                if ("SUCCESS".equals(item.status)) success++;
                else if ("FAILED".equals(item.status)) failed++;
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
            }
        }
        boolean mock = connRepo.findByTeamId(r.getTeamId()).map(c -> tokenStore.get(c.getId()) == null).orElse(true);

        JiraDtos.BulkCreateResponse out = new JiraDtos.BulkCreateResponse();
        out.results = results;
        out.successCount = success;
        out.failedCount = failed;
        out.mock = mock;
        return out;
    }

    private JiraDtos.ConnectionResponse toResponse(JiraConnection c) {
        JiraDtos.ConnectionResponse out = new JiraDtos.ConnectionResponse();
        out.id = c.getId();
        out.teamId = c.getTeamId();
        out.email = c.getEmail();
        out.jiraDomain = c.getJiraDomain();
        out.projectKey = c.getProjectKey();
        out.boardId = c.getBoardId();
        out.selectedBoardId = c.getSelectedBoardId();
        out.status = c.getStatus();
        return out;
    }

    /**
     * Same as {@link #connect(Long, JiraDtos.ConnectRequest)} but also returns
     * the list of agile boards available in the project so the leader can
     * pick which one to track.
     */
    @Transactional
    public JiraDtos.ConnectWithBoardsResponse connectWithBoards(Long userId, JiraDtos.ConnectRequest req) {
        JiraDtos.ConnectionResponse conn = connect(userId, req);
        List<JiraDtos.BoardItem> boards = fetchBoards(req.jiraDomain, req.email, req.apiToken, req.projectKey);
        return new JiraDtos.ConnectWithBoardsResponse(conn, boards);
    }

    public List<JiraDtos.BoardItem> listBoards(Long userId, Long teamId) {
        teamService.requireMember(userId, teamId);
        JiraConnection conn = connRepo.findByTeamId(teamId)
                .orElseThrow(() -> ApiException.notFound("No Jira connection for team"));
        String token = tokenStore.get(conn.getId());
        return fetchBoards(conn.getJiraDomain(), conn.getEmail(), token, conn.getProjectKey());
    }

    @Transactional
    public JiraDtos.ConnectionResponse selectBoard(Long userId, JiraDtos.SelectBoardRequest req) {
        teamService.requireLeader(userId, req.teamId);
        JiraConnection conn = connRepo.findByTeamId(req.teamId)
                .orElseThrow(() -> ApiException.notFound("No Jira connection for team"));
        conn.setSelectedBoardId(req.boardId);
        connRepo.save(conn);
        return toResponse(conn);
    }

    private List<JiraDtos.BoardItem> fetchBoards(String domain, String email, String apiToken, String projectKey) {
        List<JiraDtos.BoardItem> out = new ArrayList<>();
        if (apiToken == null || apiToken.isBlank()) {
            return mockBoards(projectKey);
        }
        String url = "https://" + domain + "/rest/agile/1.0/board?projectKeyOrId=" + projectKey;
        JsonNode resp = jiraClient.get(url, email, apiToken);
        if (resp == null || !resp.has("values")) {
            return mockBoards(projectKey);
        }
        for (JsonNode b : resp.path("values")) {
            out.add(new JiraDtos.BoardItem(
                    b.path("id").asLong(),
                    b.path("name").asText(""),
                    b.path("type").asText("scrum")));
        }
        return out;
    }

    private List<JiraDtos.BoardItem> mockBoards(String projectKey) {
        List<JiraDtos.BoardItem> out = new ArrayList<>();
        out.add(new JiraDtos.BoardItem(101L, projectKey + " Sprint Board", "scrum"));
        out.add(new JiraDtos.BoardItem(102L, projectKey + " Bug Triage", "kanban"));
        return out;
    }
}
