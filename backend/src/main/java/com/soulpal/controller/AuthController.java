package com.soulpal.controller;

import com.soulpal.dto.AuthResponse;
import com.soulpal.dto.LoginRequest;
import com.soulpal.dto.RegisterRequest;
import com.soulpal.model.User;
import com.soulpal.service.AuthService;
import com.soulpal.service.RateLimitService;
import com.soulpal.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RateLimitService rateLimitService;

    @Operation(summary = "회원가입")
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest httpReq) {
        rateLimitService.checkAuth(resolveClientIp(httpReq));
        return authService.register(req);
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        rateLimitService.checkAuth(resolveClientIp(httpReq));
        return authService.login(req);
    }

    private String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
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

    /** 액세스 + 리프레시 토큰 재발급 (Rotation) */
    @PostMapping("/refresh")
    public Map<String, String> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new com.soulpal.exception.BusinessException(
                    com.soulpal.exception.ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return tokenService.refresh(refreshToken);
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
