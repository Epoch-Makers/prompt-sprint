package com.retroai.dto;

import com.retroai.enums.JiraConnectionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class JiraDtos {

    public static class ConnectRequest {
        @NotNull
        public Long teamId;
        @NotBlank
        public String email;
        @NotBlank
        public String apiToken;
        @NotBlank
        public String jiraDomain;
        @NotBlank
        public String projectKey;
        @NotNull
        public Long boardId;
    }

    public static class ConnectionResponse {
        public Long id;
        public Long teamId;
        public String email;
        public String jiraDomain;
        public String projectKey;
        public Long boardId;
        public Long selectedBoardId;
        public JiraConnectionStatus status;

        public ConnectionResponse() {}
    }

    public static class BoardItem {
        public Long id;
        public String name;
        public String type;

        public BoardItem() {}
        public BoardItem(Long id, String name, String type) {
            this.id = id; this.name = name; this.type = type;
        }
    }

    public static class ConnectWithBoardsResponse {
        public ConnectionResponse connection;
        public List<BoardItem> boards;

        public ConnectWithBoardsResponse() {}
        public ConnectWithBoardsResponse(ConnectionResponse connection, List<BoardItem> boards) {
            this.connection = connection; this.boards = boards;
        }
    }

    public static class SelectBoardRequest {
        @NotNull
        public Long teamId;
        @NotNull
        public Long boardId;
    }

    public static class SprintContextResponse {
        public String sprintName;
        public Integer plannedStories;
        public Integer doneStories;
        public Integer openBugs;
        public Integer velocityPct;
        public String topBugService;
        public Boolean mock;

        public SprintContextResponse() {}
    }

    public static class IssueRequest {
        @NotNull
        public Long actionId;
    }

    public static class IssueResponse {
        public Long actionId;
        public String jiraKey;
        public String jiraUrl;

        public IssueResponse() {}
        public IssueResponse(Long actionId, String jiraKey, String jiraUrl) {
            this.actionId = actionId; this.jiraKey = jiraKey; this.jiraUrl = jiraUrl;
        }
    }

    public static class BulkCreateRequest {
        @NotNull
        public Long retroId;
    }

    public static class BulkResultItem {
        public Long actionId;
        public String jiraKey;
        public String status;
        public String error;

        public BulkResultItem() {}
        public BulkResultItem(Long actionId, String jiraKey, String status, String error) {
            this.actionId = actionId; this.jiraKey = jiraKey; this.status = status; this.error = error;
        }
    }

    public static class BulkCreateResponse {
        public List<BulkResultItem> results;
        public Integer successCount;
        public Integer failedCount;
        public Boolean mock;

        public BulkCreateResponse() {}
    }
}
