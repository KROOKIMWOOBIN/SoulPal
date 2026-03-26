package com.soulpal.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.concurrent.RejectedExecutionException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandler 슬라이스 테스트.
 * standaloneSetup을 사용해 Spring Security 없이 예외 핸들러만 격리 테스트합니다.
 */
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    /** 예외를 유발하는 테스트용 컨트롤러 */
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business")
        void business() {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }

        @GetMapping("/rejected")
        void rejected() {
            throw new RejectedExecutionException("스레드 풀 포화");
        }

        @GetMapping("/notfound")
        void notFound() {
            throw new ResourceNotFoundException("리소스 없음");
        }

        @GetMapping("/ratelimit")
        void rateLimit() {
            throw new RateLimitExceededException();
        }

        @GetMapping("/generic")
        void generic() {
            throw new RuntimeException("알 수 없는 오류");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("BusinessException → AI001, 503")
    void businessException() throws Exception {
        mockMvc.perform(get("/test/business").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI001"));
    }

    @Test
    @DisplayName("RejectedExecutionException → R002, 503")
    void rejectedExecutionException() throws Exception {
        mockMvc.perform(get("/test/rejected").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("R002"));
    }

    @Test
    @DisplayName("ResourceNotFoundException → C002, 404")
    void resourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/notfound").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("C002"));
    }

    @Test
    @DisplayName("RateLimitExceededException → R001, 429")
    void rateLimitExceededException() throws Exception {
        mockMvc.perform(get("/test/ratelimit").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("R001"));
    }

    @Test
    @DisplayName("미처리 Exception → C005, 500")
    void genericException() throws Exception {
        mockMvc.perform(get("/test/generic").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("C005"));
    }

    @Test
    @DisplayName("응답에 timestamp 필드 포함")
    void responseHasTimestamp() throws Exception {
        mockMvc.perform(get("/test/notfound").accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
