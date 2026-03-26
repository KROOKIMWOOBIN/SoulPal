package com.soulpal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class AsyncConfig {

    /**
     * SSE 스트리밍용 공유 스레드 풀
     * core=5, max=20, queue=100 — 무제한 스레드 생성 방지
     * AbortPolicy: 풀 포화 시 RejectedExecutionException → 컨트롤러에서 503 반환
     * (CallerRunsPolicy는 Tomcat 요청 스레드를 직접 점유해 서버 전체를 차단할 수 있음)
     */
    @Bean(name = "sseExecutor", destroyMethod = "shutdown")
    public ExecutorService sseExecutor() {
        return new ThreadPoolExecutor(
                5, 20, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
