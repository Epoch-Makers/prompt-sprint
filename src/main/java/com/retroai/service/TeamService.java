package com.retroai.service;

import com.retroai.dto.TeamDtos;
import com.retroai.entity.Team;
import com.retroai.entity.TeamMember;
import com.retroai.entity.User;
import com.retroai.enums.TeamRole;
import com.retroai.exception.ApiException;
import com.retroai.repository.TeamMemberRepository;
import com.retroai.repository.TeamRepository;
import com.retroai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TeamDtos.TeamResponse createTeam(Long userId, TeamDtos.CreateTeamRequest req) {
        Team t = new Team();
        t.setName(req.name);
        t.setCreatedByUserId(userId);
        teamRepository.save(t);

        TeamMember member = new TeamMember();
        member.setTeamId(t.getId());
        member.setUserId(userId);
        member.setRole(TeamRole.LEADER);
        teamMemberRepository.save(member);

        return new TeamDtos.TeamResponse(t.getId(), t.getName(), t.getCreatedAt(), TeamRole.LEADER, 1L);
    }

    public List<TeamDtos.TeamResponse> listMyTeams(Long userId) {
        List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);
        List<TeamDtos.TeamResponse> out = new ArrayList<>();
        for (TeamMember m : memberships) {
            Team t = teamRepository.findById(m.getTeamId()).orElse(null);
            if (t == null) continue;
            long count = teamMemberRepository.countByTeamId(t.getId());
            out.add(new TeamDtos.TeamResponse(t.getId(), t.getName(), t.getCreatedAt(), m.getRole(), count));
        }
        return out;
    }

    public TeamDtos.TeamDetailResponse getTeamDetail(Long userId, Long teamId) {
        Team t = teamRepository.findById(teamId)
                .orElseThrow(() -> ApiException.notFound("Team not found"));
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw ApiException.forbidden("Not a team member");
        }
        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        List<TeamDtos.MemberResponse> memberDtos = new ArrayList<>();
        for (TeamMember m : members) {
            User u = userRepository.findById(m.getUserId()).orElse(null);
            if (u == null) continue;
            memberDtos.add(new TeamDtos.MemberResponse(u.getId(), u.getFullName(), u.getEmail(), m.getRole()));
        }
        return new TeamDtos.TeamDetailResponse(t.getId(), t.getName(), memberDtos);
    }

    @Transactional
    public TeamDtos.MemberResponse addMember(Long currentUserId, Long teamId, TeamDtos.AddMemberRequest req) {
        requireLeader(currentUserId, teamId);
        User user = userRepository.findByEmail(req.email)
                .orElseThrow(() -> ApiException.notFound("User not found with email " + req.email));
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId())) {
            throw ApiException.conflict("User already in team");
        }
        TeamMember m = new TeamMember();
        m.setTeamId(teamId);
        m.setUserId(user.getId());
        m.setRole(TeamRole.MEMBER);
        teamMemberRepository.save(m);
        return new TeamDtos.MemberResponse(user.getId(), user.getFullName(), user.getEmail(), TeamRole.MEMBER);
    }

    @Transactional
    public void removeMember(Long currentUserId, Long teamId, Long userId) {
        requireLeader(currentUserId, teamId);
        TeamMember m = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> ApiException.notFound("Member not found"));
        teamMemberRepository.delete(m);
    }

    public void requireMember(Long userId, Long teamId) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw ApiException.forbidden("Not a team member");
        }
    }

    public void requireLeader(Long userId, Long teamId) {
        TeamMember m = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> ApiException.forbidden("Not a team member"));
        if (m.getRole() != TeamRole.LEADER) {
            throw ApiException.forbidden("Leader role required");
        }
    }
}
