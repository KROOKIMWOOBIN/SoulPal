package com.soulpal.exception;

import com.soulpal.config.JwtUtil;
import com.soulpal.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandler 슬라이스 테스트.
 * 각 예외 타입별로 올바른 HTTP 상태코드와 응답 코드를 검증합니다.
 */
@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;

    @MockBean JwtUtil      jwtUtil;
    @MockBean TokenService tokenService;

    /** 예외를 유발하는 테스트용 컨트롤러 */
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business")
        void business() {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }

        @GetMapping("/illegal")
        void illegal() {
            throw new IllegalArgumentException("잘못된 값");
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
        void generic() throws Exception {
            throw new RuntimeException("알 수 없는 오류");
        }
    }

    @Test
    @DisplayName("BusinessException → AI001, 503")
    void businessException() throws Exception {
        mockMvc.perform(get("/test/business").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI001"));
    }

    @Test
    @DisplayName("IllegalArgumentException → C001, 400")
    void illegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/illegal").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 값"));
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
        mockMvc.perform(get("/test/illegal").accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
