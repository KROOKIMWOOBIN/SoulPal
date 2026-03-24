package com.soulpal.controller;

import com.soulpal.dto.AuthResponse;
import com.soulpal.dto.LoginRequest;
import com.soulpal.dto.RegisterRequest;
import com.soulpal.model.User;
import com.soulpal.service.AuthService;
import com.soulpal.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @Operation(summary = "회원가입")
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

    @Operation(summary = "로그아웃 (토큰 무효화)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication auth,
            @RequestHeader("Authorization") String authHeader) {
        String userId      = (String) auth.getPrincipal();
        String accessToken = authHeader.substring(7);
        tokenService.logout(userId, accessToken);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴 (계정 및 데이터 삭제)")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            Authentication auth,
            @RequestHeader("Authorization") String authHeader) {
        String userId      = (String) auth.getPrincipal();
        String accessToken = authHeader.substring(7);
        authService.deleteAccount(userId, accessToken);
        return ResponseEntity.noContent().build();
    }
}
