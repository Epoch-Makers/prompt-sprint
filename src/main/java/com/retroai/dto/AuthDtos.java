package com.retroai.dto;

import com.retroai.enums.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public static class RegisterRequest {
        @Email @NotBlank
        public String email;
        @NotBlank @Size(max = 120)
        public String fullName;
        @NotBlank @Size(min = 6, max = 100)
        public String password;
    }

    public static class LoginRequest {
        @Email @NotBlank
        public String email;
        @NotBlank
        public String password;
    }

    public static class UserResponse {
        public Long id;
        public String email;
        public String fullName;
        public AuthProvider authProvider;

        public UserResponse() {}
        public UserResponse(Long id, String email, String fullName) {
            this(id, email, fullName, AuthProvider.LOCAL);
        }
        public UserResponse(Long id, String email, String fullName, AuthProvider authProvider) {
            this.id = id; this.email = email; this.fullName = fullName; this.authProvider = authProvider;
        }
    }

    public static class LoginResponse {
        public String token;
        public UserResponse user;

        public LoginResponse() {}
        public LoginResponse(String token, UserResponse user) {
            this.token = token; this.user = user;
        }
    }
}
