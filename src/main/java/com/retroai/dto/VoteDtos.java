package com.retroai.dto;

public class VoteDtos {

    public static class VoteResponse {
        public Long cardId;
        public Long voteCount;
        public Integer myRemainingVotes;

        public VoteResponse() {}
        public VoteResponse(Long cardId, Long voteCount, Integer myRemainingVotes) {
            this.cardId = cardId; this.voteCount = voteCount; this.myRemainingVotes = myRemainingVotes;
        }
    }
}
