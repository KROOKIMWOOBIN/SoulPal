package com.soulpal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class StartupValidator {

    private static final String DEFAULT_SECRET = "soulpal-production-secret-change-this-in-real-deployment-please";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (DEFAULT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                "[SECURITY] JWT_SECRET이 기본값입니다. 프로덕션 환경에서는 반드시 변경하세요."
            );
        }
    }
}
