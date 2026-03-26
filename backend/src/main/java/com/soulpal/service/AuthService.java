package com.soulpal.service;

import com.soulpal.config.JwtUtil;
import com.soulpal.dto.AuthResponse;
import com.soulpal.dto.LoginRequest;
import com.soulpal.dto.RegisterRequest;
import com.soulpal.model.User;
import com.soulpal.repository.CharacterRepository;
import com.soulpal.repository.GroupRoomRepository;
import com.soulpal.repository.MessageRepository;
import com.soulpal.repository.ProjectRepository;
import com.soulpal.repository.UserRepository;
import com.soulpal.exception.BusinessException;
import com.soulpal.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final CharacterRepository characterRepository;
    private final MessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final GroupRoomRepository groupRoomRepository;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            log.warn("[AUTH] 회원가입 실패 - 이메일 중복: {}", req.getEmail());
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            log.warn("[AUTH] 회원가입 실패 - 사용자명 중복: {}", req.getUsername());
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATED);
        }

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();
        userRepository.save(user);
        log.info("[AUTH] 회원가입 완료: userId={}, email={}", user.getId(), user.getEmail());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> {
                    log.warn("[AUTH] 로그인 실패 - 존재하지 않는 이메일: {}", req.getEmail());
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            log.warn("[AUTH] 로그인 실패 - 비밀번호 불일치: userId={}", user.getId());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("[AUTH] 로그인 성공: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    public User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    /** 회원 탈퇴: 메시지 → 캐릭터 → 프로젝트 → 유저 순으로 삭제, 토큰 무효화 */
    @Transactional
    public void deleteAccount(String userId, String accessToken) {
        // 해당 유저의 캐릭터 ID 목록 수집 후 메시지 삭제
        List<String> characterIds = characterRepository.findAllByUserId(userId)
                .stream().map(c -> c.getId()).toList();
        if (!characterIds.isEmpty()) {
            messageRepository.deleteByCharacterIdIn(characterIds);
        }

        // 캐릭터 → 그룹 방 → 프로젝트 → 유저 삭제
        characterRepository.deleteAllByUserId(userId);
        groupRoomRepository.deleteAllByUserId(userId);
        projectRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);

        // 토큰 무효화
        tokenService.logout(userId, accessToken);
        log.info("[AUTH] 회원 탈퇴 완료: userId={}", userId);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        tokenService.saveRefreshToken(user.getId(), refreshToken);
        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getUsername(), user.getEmail());
    }
}
