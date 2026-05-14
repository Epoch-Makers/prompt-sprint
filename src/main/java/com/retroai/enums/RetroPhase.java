package com.retroai.enums;

/**
 * 4-phase retrospective workflow.
 *
 * Phase transitions are linear: WRITING → GROUPING → VOTING → DISCUSSION → CLOSED.
 * Board owner advances/rewinds via /api/retros/{id}/phase/next | prev.
 */
public enum RetroPhase {
    WRITING,    // Members add/edit/delete cards
    GROUPING,   // Board owner clusters cards
    VOTING,     // Members spend their 3-vote quota
    DISCUSSION, // AI analyze, action approval, Jira export
    CLOSED;     // Terminal — no further mutations

    public RetroPhase next() {
        switch (this) {
            case WRITING:    return GROUPING;
            case GROUPING:   return VOTING;
            case VOTING:     return DISCUSSION;
            case DISCUSSION: return CLOSED;
            default:         return CLOSED;
        }
    }

    public RetroPhase prev() {
        switch (this) {
            case GROUPING:   return WRITING;
            case VOTING:     return GROUPING;
            case DISCUSSION: return VOTING;
            case CLOSED:     return DISCUSSION;
            default:         return WRITING;
        }
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
