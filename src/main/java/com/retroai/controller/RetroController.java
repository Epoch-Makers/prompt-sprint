package com.retroai.controller;

import com.retroai.dto.CardDtos;
import com.retroai.dto.RetroDtos;
import com.retroai.security.CurrentUser;
import com.retroai.service.CardService;
import com.retroai.service.RetroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retros")
public class RetroController {

    private final RetroService retroService;
    private final CardService cardService;

    public RetroController(RetroService retroService, CardService cardService) {
        this.retroService = retroService;
        this.cardService = cardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RetroDtos.RetroResponse create(@Valid @RequestBody RetroDtos.CreateRetroRequest req) {
        return retroService.createRetro(CurrentUser.id(), req);
    }

    @GetMapping
    public List<RetroDtos.RetroSummaryResponse> listByTeam(@RequestParam Long teamId) {
        return retroService.listByTeam(CurrentUser.id(), teamId);
    }

    @GetMapping("/{retroId}")
    public RetroDtos.RetroDetailResponse detail(@PathVariable Long retroId) {
        return retroService.getDetail(CurrentUser.id(), retroId);
    }

    @PatchMapping("/{retroId}")
    public RetroDtos.RetroResponse update(@PathVariable Long retroId,
                                          @RequestBody RetroDtos.UpdateRetroRequest req) {
        return retroService.updateRetro(CurrentUser.id(), retroId, req);
    }

    @GetMapping("/{retroId}/participation")
    public List<RetroDtos.ParticipationEntry> participation(@PathVariable Long retroId) {
        return retroService.participation(CurrentUser.id(), retroId);
    }

    /**
     * Public guest join endpoint. No JWT required — accepts the guest token
     * issued at retro creation. Frontend stores the returned token and sends
     * it back as {@code X-Guest-Token} on subsequent card/vote calls.
     */
    @GetMapping("/{retroId}/join")
    public RetroDtos.JoinResponse join(@PathVariable Long retroId, @RequestParam("token") String token) {
        return retroService.joinByToken(retroId, token);
    }

    /**
     * Final guest-join step: guest types a display name. Returns a JWT and
     * minimal retro state so the SPA can render the board immediately.
     */
    @PostMapping("/guest-join")
    public RetroDtos.GuestJoinResponse guestJoin(@Valid @RequestBody RetroDtos.GuestJoinRequest req) {
        return retroService.guestJoin(req);
    }

    /** Advance retro to the next workflow phase. */
    @PostMapping("/{retroId}/phase/next")
    public RetroDtos.RetroResponse advancePhase(@PathVariable Long retroId) {
        return retroService.advancePhase(CurrentUser.id(), retroId);
    }

    /** Rewind retro to the previous workflow phase. */
    @PostMapping("/{retroId}/phase/prev")
    public RetroDtos.RetroResponse rewindPhase(@PathVariable Long retroId) {
        return retroService.rewindPhase(CurrentUser.id(), retroId);
    }

    // Card endpoints scoped to a retro
    @PostMapping("/{retroId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public CardDtos.CardResponse createCard(@PathVariable Long retroId,
                                            @Valid @RequestBody CardDtos.CreateCardRequest req) {
        return cardService.create(CurrentUser.id(), retroId, req);
    }

    @GetMapping("/{retroId}/cards")
    public List<CardDtos.CardResponse> listCards(@PathVariable Long retroId) {
        return cardService.list(CurrentUser.id(), retroId);
    }
}
