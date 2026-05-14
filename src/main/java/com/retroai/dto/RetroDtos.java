package com.retroai.dto;

import com.retroai.enums.ParticipationStatus;
import com.retroai.enums.RetroPhase;
import com.retroai.enums.RetroStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
        public Integer carriedOverActionCount;
        public String guestToken;

        public RetroResponse() {}
    }

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
        public String token;          // JWT for the new guest user
        public Long userId;
        public String displayName;
        public Long retroId;
        public Long teamId;
        public RetroPhase currentPhase;
        public Boolean anonymousMode;

        public GuestJoinResponse() {}
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
        public Map<String, Long> cardCounts;
        public Integer myRemainingVotes;
        public Integer carriedOverActionCount;

        public RetroDetailResponse() {}
    }

    public static class UpdateRetroRequest {
        public Boolean anonymousMode;
        public RetroStatus status;
    }

    public static class ParticipationEntry {
        public Long userId;
        public String fullName;
        public Long cardCount;
        public ParticipationStatus status;

        public ParticipationEntry() {}
        public ParticipationEntry(Long userId, String fullName, Long cardCount, ParticipationStatus status) {
            this.userId = userId; this.fullName = fullName; this.cardCount = cardCount; this.status = status;
        }
    }
}
