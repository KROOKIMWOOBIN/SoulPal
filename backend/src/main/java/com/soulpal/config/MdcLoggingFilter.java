package com.soulpal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 MDC 컨텍스트를 주입하는 필터.
 *
 * MDC에 주입되는 필드:
 *   - requestId : 요청 단위 추적 ID (8자 UUID)
 *   - method    : HTTP 메서드
 *   - uri       : 요청 URI
 *   - userId    : 인증된 사용자 ID (JwtFilter에서 채움)
 *
 * 요청 완료 시 "{method} {uri} → {status} ({duration}ms)" 형식으로 INFO 로그 출력.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final Set<String> SKIP_PATHS = Set.of(
            "/actuator/health",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = req.getRequestURI();
        if (SKIP_PATHS.contains(uri)) {
            chain.doFilter(req, res);
            return;
        }

        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        long start = System.currentTimeMillis();

        MDC.put("requestId", requestId);
        MDC.put("method",    req.getMethod());
        MDC.put("uri",       uri);
        res.setHeader("X-Request-Id", requestId);

        try {
            chain.doFilter(req, res);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("{} {} → {} ({}ms)", req.getMethod(), uri, res.getStatus(), duration);
            MDC.clear();
        }
    }
}
