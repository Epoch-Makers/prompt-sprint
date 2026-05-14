package com.retroai.service;

import com.retroai.dto.RetroDtos;
import com.retroai.entity.Action;
import com.retroai.entity.RetroSession;
import com.retroai.entity.TeamMember;
import com.retroai.entity.User;
import com.retroai.enums.ActionSource;
import com.retroai.enums.ActionStatus;
import com.retroai.enums.ParticipationStatus;
import com.retroai.enums.RetroColumn;
import com.retroai.enums.RetroPhase;
import com.retroai.enums.RetroStatus;
import com.retroai.enums.TeamRole;
import com.retroai.exception.ApiException;
import com.retroai.repository.ActionRepository;
import com.retroai.repository.RetroCardRepository;
import com.retroai.repository.RetroSessionRepository;
import com.retroai.repository.TeamMemberRepository;
import com.retroai.repository.UserRepository;
import com.retroai.repository.VoteRepository;
import com.retroai.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class RetroService {

    private static final int MAX_VOTES_PER_RETRO = 3;

    private final RetroSessionRepository retroRepo;
    private final RetroCardRepository cardRepo;
    private final VoteRepository voteRepo;
    private final ActionRepository actionRepo;
    private final TeamService teamService;
    private final TeamMemberRepository teamMemberRepo;
    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public RetroService(RetroSessionRepository retroRepo,
                        RetroCardRepository cardRepo,
                        VoteRepository voteRepo,
                        ActionRepository actionRepo,
                        TeamService teamService,
                        TeamMemberRepository teamMemberRepo,
                        UserRepository userRepo,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder) {
        this.retroRepo = retroRepo;
        this.cardRepo = cardRepo;
        this.voteRepo = voteRepo;
        this.actionRepo = actionRepo;
        this.teamService = teamService;
        this.teamMemberRepo = teamMemberRepo;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RetroDtos.RetroResponse createRetro(Long userId, RetroDtos.CreateRetroRequest req) {
        teamService.requireMember(userId, req.teamId);

        Optional<RetroSession> active = retroRepo.findFirstByTeamIdAndStatus(req.teamId, RetroStatus.ACTIVE);
        if (active.isPresent()) {
            throw ApiException.conflict("An active retro already exists for this team");
        }

        RetroSession r = new RetroSession();
        r.setTeamId(req.teamId);
        r.setSprintName(req.sprintName);
        r.setRetroName(req.retroName);
        r.setAnonymousMode(req.anonymousMode == null ? true : req.anonymousMode);
        r.setCreatedByUserId(userId);
        r.setStatus(RetroStatus.ACTIVE);
        r.setCurrentPhase(RetroPhase.WRITING);
        r.setGuestToken(UUID.randomUUID().toString());
        retroRepo.save(r);

        int carriedOver = carryOverActions(req.teamId, r.getId());
        RetroDtos.RetroResponse out = toRetroResponse(r);
        out.carriedOverActionCount = carriedOver;
        return out;
    }

    /**
     * Validates a guest token against the supplied retroId and returns a
     * minimal join response. Used by the public {@code /api/retros/{id}/join}
     * endpoint — no JWT required.
     */
    public RetroDtos.JoinResponse joinByToken(Long retroId, String token) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        if (r.getGuestToken() == null || !r.getGuestToken().equals(token)) {
            throw ApiException.forbidden("Invalid guest token");
        }
        RetroDtos.JoinResponse out = new RetroDtos.JoinResponse();
        out.retroId = r.getId();
        out.teamId = r.getTeamId();
        out.sprintName = r.getSprintName();
        out.retroName = r.getRetroName();
        out.status = r.getStatus();
        out.currentPhase = r.getCurrentPhase();
        out.anonymousMode = r.isAnonymousMode();
        out.guestToken = r.getGuestToken();
        return out;
    }

    /**
     * Final guest-join step: user has typed a display name. Backend creates
     * a real {@code User} for them, adds them to the retro's team as MEMBER,
     * issues a JWT, and returns retro state so the SPA can render the board.
     */
    @Transactional
    public RetroDtos.GuestJoinResponse guestJoin(RetroDtos.GuestJoinRequest req) {
        RetroSession r = retroRepo.findByGuestToken(req.guestJoinToken)
                .orElseThrow(() -> ApiException.notFound("Retro not found for token"));
        if (r.getStatus() == RetroStatus.CLOSED || r.getCurrentPhase() == RetroPhase.CLOSED) {
            throw ApiException.conflict("Retro is closed — guests cannot join");
        }
        String displayName = req.displayName == null ? "" : req.displayName.trim();
        if (displayName.isEmpty()) {
            throw ApiException.badRequest("displayName is required");
        }

        // Each guest gets a unique synthetic user — multiple people can join with
        // the same display name and remain distinct.
        String guestEmail = "guest-" + UUID.randomUUID() + "@retro.local";
        User guest = new User();
        guest.setEmail(guestEmail);
        guest.setFullName(displayName);
        guest.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        guest.setCreatedAt(Instant.now());
        userRepo.save(guest);

        TeamMember tm = new TeamMember();
        tm.setTeamId(r.getTeamId());
        tm.setUserId(guest.getId());
        tm.setRole(TeamRole.MEMBER);
        teamMemberRepo.save(tm);

        String jwt = jwtService.generateToken(guest.getId(), guest.getEmail());
        RetroDtos.GuestJoinResponse out = new RetroDtos.GuestJoinResponse();
        out.token = jwt;
        out.userId = guest.getId();
        out.displayName = displayName;
        out.retroId = r.getId();
        out.teamId = r.getTeamId();
        out.currentPhase = r.getCurrentPhase();
        out.anonymousMode = r.isAnonymousMode();
        return out;
    }

    private int carryOverActions(Long teamId, Long newRetroId) {
        List<RetroSession> prev = retroRepo.findByTeamIdAndIdNotOrderByCreatedAtDesc(teamId, newRetroId);
        if (prev.isEmpty()) return 0;
        RetroSession prevRetro = prev.get(0);
        List<Action> openActions = actionRepo.findByRetroIdAndStatusNot(prevRetro.getId(), ActionStatus.DONE);
        int count = 0;
        for (Action a : openActions) {
            Action copy = new Action();
            copy.setRetroId(newRetroId);
            copy.setTeamId(teamId);
            copy.setTitle(a.getTitle());
            copy.setDescription(a.getDescription());
            copy.setAssigneeUserId(a.getAssigneeUserId());
            copy.setDeadline(a.getDeadline());
            copy.setStatus(ActionStatus.OPEN);
            copy.setCarriedFromRetroId(prevRetro.getId());
            copy.setCarriedFromSprint(prevRetro.getSprintName());
            copy.setSource(ActionSource.AI_SUGGESTED);
            actionRepo.save(copy);
            count++;
        }
        return count;
    }

    public List<RetroDtos.RetroSummaryResponse> listByTeam(Long userId, Long teamId) {
        teamService.requireMember(userId, teamId);
        List<RetroSession> sessions = retroRepo.findByTeamIdOrderByCreatedAtDesc(teamId);
        List<RetroDtos.RetroSummaryResponse> out = new ArrayList<>();
        for (RetroSession r : sessions) {
            out.add(new RetroDtos.RetroSummaryResponse(r.getId(), r.getSprintName(), r.getStatus(), r.getCreatedAt()));
        }
        return out;
    }

    public RetroDtos.RetroDetailResponse getDetail(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        Map<String, Long> cardCounts = new LinkedHashMap<>();
        for (RetroColumn col : RetroColumn.values()) {
            cardCounts.put(col.name(), cardRepo.countByRetroIdAndColumn(retroId, col));
        }
        long used = voteRepo.countByUserIdAndRetroId(userId, retroId);
        int remaining = Math.max(0, MAX_VOTES_PER_RETRO - (int) used);

        long carriedCount = actionRepo.findByRetroIdOrderByCreatedAtDesc(retroId).stream()
                .filter(a -> a.getCarriedFromRetroId() != null).count();

        RetroDtos.RetroDetailResponse out = new RetroDtos.RetroDetailResponse();
        out.id = r.getId();
        out.teamId = r.getTeamId();
        out.sprintName = r.getSprintName();
        out.retroName = r.getRetroName();
        out.status = r.getStatus();
        out.currentPhase = r.getCurrentPhase();
        out.anonymousMode = r.isAnonymousMode();
        out.cardCounts = cardCounts;
        out.myRemainingVotes = remaining;
        out.carriedOverActionCount = (int) carriedCount;
        return out;
    }

    @Transactional
    public RetroDtos.RetroResponse updateRetro(Long userId, Long retroId, RetroDtos.UpdateRetroRequest req) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        if (req.anonymousMode != null) r.setAnonymousMode(req.anonymousMode);
        if (req.status != null) {
            r.setStatus(req.status);
            if (req.status == RetroStatus.CLOSED) {
                r.setClosedAt(Instant.now());
                r.setCurrentPhase(RetroPhase.CLOSED);
            }
        }
        retroRepo.save(r);
        return toRetroResponse(r);
    }

    /** Advance to the next workflow phase. */
    @Transactional
    public RetroDtos.RetroResponse advancePhase(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());
        RetroPhase before = r.getCurrentPhase();
        if (before == RetroPhase.CLOSED) {
            throw ApiException.conflict("Retro already closed");
        }
        RetroPhase after = before.next();
        r.setCurrentPhase(after);
        if (after == RetroPhase.CLOSED) {
            r.setStatus(RetroStatus.CLOSED);
            r.setClosedAt(Instant.now());
        }
        retroRepo.save(r);
        return toRetroResponse(r);
    }

    /** Rewind to the previous workflow phase. */
    @Transactional
    public RetroDtos.RetroResponse rewindPhase(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());
        RetroPhase before = r.getCurrentPhase();
        if (before == RetroPhase.WRITING) {
            throw ApiException.conflict("Already at the first phase");
        }
        RetroPhase after = before.prev();
        r.setCurrentPhase(after);
        // Re-opening from CLOSED → DISCUSSION reverts status to ACTIVE
        if (before == RetroPhase.CLOSED && after != RetroPhase.CLOSED) {
            r.setStatus(RetroStatus.ACTIVE);
            r.setClosedAt(null);
        }
        retroRepo.save(r);
        return toRetroResponse(r);
    }

    public List<RetroDtos.ParticipationEntry> participation(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        List<TeamMember> members = teamMemberRepo.findByTeamId(r.getTeamId());
        List<RetroDtos.ParticipationEntry> out = new ArrayList<>();
        for (TeamMember m : members) {
            User u = userRepo.findById(m.getUserId()).orElse(null);
            if (u == null) continue;
            long count = cardRepo.countByRetroIdAndAuthorUserId(retroId, u.getId());
            ParticipationStatus status =
                    count >= 2 ? ParticipationStatus.GREEN
                            : count == 1 ? ParticipationStatus.YELLOW
                            : ParticipationStatus.GREY;
            String name = r.isAnonymousMode() ? "Anonim üye" : u.getFullName();
            out.add(new RetroDtos.ParticipationEntry(u.getId(), name, count, status));
        }
        return out;
    }

    public RetroSession requireExisting(Long retroId) {
        return retroRepo.findById(retroId).orElseThrow(() -> ApiException.notFound("Retro not found"));
    }

    /**
     * Phase guard helper — used by Card, Vote, AI services to enforce that
     * an operation is only allowed in specific phase(s).
     */
    public void requirePhase(RetroSession retro, RetroPhase... allowed) {
        RetroPhase cur = retro.getCurrentPhase();
        for (RetroPhase a : allowed) {
            if (a == cur) return;
        }
        throw ApiException.conflict("Operation not allowed in phase " + cur
                + " (allowed: " + Arrays.toString(allowed) + ")");
    }

    private RetroDtos.RetroResponse toRetroResponse(RetroSession r) {
        RetroDtos.RetroResponse out = new RetroDtos.RetroResponse();
        out.id = r.getId();
        out.teamId = r.getTeamId();
        out.sprintName = r.getSprintName();
        out.retroName = r.getRetroName();
        out.status = r.getStatus();
        out.currentPhase = r.getCurrentPhase();
        out.anonymousMode = r.isAnonymousMode();
        out.createdAt = r.getCreatedAt();
        out.guestToken = r.getGuestToken();
        return out;
    }
}
