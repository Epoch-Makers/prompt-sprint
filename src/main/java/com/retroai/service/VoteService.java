package com.retroai.service;

import com.retroai.dto.VoteDtos;
import com.retroai.entity.RetroCard;
import com.retroai.entity.RetroSession;
import com.retroai.entity.Vote;
import com.retroai.enums.RetroPhase;
import com.retroai.exception.ApiException;
import com.retroai.repository.RetroCardRepository;
import com.retroai.repository.RetroSessionRepository;
import com.retroai.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class VoteService {

    private static final int MAX_VOTES = 3;

    private final VoteRepository voteRepo;
    private final RetroCardRepository cardRepo;
    private final RetroSessionRepository retroRepo;
    private final TeamService teamService;
    private final RetroService retroService;

    public VoteService(VoteRepository voteRepo, RetroCardRepository cardRepo,
                       RetroSessionRepository retroRepo, TeamService teamService,
                       RetroService retroService) {
        this.voteRepo = voteRepo;
        this.cardRepo = cardRepo;
        this.retroRepo = retroRepo;
        this.teamService = teamService;
        this.retroService = retroService;
    }

    @Transactional
    public VoteDtos.VoteResponse castVote(Long userId, Long cardId) {
        RetroCard card = cardRepo.findById(cardId)
                .orElseThrow(() -> ApiException.notFound("Card not found"));
        RetroSession retro = retroRepo.findById(card.getRetroId()).orElseThrow();
        teamService.requireMember(userId, retro.getTeamId());
        retroService.requirePhase(retro, RetroPhase.VOTING);

        if (voteRepo.existsByCardIdAndUserId(cardId, userId)) {
            throw ApiException.conflict("Already voted this card");
        }
        long used = voteRepo.countByUserIdAndRetroId(userId, card.getRetroId());
        if (used >= MAX_VOTES) {
            throw ApiException.badRequest("NO_VOTES_LEFT");
        }
        Vote v = new Vote();
        v.setCardId(cardId);
        v.setUserId(userId);
        v.setRetroId(card.getRetroId());
        voteRepo.save(v);

        long voteCount = voteRepo.countByCardId(cardId);
        int remaining = MAX_VOTES - (int) (used + 1);
        return new VoteDtos.VoteResponse(cardId, voteCount, remaining);
    }

    @Transactional
    public VoteDtos.VoteResponse retractVote(Long userId, Long cardId) {
        RetroCard card = cardRepo.findById(cardId)
                .orElseThrow(() -> ApiException.notFound("Card not found"));
        RetroSession retro = retroRepo.findById(card.getRetroId()).orElseThrow();
        retroService.requirePhase(retro, RetroPhase.VOTING);

        Optional<Vote> v = voteRepo.findByCardIdAndUserId(cardId, userId);
        if (v.isEmpty()) {
            throw ApiException.notFound("Vote not found");
        }
        voteRepo.delete(v.get());

        long voteCount = voteRepo.countByCardId(cardId);
        long used = voteRepo.countByUserIdAndRetroId(userId, card.getRetroId());
        int remaining = MAX_VOTES - (int) used;
        return new VoteDtos.VoteResponse(cardId, voteCount, remaining);
    }
}
