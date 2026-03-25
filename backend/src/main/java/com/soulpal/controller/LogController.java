package com.soulpal.controller;

import com.soulpal.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private static final int MAX_PER_MINUTE = 20;

    private final StringRedisTemplate redis;

    @PostMapping("/error")
    public ResponseEntity<Void> receiveError(@RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        checkIpRateLimit(request);

        String message  = str(body, "message");
        String source   = str(body, "source");
        String stack    = str(body, "stack");
        String route    = str(body, "route");
        String ua       = str(body, "userAgent");

        log.error("[FRONTEND] {} | route={} | source={} | ua={}\n{}",
                message, route, source, ua, stack);

        return ResponseEntity.noContent().build();
    }

    private void checkIpRateLimit(HttpServletRequest request) {
        String ip = resolveClientIp(request);
        long bucket = System.currentTimeMillis() / 60_000;
        String key = "ratelimit:log:" + ip + ":" + bucket;
        Long count = redis.opsForValue().increment(key);
        if (count == 1) {
            redis.expire(key, Duration.ofMinutes(2));
        }
        if (count > MAX_PER_MINUTE) {
            throw new RateLimitExceededException();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
