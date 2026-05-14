package com.retroai.service;

import com.retroai.dto.AuthDtos;
import com.retroai.entity.User;
import com.retroai.exception.ApiException;
import com.retroai.repository.UserRepository;
import com.retroai.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.UserResponse register(AuthDtos.RegisterRequest req) {
        if (userRepository.existsByEmail(req.email)) {
            throw ApiException.conflict("Email already registered");
        }
        User u = new User();
        u.setEmail(req.email);
        u.setFullName(req.fullName);
        u.setPasswordHash(passwordEncoder.encode(req.password));
        userRepository.save(u);
        return new AuthDtos.UserResponse(u.getId(), u.getEmail(), u.getFullName());
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req) {
        User u = userRepository.findByEmail(req.email)
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (!passwordEncoder.matches(req.password, u.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        String token = jwtService.generateToken(u.getId(), u.getEmail());
        return new AuthDtos.LoginResponse(token,
                new AuthDtos.UserResponse(u.getId(), u.getEmail(), u.getFullName()));
    }

    public AuthDtos.UserResponse me(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        return new AuthDtos.UserResponse(u.getId(), u.getEmail(), u.getFullName());
    }
}
