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
     * Public guest join endpoint (legacy). No JWT required — accepts the guest
     * token issued at retro creation.
     */
    @GetMapping("/{retroId}/join")
    public RetroDtos.JoinResponse join(@PathVariable Long retroId, @RequestParam("token") String token) {
        return retroService.joinByToken(retroId, token);
    }

    /**
     * Spec 3.6 — public lookup. Validates the guest-join token and returns
     * a retro summary so the frontend can render a "Görünen adınızı girin" form.
     */
    @GetMapping("/{retroId}/join/lookup")
    public RetroDtos.JoinLookupResponse joinLookup(@PathVariable Long retroId,
                                                   @RequestParam("token") String token) {
        return retroService.joinLookup(retroId, token);
    }

    /**
     * Spec 3.7 — public guest join. {@code {token, displayName}} → guest
     * session with {@code guestSessionId} the SPA uses on subsequent writes
     * via the {@code X-Guest-Session} header.
     */
    @PostMapping("/{retroId}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public RetroDtos.JoinPostResponse joinPost(@PathVariable Long retroId,
                                               @Valid @RequestBody RetroDtos.JoinPostRequest req) {
        return retroService.joinAsGuest(retroId, req);
    }

    /** Legacy /guest-join endpoint — kept for backward compatibility. */
    @PostMapping("/guest-join")
    public RetroDtos.GuestJoinResponse guestJoin(@Valid @RequestBody RetroDtos.GuestJoinRequest req) {
        return retroService.guestJoin(req);
    }

    /** Spec 3.8 — unified phase change. Body {@code {targetPhase}} or {@code {action}}. */
    @PostMapping("/{retroId}/phase")
    public RetroDtos.PhaseChangeResponse changePhase(@PathVariable Long retroId,
                                                     @RequestBody RetroDtos.PhaseChangeRequest req) {
        return retroService.changePhase(CurrentUser.id(), retroId, req);
    }

    /** Legacy: advance retro to the next workflow phase. */
    @PostMapping("/{retroId}/phase/next")
    public RetroDtos.RetroResponse advancePhase(@PathVariable Long retroId) {
        return retroService.advancePhase(CurrentUser.id(), retroId);
    }

    /** Legacy: rewind retro to the previous workflow phase. */
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
