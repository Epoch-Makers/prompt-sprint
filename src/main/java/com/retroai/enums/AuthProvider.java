package com.retroai.enums;

public enum AuthProvider {
    LOCAL,      // Email + password registration
    ATLASSIAN,  // OAuth 3LO via Atlassian
    GUEST       // Anonymous guest-join, no credentials
}
