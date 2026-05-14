package com.retroai.dto;

import com.retroai.enums.ParticipationStatus;
import com.retroai.enums.RetroPhase;
import com.retroai.enums.RetroStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public class RetroDtos {

    public static class CreateRetroRequest {
        @NotNull
        public Long teamId;
        @NotBlank
        public String sprintName;
        @NotBlank
        public String retroName;
        public Boolean anonymousMode;
    }

    public static class RetroResponse {
        public Long id;
        public Long teamId;
        public String sprintName;
        public String retroName;
        public RetroStatus status;
        public RetroPhase currentPhase;
        public Boolean anonymousMode;
        public Instant createdAt;
        public Long createdByUserId;
        public Integer carriedOverActionCount;
        public String guestJoinToken;
        public String guestJoinUrl;

        public RetroResponse() {}
    }

    public static class RetroSummaryResponse {
        public Long id;
        public String sprintName;
        public RetroStatus status;
        public Instant createdAt;

        public RetroSummaryResponse() {}
        public RetroSummaryResponse(Long id, String sprintName, RetroStatus status, Instant createdAt) {
            this.id = id; this.sprintName = sprintName; this.status = status; this.createdAt = createdAt;
        }
    }

    public static class RetroDetailResponse {
        public Long id;
        public Long teamId;
        public String sprintName;
        public String retroName;
        public RetroStatus status;
        public RetroPhase currentPhase;
        public Boolean anonymousMode;
        public Long createdByUserId;
        public Boolean iAmBoardOwner;
        public Map<String, Long> cardCounts;
        public Integer myRemainingVotes;
        public Integer carriedOverActionCount;
        public String guestJoinUrl;

        public RetroDetailResponse() {}
    }

    public static class UpdateRetroRequest {
        public Boolean anonymousMode;
        public RetroStatus status;
    }

    public static class ParticipationEntry {
        public Long userId;          // null for guests
        public String guestSessionId; // null for real members
        public String fullName;
        public Long cardCount;
        public ParticipationStatus status;
        public Boolean isGuest;

        public ParticipationEntry() {}
        public ParticipationEntry(Long userId, String guestSessionId, String fullName,
                                  Long cardCount, ParticipationStatus status, Boolean isGuest) {
            this.userId = userId;
            this.guestSessionId = guestSessionId;
            this.fullName = fullName;
            this.cardCount = cardCount;
            this.status = status;
            this.isGuest = isGuest;
        }
    }

    // --- Guest join (Section 3.6 / 3.7) ---

    public static class JoinLookupResponse {
        public Long retroId;
        public String sprintName;
        public String retroName;
        public String teamName;
        public RetroStatus status;
        public RetroPhase currentPhase;
        public Boolean anonymousMode;
        public Boolean tokenValid;

        public JoinLookupResponse() {}
    }

    public static class JoinPostRequest {
        @NotBlank
        public String token;
        @NotBlank
        @Size(min = 2, max = 40, message = "displayName must be 2-40 characters")
        @Pattern(regexp = "^[\\p{L}\\p{N} ._\\-]+$",
                 message = "displayName may contain letters, digits, spaces, dot, underscore, hyphen")
        public String displayName;
    }

    public static class JoinPostResponse {
        public Long retroId;
        public String guestSessionId;
        public String displayName;
        public RetroPhase currentPhase;
        public Integer myRemainingVotes;

        public JoinPostResponse() {}
    }

    // --- Phase change (Section 3.8) ---

    public static class PhaseChangeRequest {
        /** Direct phase setting — preferred per spec. */
        public RetroPhase targetPhase;
        /** Alternative shorthand: "next" / "prev". */
        public String action;
    }

    public static class PhaseChangeResponse {
        public Long id;
        public RetroPhase currentPhase;
        public RetroPhase previousPhase;
        public Instant changedAt;

        public PhaseChangeResponse() {}
        public PhaseChangeResponse(Long id, RetroPhase currentPhase,
                                   RetroPhase previousPhase, Instant changedAt) {
            this.id = id;
            this.currentPhase = currentPhase;
            this.previousPhase = previousPhase;
            this.changedAt = changedAt;
        }
    }

    // --- Legacy guest endpoints (backward compatibility) ---

    public static class JoinResponse {
        public Long retroId;
        public Long teamId;
        public String sprintName;
        public String retroName;
        public RetroStatus status;
        public RetroPhase currentPhase;
        public Boolean anonymousMode;
        public String guestToken;

        public JoinResponse() {}
    }

    public static class GuestJoinRequest {
        @NotBlank
        public String guestJoinToken;
        @NotBlank
        public String displayName;
    }

    public static class GuestJoinResponse {
        public String token;
        public Long userId;
        public String displayName;
        public Long retroId;
        public Long teamId;
        public RetroPhase currentPhase;
        public Boolean anonymousMode;

        public GuestJoinResponse() {}
    }
}
