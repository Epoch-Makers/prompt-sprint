package com.retroai.service;

import com.retroai.dto.ActionDtos;
import com.retroai.entity.Action;
import com.retroai.entity.RetroSession;
import com.retroai.entity.User;
import com.retroai.enums.ActionSource;
import com.retroai.enums.ActionStatus;
import com.retroai.exception.ApiException;
import com.retroai.repository.ActionRepository;
import com.retroai.repository.RetroSessionRepository;
import com.retroai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ActionService {

    private final ActionRepository actionRepo;
    private final RetroSessionRepository retroRepo;
    private final UserRepository userRepo;
    private final TeamService teamService;

    public ActionService(ActionRepository actionRepo, RetroSessionRepository retroRepo,
                         UserRepository userRepo, TeamService teamService) {
        this.actionRepo = actionRepo;
        this.retroRepo = retroRepo;
        this.userRepo = userRepo;
        this.teamService = teamService;
    }

    @Transactional
    public List<ActionDtos.ActionResponse> bulkCreate(Long userId, ActionDtos.BulkCreateRequest req) {
        RetroSession r = retroRepo.findById(req.retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        List<ActionDtos.ActionResponse> out = new ArrayList<>();
        for (ActionDtos.ActionInputItem item : req.actions) {
            Action a = new Action();
            a.setRetroId(r.getId());
            a.setTeamId(r.getTeamId());
            a.setTitle(item.title);
            a.setDescription(item.description);
            a.setAssigneeUserId(item.assigneeUserId);
            a.setDeadline(item.deadline);
            a.setStatus(ActionStatus.OPEN);
            a.setSource(ActionSource.AI_SUGGESTED);
            actionRepo.save(a);
            out.add(toResponse(a));
        }
        return out;
    }

    public List<ActionDtos.ActionResponse> list(Long userId, Long teamId, ActionStatus status, Long retroId) {
        teamService.requireMember(userId, teamId);
        List<Action> actions;
        if (retroId != null) {
            actions = actionRepo.findByRetroIdOrderByCreatedAtDesc(retroId);
        } else if (status != null) {
            actions = actionRepo.findByTeamIdAndStatusOrderByCreatedAtDesc(teamId, status);
        } else {
            actions = actionRepo.findByTeamIdOrderByCreatedAtDesc(teamId);
        }
        List<ActionDtos.ActionResponse> out = new ArrayList<>();
        for (Action a : actions) out.add(toResponse(a));
        return out;
    }

    @Transactional
    public ActionDtos.ActionResponse update(Long userId, Long actionId, ActionDtos.UpdateActionRequest req) {
        Action a = actionRepo.findById(actionId)
                .orElseThrow(() -> ApiException.notFound("Action not found"));
        teamService.requireMember(userId, a.getTeamId());

        if (req.status != null) a.setStatus(req.status);
        if (req.title != null) a.setTitle(req.title);
        if (req.description != null) a.setDescription(req.description);
        if (req.assigneeUserId != null) a.setAssigneeUserId(req.assigneeUserId);
        if (req.deadline != null) a.setDeadline(req.deadline);
        a.setUpdatedAt(Instant.now());
        actionRepo.save(a);
        return toResponse(a);
    }

    @Transactional
    public void delete(Long userId, Long actionId) {
        Action a = actionRepo.findById(actionId)
                .orElseThrow(() -> ApiException.notFound("Action not found"));
        teamService.requireMember(userId, a.getTeamId());
        actionRepo.delete(a);
    }

    public ActionDtos.ActionResponse toResponse(Action a) {
        ActionDtos.ActionResponse out = new ActionDtos.ActionResponse();
        out.id = a.getId();
        out.retroId = a.getRetroId();
        out.title = a.getTitle();
        out.description = a.getDescription();
        out.assigneeUserId = a.getAssigneeUserId();
        if (a.getAssigneeUserId() != null) {
            User u = userRepo.findById(a.getAssigneeUserId()).orElse(null);
            out.assigneeName = u != null ? u.getFullName() : null;
        }
        out.deadline = a.getDeadline();
        out.status = a.getStatus();
        out.riskScore = a.getRiskScore();
        out.riskReason = a.getRiskReason();
        out.rewriteSuggestion = a.getRewriteSuggestion();
        out.jiraKey = a.getJiraKey();
        out.jiraUrl = a.getJiraUrl();
        out.carriedFromSprint = a.getCarriedFromSprint();
        out.createdAt = a.getCreatedAt();
        return out;
    }
}
