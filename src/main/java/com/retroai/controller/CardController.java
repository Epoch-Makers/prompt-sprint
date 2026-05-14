package com.retroai.controller;

import com.retroai.dto.CardDtos;
import com.retroai.dto.VoteDtos;
import com.retroai.security.CurrentUser;
import com.retroai.service.CardService;
import com.retroai.service.VoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;
    private final VoteService voteService;

    public CardController(CardService cardService, VoteService voteService) {
        this.cardService = cardService;
        this.voteService = voteService;
    }

    @PatchMapping("/{cardId}")
    public CardDtos.CardResponse update(@PathVariable Long cardId,
                                        @RequestBody CardDtos.UpdateCardRequest req) {
        return cardService.update(CurrentUser.id(), cardId, req);
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> delete(@PathVariable Long cardId) {
        cardService.delete(CurrentUser.id(), cardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{cardId}/vote")
    @ResponseStatus(HttpStatus.CREATED)
    public VoteDtos.VoteResponse vote(@PathVariable Long cardId) {
        return voteService.castVote(CurrentUser.id(), cardId);
    }

    @DeleteMapping("/{cardId}/vote")
    public VoteDtos.VoteResponse unvote(@PathVariable Long cardId) {
        return voteService.retractVote(CurrentUser.id(), cardId);
    }
}
