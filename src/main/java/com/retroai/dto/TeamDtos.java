package com.retroai.dto;

import com.retroai.enums.TeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class TeamDtos {

    public static class CreateTeamRequest {
        @NotBlank
        public String name;
    }

    public static class TeamResponse {
        public Long id;
        public String name;
        public Instant createdAt;
        public TeamRole myRole;
        public Long memberCount;

        public TeamResponse() {}
        public TeamResponse(Long id, String name, Instant createdAt, TeamRole myRole, Long memberCount) {
            this.id = id; this.name = name; this.createdAt = createdAt;
            this.myRole = myRole; this.memberCount = memberCount;
        }
    }

    public static class MemberResponse {
        public Long userId;
        public String fullName;
        public String email;
        public TeamRole role;

        public MemberResponse() {}
        public MemberResponse(Long userId, String fullName, String email, TeamRole role) {
            this.userId = userId; this.fullName = fullName; this.email = email; this.role = role;
        }
    }

    public static class TeamDetailResponse {
        public Long id;
        public String name;
        public List<MemberResponse> members;

        public TeamDetailResponse() {}
        public TeamDetailResponse(Long id, String name, List<MemberResponse> members) {
            this.id = id; this.name = name; this.members = members;
        }
    }

    public static class AddMemberRequest {
        @Email @NotBlank
        public String email;
    }
}
