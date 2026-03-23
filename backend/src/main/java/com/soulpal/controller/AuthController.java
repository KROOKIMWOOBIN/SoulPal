package com.soulpal.controller;

import com.soulpal.dto.AuthResponse;
import com.soulpal.dto.LoginRequest;
import com.soulpal.dto.RegisterRequest;
import com.soulpal.model.User;
import com.soulpal.service.AuthService;
import com.soulpal.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        User user = authService.getUser(userId);
        return Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
        );
    }

    /** 액세스 토큰 재발급 */
    @PostMapping("/refresh")
    public Map<String, String> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken이 필요합니다.");
        }
        String newAccessToken = tokenService.refresh(refreshToken);
        return Map.of("accessToken", newAccessToken);
    }

    /** 로그아웃: 리프레시 토큰 삭제 + 액세스 토큰 블랙리스트 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication auth,
            @RequestHeader("Authorization") String authHeader) {
        String userId      = (String) auth.getPrincipal();
        String accessToken = authHeader.substring(7);
        tokenService.logout(userId, accessToken);
        return ResponseEntity.noContent().build();
    }
}
