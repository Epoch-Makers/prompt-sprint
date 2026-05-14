package com.retroai.controller;

import com.retroai.dto.AiDtos;
import com.retroai.security.CurrentUser;
import com.retroai.service.AiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/analyze")
    public AiDtos.AnalyzeResponse analyze(@Valid @RequestBody AiDtos.AnalyzeRequest req) {
        return aiService.analyze(CurrentUser.id(), req.retroId);
    }

    @PostMapping("/risk-score")
    public List<AiDtos.RiskScoreItem> riskScore(@Valid @RequestBody AiDtos.RiskScoreRequest req) {
        return aiService.riskScore(CurrentUser.id(), req.actionIds);
    }

    @PostMapping("/maturity")
    public AiDtos.MaturityResponse maturity(@Valid @RequestBody AiDtos.MaturityRequest req) {
        return aiService.maturity(CurrentUser.id(), req.retroId);
    }

    @GetMapping("/briefing")
    public AiDtos.BriefingResponse briefing(@RequestParam Long retroId) {
        return aiService.briefing(CurrentUser.id(), retroId);
    }

    @GetMapping("/silent-prompt")
    public AiDtos.SilentPromptResponse silentPrompt(@RequestParam Long retroId,
                                                    @RequestParam Long userId) {
        return aiService.silentPrompt(CurrentUser.id(), retroId, userId);
    }

    @PostMapping("/jira-history")
    public AiDtos.JiraHistoryResponse jiraHistory(@Valid @RequestBody AiDtos.JiraHistoryRequest req) {
        return aiService.jiraHistory(CurrentUser.id(), req.retroId, null);
    }
}
