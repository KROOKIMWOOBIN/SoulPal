package com.soulpal.service;

import com.soulpal.config.JwtUtil;
import com.soulpal.dto.AuthResponse;
import com.soulpal.dto.LoginRequest;
import com.soulpal.dto.RegisterRequest;
import com.soulpal.model.User;
import com.soulpal.repository.CharacterRepository;
import com.soulpal.repository.MessageRepository;
import com.soulpal.repository.ProjectRepository;
import com.soulpal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock UserRepository     userRepository;
    @Mock PasswordEncoder    passwordEncoder;
    @Mock JwtUtil            jwtUtil;
    @Mock TokenService       tokenService;
    @Mock CharacterRepository characterRepository;
    @Mock MessageRepository  messageRepository;
    @Mock ProjectRepository  projectRepository;

    @InjectMocks AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-1")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$encoded$")
                .build();
    }

    // ── 회원가입 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("정상 회원가입 → AuthResponse 반환")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("password123");

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByUsername(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("$encoded$");
        given(userRepository.save(any())).willReturn(testUser);
        given(jwtUtil.generateAccessToken(anyString(), anyString())).willReturn("access-token");
        given(jwtUtil.generateRefreshToken(anyString(), anyString())).willReturn("refresh-token");

        AuthResponse resp = authService.register(req);

        assertThat(resp.getAccessToken()).isEqualTo("access-token");
        assertThat(resp.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(resp.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("이메일 중복 → IllegalArgumentException")
    void register_emailDuplicated() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@example.com");
        req.setUsername("newuser");

        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일");
    }

    @Test
    @DisplayName("사용자명 중복 → IllegalArgumentException")
    void register_usernameDuplicated() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@example.com");
        req.setUsername("dupuser");

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByUsername("dupuser")).willReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자명");
    }

    // ── 로그인 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("정상 로그인 → AuthResponse 반환")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("password123", "$encoded$")).willReturn(true);
        given(jwtUtil.generateAccessToken(anyString(), anyString())).willReturn("access-token");
        given(jwtUtil.generateRefreshToken(anyString(), anyString())).willReturn("refresh-token");

        AuthResponse resp = authService.login(req);

        assertThat(resp.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("이메일 없음 → IllegalArgumentException")
    void login_emailNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("notfound@example.com");
        req.setPassword("pw");

        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("비밀번호 불일치 → IllegalArgumentException")
    void login_wrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrong");

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("wrong", "$encoded$")).willReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── 회원탈퇴 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("회원 탈퇴 → 캐릭터/메시지/프로젝트/유저 삭제 및 토큰 무효화")
    void deleteAccount_success() {
        given(characterRepository.findAllByUserId("user-1")).willReturn(List.of());

        authService.deleteAccount("user-1", "access-token");

        then(characterRepository).should().deleteAllByUserId("user-1");
        then(projectRepository).should().deleteAllByUserId("user-1");
        then(userRepository).should().deleteById("user-1");
        then(tokenService).should().logout("user-1", "access-token");
    }
}
