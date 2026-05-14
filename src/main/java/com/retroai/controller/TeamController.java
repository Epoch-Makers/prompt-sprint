package com.retroai.controller;

import com.retroai.dto.TeamDtos;
import com.retroai.security.CurrentUser;
import com.retroai.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDtos.TeamResponse create(@Valid @RequestBody TeamDtos.CreateTeamRequest req) {
        return teamService.createTeam(CurrentUser.id(), req);
    }

    @GetMapping
    public List<TeamDtos.TeamResponse> listMine() {
        return teamService.listMyTeams(CurrentUser.id());
    }

    @GetMapping("/{teamId}")
    public TeamDtos.TeamDetailResponse detail(@PathVariable Long teamId) {
        return teamService.getTeamDetail(CurrentUser.id(), teamId);
    }

    @PostMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDtos.MemberResponse addMember(@PathVariable Long teamId,
                                             @Valid @RequestBody TeamDtos.AddMemberRequest req) {
        return teamService.addMember(CurrentUser.id(), teamId, req);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long teamId, @PathVariable Long userId) {
        teamService.removeMember(CurrentUser.id(), teamId, userId);
        return ResponseEntity.noContent().build();
    }
}
