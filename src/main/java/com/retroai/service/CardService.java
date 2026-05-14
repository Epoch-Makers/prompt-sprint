package com.retroai.service;

import com.retroai.dto.CardDtos;
import com.retroai.entity.RetroCard;
import com.retroai.entity.RetroSession;
import com.retroai.entity.User;
import com.retroai.enums.CardSource;
import com.retroai.enums.RetroPhase;
import com.retroai.exception.ApiException;
import com.retroai.repository.RetroCardRepository;
import com.retroai.repository.RetroSessionRepository;
import com.retroai.repository.UserRepository;
import com.retroai.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CardService {

    private final RetroCardRepository cardRepo;
    private final RetroSessionRepository retroRepo;
    private final UserRepository userRepo;
    private final VoteRepository voteRepo;
    private final TeamService teamService;
    private final RetroService retroService;

    public CardService(RetroCardRepository cardRepo, RetroSessionRepository retroRepo,
                       UserRepository userRepo, VoteRepository voteRepo,
                       TeamService teamService, RetroService retroService) {
        this.cardRepo = cardRepo;
        this.retroRepo = retroRepo;
        this.userRepo = userRepo;
        this.voteRepo = voteRepo;
        this.teamService = teamService;
        this.retroService = retroService;
    }

    @Transactional
    public CardDtos.CardResponse create(Long userId, Long retroId, CardDtos.CreateCardRequest req) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());
        retroService.requirePhase(r, RetroPhase.WRITING);

        RetroCard c = new RetroCard();
        c.setRetroId(retroId);
        c.setAuthorUserId(userId);
        c.setContent(req.content);
        c.setColumn(req.column);
        c.setSource(req.source == null ? CardSource.USER : req.source);
        cardRepo.save(c);
        return toResponse(c, r.isAnonymousMode(), userId);
    }

    public List<CardDtos.CardResponse> list(Long userId, Long retroId) {
        RetroSession r = retroRepo.findById(retroId)
                .orElseThrow(() -> ApiException.notFound("Retro not found"));
        teamService.requireMember(userId, r.getTeamId());

        List<RetroCard> cards = cardRepo.findByRetroIdOrderByCreatedAtAsc(retroId);
        List<CardDtos.CardResponse> out = new ArrayList<>();
        for (RetroCard c : cards) {
            out.add(toResponse(c, r.isAnonymousMode(), userId));
        }
        return out;
    }

    @Transactional
    public CardDtos.CardResponse update(Long userId, Long cardId, CardDtos.UpdateCardRequest req) {
        RetroCard c = cardRepo.findById(cardId)
                .orElseThrow(() -> ApiException.notFound("Card not found"));
        if (!c.getAuthorUserId().equals(userId)) {
            throw ApiException.forbidden("Only card owner can update");
        }
        RetroSession r = retroRepo.findById(c.getRetroId()).orElseThrow();
        // GROUPING phase: board owner can move cards between columns even if
        // not the author. WRITING phase: full edit by author.
        retroService.requirePhase(r, RetroPhase.WRITING, RetroPhase.GROUPING);

        if (req.content != null) c.setContent(req.content);
        if (req.column != null) c.setColumn(req.column);
        c.setUpdatedAt(Instant.now());
        cardRepo.save(c);

        return toResponse(c, r.isAnonymousMode(), userId);
    }

    @Transactional
    public void delete(Long userId, Long cardId) {
        RetroCard c = cardRepo.findById(cardId)
                .orElseThrow(() -> ApiException.notFound("Card not found"));
        if (!c.getAuthorUserId().equals(userId)) {
            throw ApiException.forbidden("Only card owner can delete");
        }
        RetroSession r = retroRepo.findById(c.getRetroId()).orElseThrow();
        retroService.requirePhase(r, RetroPhase.WRITING);
        cardRepo.delete(c);
    }

    private CardDtos.CardResponse toResponse(RetroCard c, boolean anonymous, Long currentUserId) {
        CardDtos.CardResponse out = new CardDtos.CardResponse();
        out.id = c.getId();
        out.retroId = c.getRetroId();
        out.content = c.getContent();
        out.column = c.getColumn();
        out.source = c.getSource();
        out.createdAt = c.getCreatedAt();

        User author = userRepo.findById(c.getAuthorUserId()).orElse(null);
        boolean isGuestAuthor = author != null
                && author.getAuthProvider() == com.retroai.enums.AuthProvider.GUEST;
        if (isGuestAuthor) {
            out.authorId = null;
            out.guestSessionId = author.getGuestSessionId();
        } else {
            out.authorId = c.getAuthorUserId();
            out.guestSessionId = null;
        }

        if (anonymous) {
            out.authorName = "Anonim üye";
        } else {
            out.authorName = author != null ? author.getFullName() : "Bilinmeyen";
        }
        out.voteCount = voteRepo.countByCardId(c.getId());
        out.myVoted = voteRepo.existsByCardIdAndUserId(c.getId(), currentUserId);
        return out;
    }
}
