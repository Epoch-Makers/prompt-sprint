package com.retroai.enums;

public enum CardSource {
    USER,           // Created by a human user
    JIRA_AI,        // Created from a Jira-history AI insight
    AI_NEXT_STEP    // Auto-created in NEXT_STEPS column during AI analyze
}
