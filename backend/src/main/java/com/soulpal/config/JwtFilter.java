package com.soulpal.config;

import com.soulpal.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import org.slf4j.MDC;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isValid(token) && !tokenService.isBlacklisted(token)) {
                Claims claims = jwtUtil.parse(token);
                // access 토큰만 API 접근 허용 (type 필드 없거나 access 아닌 토큰 차단)
                if ("access".equals(claims.get("type", String.class))) {
                    String userId   = claims.getSubject();
                    String username = claims.get("username", String.class);
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    auth.setDetails(username);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    MDC.put("userId", userId);
                }
            }
        }
        chain.doFilter(req, res);
    }
}
