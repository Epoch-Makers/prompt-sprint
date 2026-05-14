package com.retroai.security;

public class AuthenticatedUser {
    private final Long id;
    private final String email;
    private final boolean guest;
    private final Long guestRetroId;
    private final Long guestTeamId;

    public AuthenticatedUser(Long id, String email) {
        this(id, email, false, null, null);
    }

    public AuthenticatedUser(Long id, String email, boolean guest, Long guestRetroId, Long guestTeamId) {
        this.id = id;
        this.email = email;
        this.guest = guest;
        this.guestRetroId = guestRetroId;
        this.guestTeamId = guestTeamId;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public boolean isGuest() { return guest; }
    public Long getGuestRetroId() { return guestRetroId; }
    public Long getGuestTeamId() { return guestTeamId; }
}
