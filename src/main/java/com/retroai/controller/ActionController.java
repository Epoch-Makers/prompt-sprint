package com.retroai.controller;

import com.retroai.dto.ActionDtos;
import com.retroai.enums.ActionStatus;
import com.retroai.security.CurrentUser;
import com.retroai.service.ActionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actions")
public class ActionController {

    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ActionDtos.ActionResponse> bulkCreate(@Valid @RequestBody ActionDtos.BulkCreateRequest req) {
        return actionService.bulkCreate(CurrentUser.id(), req);
    }

    /** Spec 4.5 — create an action from a NEXT_STEPS card. */
    @PostMapping("/from-card")
    @ResponseStatus(HttpStatus.CREATED)
    public ActionDtos.ActionResponse createFromCard(@Valid @RequestBody ActionDtos.FromCardRequest req) {
        return actionService.createFromCard(CurrentUser.id(), req);
    }

    @GetMapping
    public List<ActionDtos.ActionResponse> list(@RequestParam Long teamId,
                                                @RequestParam(required = false) ActionStatus status,
                                                @RequestParam(required = false) Long retroId) {
        return actionService.list(CurrentUser.id(), teamId, status, retroId);
    }

    @PatchMapping("/{actionId}")
    public ActionDtos.ActionResponse update(@PathVariable Long actionId,
                                            @RequestBody ActionDtos.UpdateActionRequest req) {
        return actionService.update(CurrentUser.id(), actionId, req);
    }

    @DeleteMapping("/{actionId}")
    public ResponseEntity<Void> delete(@PathVariable Long actionId) {
        actionService.delete(CurrentUser.id(), actionId);
        return ResponseEntity.noContent().build();
    }
}
