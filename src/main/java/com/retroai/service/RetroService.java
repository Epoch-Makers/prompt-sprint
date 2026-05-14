package com.retroai.service;

import com.retroai.dto.RetroDtos;
import com.retroai.entity.Action;
import com.retroai.entity.RetroSession;
import com.retroai.entity.Team;
import com.retroai.entity.TeamMember;
import com.retroai.entity.User;
import com.retroai.enums.ActionSource;
import com.retroai.enums.ActionStatus;
import com.retroai.enums.AuthProvider;
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
import com.retroai.repository.TeamRepository;
import com.retroai.repository.UserRepository;
import com.retroai.repository.VoteRepository;
import com.retroai.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
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
    private final TeamRepository teamRepo;
    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final String frontendBaseUrl;

    public RetroService(RetroSessionRepository retroRepo,
                        RetroCardRepository cardRepo,
                        VoteRepository voteRepo,
                        ActionRepository actionRepo,
                        TeamService teamService,
                        TeamMemberRepository teamMemberRepo,
                        TeamRepository teamRepo,
                        UserRepository userRepo,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.frontend.base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.retroRepo = retroRepo;
        this.cardRepo = cardRepo;
        this.voteRepo = voteRepo;
        this.actionRepo = actionRepo;
        this.teamService = teamService;
        this.teamMemberRepo = teamMemberRepo;
        this.teamRepo = teamRepo;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.frontendBaseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
    }

    // ---------------------------------------------------------------------
    // Create / list / detail / update
    // ---------------------------------------------------------------------

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
        out.createdByUserId = r.getCreatedByUserId();
        out.iAmBoardOwner = r.getCreatedByUserId() != null && r.getCreatedByUserId().equals(userId);
        out.cardCounts = cardCounts;
        out.myRemainingVotes = remaining;
        out.carriedOverActionCount = (int) carriedCount;
        out.guestJoinUrl = buildGuestJoinUrl(r.getGuestToken());
        return out;
    }

    @Transactional
    public RetroDtos.RetroResponse updateRetro(Long userId, Long retroId, RetroDtos.UpdateRetroRequest req) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        requireBoardOwner(userId, r);

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

    // ---------------------------------------------------------------------
    // Phase change (Section 3.8)
    // ---------------------------------------------------------------------

    /**
     * Unified phase change endpoint supporting both spec-style {@code targetPhase}
     * and shorthand {@code action: "next"|"prev"}. Board-owner only.
     */
    @Transactional
    public RetroDtos.PhaseChangeResponse changePhase(Long userId, Long retroId, RetroDtos.PhaseChangeRequest req) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        requireBoardOwner(userId, r);

        RetroPhase before = r.getCurrentPhase();
        RetroPhase target = resolveTargetPhase(before, req);

        if (target == RetroPhase.CLOSED) {
            throw ApiException.badRequest("Cannot transition directly to CLOSED. Use PATCH /api/retros/{id} status=CLOSED");
        }

        r.setCurrentPhase(target);
        // Reopen path: CLOSED → DISCUSSION should restore ACTIVE
        if (before == RetroPhase.CLOSED && target != RetroPhase.CLOSED) {
            r.setStatus(RetroStatus.ACTIVE);
            r.setClosedAt(null);
        }
        retroRepo.save(r);
        return new RetroDtos.PhaseChangeResponse(r.getId(), r.getCurrentPhase(), before, Instant.now());
    }

    private RetroPhase resolveTargetPhase(RetroPhase current, RetroDtos.PhaseChangeRequest req) {
        if (req.targetPhase != null) {
            return req.targetPhase;
        }
        if (req.action != null) {
            String a = req.action.trim().toLowerCase();
            if ("next".equals(a)) {
                if (current == RetroPhase.CLOSED) {
                    throw ApiException.conflict("Retro is closed");
                }
                return current.next();
            }
            if ("prev".equals(a) || "previous".equals(a) || "back".equals(a)) {
                if (current == RetroPhase.WRITING) {
                    throw ApiException.conflict("Already at the first phase");
                }
                return current.prev();
            }
        }
        throw ApiException.badRequest("Provide either targetPhase or action (\"next\"/\"prev\")");
    }

    // Legacy wrappers — keep /phase/next and /phase/prev endpoints working

    @Transactional
    public RetroDtos.RetroResponse advancePhase(Long userId, Long retroId) {
        RetroDtos.PhaseChangeRequest req = new RetroDtos.PhaseChangeRequest();
        req.action = "next";
        changePhase(userId, retroId, req);
        return toRetroResponse(retroRepo.findById(retroId).orElseThrow());
    }

    @Transactional
    public RetroDtos.RetroResponse rewindPhase(Long userId, Long retroId) {
        RetroDtos.PhaseChangeRequest req = new RetroDtos.PhaseChangeRequest();
        req.action = "prev";
        changePhase(userId, retroId, req);
        return toRetroResponse(retroRepo.findById(retroId).orElseThrow());
    }

    // ---------------------------------------------------------------------
    // Guest join (Sections 3.6 / 3.7)
    // ---------------------------------------------------------------------

    /** Public — no auth. Validates a guest-join token and returns retro summary. */
    public RetroDtos.JoinLookupResponse joinLookup(Long retroId, String token) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        if (r.getGuestToken() == null || !r.getGuestToken().equals(token)) {
            throw ApiException.notFound("Invalid guest token");
        }
        if (r.getStatus() == RetroStatus.CLOSED || r.getCurrentPhase() == RetroPhase.CLOSED) {
            throw ApiException.gone("Retro is closed");
        }
        Team t = teamRepo.findById(r.getTeamId()).orElse(null);
        RetroDtos.JoinLookupResponse out = new RetroDtos.JoinLookupResponse();
        out.retroId = r.getId();
        out.sprintName = r.getSprintName();
        out.retroName = r.getRetroName();
        out.teamName = t != null ? t.getName() : null;
        out.status = r.getStatus();
        out.currentPhase = r.getCurrentPhase();
        out.anonymousMode = r.isAnonymousMode();
        out.tokenValid = true;
        return out;
    }

    /** Public — no auth. Creates a guest User with guestSessionId, returns session info. */
    @Transactional
    public RetroDtos.JoinPostResponse joinAsGuest(Long retroId, RetroDtos.JoinPostRequest req) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        if (r.getGuestToken() == null || !r.getGuestToken().equals(req.token)) {
            throw ApiException.notFound("Invalid guest token");
        }
        if (r.getStatus() == RetroStatus.CLOSED || r.getCurrentPhase() == RetroPhase.CLOSED) {
            throw ApiException.gone("Retro is closed");
        }

        String displayName = req.displayName.trim();

        // Uniqueness check inside this retro — find guests already in this team
        // whose retro matches and whose fullName matches.
        List<TeamMember> existingMembers = teamMemberRepo.findByTeamId(r.getTeamId());
        for (TeamMember m : existingMembers) {
            User u = userRepo.findById(m.getUserId()).orElse(null);
            if (u == null) continue;
            if (u.getAuthProvider() == AuthProvider.GUEST
                    && retroId.equals(u.getGuestRetroId())
                    && displayName.equalsIgnoreCase(u.getFullName())) {
                throw new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                        "DISPLAY_NAME_TAKEN",
                        "displayName already taken in this retro");
            }
        }

        String guestSessionId = UUID.randomUUID().toString();
        User guest = new User();
        guest.setEmail("guest-" + guestSessionId + "@retro.local");
        guest.setFullName(displayName);
        guest.setPasswordHash(null);
        guest.setAuthProvider(AuthProvider.GUEST);
        guest.setGuestSessionId(guestSessionId);
        guest.setGuestRetroId(retroId);
        guest.setCreatedAt(Instant.now());
        userRepo.save(guest);

        TeamMember tm = new TeamMember();
        tm.setTeamId(r.getTeamId());
        tm.setUserId(guest.getId());
        tm.setRole(TeamRole.MEMBER);
        teamMemberRepo.save(tm);

        long used = voteRepo.countByUserIdAndRetroId(guest.getId(), retroId);
        int remaining = Math.max(0, MAX_VOTES_PER_RETRO - (int) used);

        RetroDtos.JoinPostResponse out = new RetroDtos.JoinPostResponse();
        out.retroId = r.getId();
        out.guestSessionId = guestSessionId;
        out.displayName = displayName;
        out.currentPhase = r.getCurrentPhase();
        out.myRemainingVotes = remaining;
        return out;
    }

    // Legacy /join (GET) — keep working
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

    // Legacy /guest-join (POST) — keep working
    @Transactional
    public RetroDtos.GuestJoinResponse guestJoin(RetroDtos.GuestJoinRequest req) {
        RetroSession r = retroRepo.findByGuestToken(req.guestJoinToken)
                .orElseThrow(() -> ApiException.notFound("Retro not found for token"));
        if (r.getStatus() == RetroStatus.CLOSED || r.getCurrentPhase() == RetroPhase.CLOSED) {
            throw ApiException.gone("Retro is closed — guests cannot join");
        }
        String displayName = req.displayName == null ? "" : req.displayName.trim();
        if (displayName.isEmpty()) {
            throw ApiException.badRequest("displayName is required");
        }

        String guestSessionId = UUID.randomUUID().toString();
        User guest = new User();
        guest.setEmail("guest-" + guestSessionId + "@retro.local");
        guest.setFullName(displayName);
        guest.setPasswordHash(null);
        guest.setAuthProvider(AuthProvider.GUEST);
        guest.setGuestSessionId(guestSessionId);
        guest.setGuestRetroId(r.getId());
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

    // ---------------------------------------------------------------------
    // Participation
    // ---------------------------------------------------------------------

    public List<RetroDtos.ParticipationEntry> participation(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        List<TeamMember> members = teamMemberRepo.findByTeamId(r.getTeamId());
        List<RetroDtos.ParticipationEntry> out = new ArrayList<>();
        for (TeamMember m : members) {
            User u = userRepo.findById(m.getUserId()).orElse(null);
            if (u == null) continue;

            // Hide guests from OTHER retros — they live on User table but belong
            // to a single retro
            boolean isGuest = u.getAuthProvider() == AuthProvider.GUEST;
            if (isGuest && (u.getGuestRetroId() == null || !u.getGuestRetroId().equals(retroId))) {
                continue;
            }

            long count = cardRepo.countByRetroIdAndAuthorUserId(retroId, u.getId());
            ParticipationStatus status =
                    count >= 2 ? ParticipationStatus.GREEN
                            : count == 1 ? ParticipationStatus.YELLOW
                            : ParticipationStatus.GREY;

            String displayName;
            if (isGuest) {
                displayName = u.getFullName();
            } else if (r.isAnonymousMode()) {
                displayName = "Anonim üye";
            } else {
                displayName = u.getFullName();
            }
            out.add(new RetroDtos.ParticipationEntry(
                    isGuest ? null : u.getId(),
                    isGuest ? u.getGuestSessionId() : null,
                    displayName,
                    count,
                    status,
                    isGuest));
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

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

    public RetroSession requireExisting(Long retroId) {
        return retroRepo.findById(retroId).orElseThrow(() -> ApiException.notFound("Retro not found"));
    }

    /** Phase guard helper. Throws 423 PHASE_LOCKED if the retro is in a disallowed phase. */
    public void requirePhase(RetroSession retro, RetroPhase... allowed) {
        RetroPhase cur = retro.getCurrentPhase();
        for (RetroPhase a : allowed) {
            if (a == cur) return;
        }
        throw ApiException.phaseLocked("Operation not allowed in phase " + cur
                + " (allowed: " + Arrays.toString(allowed) + ")");
    }

    /** Board-owner gate. */
    public void requireBoardOwner(Long callerUserId, RetroSession retro) {
        if (retro.getCreatedByUserId() == null || !retro.getCreatedByUserId().equals(callerUserId)) {
            throw ApiException.forbidden("Only the board owner can perform this action");
        }
    }

    private String buildGuestJoinUrl(String guestToken) {
        if (guestToken == null) return null;
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/join/" + guestToken;
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
        out.createdByUserId = r.getCreatedByUserId();
        out.guestJoinToken = r.getGuestToken();
        out.guestJoinUrl = buildGuestJoinUrl(r.getGuestToken());
        return out;
    }
}
