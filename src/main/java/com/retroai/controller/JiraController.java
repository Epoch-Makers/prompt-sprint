package com.retroai.controller;

import com.retroai.dto.JiraDtos;
import com.retroai.security.CurrentUser;
import com.retroai.service.JiraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jira")
public class JiraController {

    private final JiraService jiraService;

    public JiraController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @PostMapping("/connections")
    @ResponseStatus(HttpStatus.CREATED)
    public JiraDtos.ConnectionResponse connect(@Valid @RequestBody JiraDtos.ConnectRequest req) {
        return jiraService.connect(CurrentUser.id(), req);
    }

    @GetMapping("/connections/active")
    public JiraDtos.ConnectionResponse getActive(@RequestParam Long teamId) {
        return jiraService.getActive(CurrentUser.id(), teamId);
    }

    @DeleteMapping("/connections/{connectionId}")
    public ResponseEntity<Void> disconnect(@PathVariable Long connectionId) {
        jiraService.disconnect(CurrentUser.id(), connectionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sprint-context")
    public JiraDtos.SprintContextResponse sprintContext(@RequestParam Long retroId) {
        return jiraService.sprintContext(CurrentUser.id(), retroId);
    }

    @PostMapping("/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public JiraDtos.IssueResponse createIssue(@Valid @RequestBody JiraDtos.IssueRequest req) {
        return jiraService.createIssue(CurrentUser.id(), req.actionId);
    }

    @PostMapping("/bulk-create")
    public JiraDtos.BulkCreateResponse bulkCreate(@Valid @RequestBody JiraDtos.BulkCreateRequest req) {
        return jiraService.bulkCreate(CurrentUser.id(), req.retroId);
    }

    /**
     * Establish connection AND return the list of agile boards in the project
     * so the leader can pick which board to track.
     */
    @PostMapping("/connect")
    @ResponseStatus(HttpStatus.CREATED)
    public JiraDtos.ConnectWithBoardsResponse connectWithBoards(@Valid @RequestBody JiraDtos.ConnectRequest req) {
        return jiraService.connectWithBoards(CurrentUser.id(), req);
    }

    @PostMapping("/connect/board")
    public JiraDtos.ConnectionResponse selectBoard(@Valid @RequestBody JiraDtos.SelectBoardRequest req) {
        return jiraService.selectBoard(CurrentUser.id(), req);
    }

    @GetMapping("/boards")
    public List<JiraDtos.BoardItem> boards(@RequestParam Long teamId) {
        return jiraService.listBoards(CurrentUser.id(), teamId);
    }
}
