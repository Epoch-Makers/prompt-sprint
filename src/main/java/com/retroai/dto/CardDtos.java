package com.retroai.dto;

import com.retroai.enums.CardSource;
import com.retroai.enums.RetroColumn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class CardDtos {

    public static class CreateCardRequest {
        @NotBlank
        public String content;
        @NotNull
        public RetroColumn column;
        public CardSource source;
    }

    public static class UpdateCardRequest {
        public String content;
        public RetroColumn column;
    }

    public static class CardResponse {
        public Long id;
        public Long retroId;
        public String content;
        public RetroColumn column;
        public Long authorId;            // null if author was a guest
        public String guestSessionId;    // populated if authored by guest
        public String authorName;
        public CardSource source;
        public Long voteCount;
        public Boolean myVoted;
        public Instant createdAt;

        public CardResponse() {}
    }
}
