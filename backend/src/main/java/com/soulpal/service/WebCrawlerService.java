package com.soulpal.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 웹 검색 + 크롤링 서비스 (RAG 용도)
 * - DuckDuckGo Lite HTML 파싱으로 검색 결과 URL 수집
 * - 최대 3개 URL을 병렬 크롤링 (각 2초 타임아웃) → 전체 최대 3초
 */
@Slf4j
@Service
public class WebCrawlerService {

    private static final String USER_AGENT         = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final int    CONNECT_TIMEOUT_MS = 2_000;   // URL당 2초 (병렬 실행)
    private static final int    PARALLEL_TIMEOUT_S = 4;       // 전체 크롤링 최대 4초
    private static final int    MAX_CONTEXT_CHARS  = 2_000;
    private static final int    MAX_URLS           = 3;

    private static final Pattern SEARCH_TRIGGER = Pattern.compile(
        "뭐야|뭔지|알려줘|찾아줘|검색해줘|최신|뉴스|날씨|정보|어떻게|언제|어디|무엇|누구|" +
        "what|who|when|where|how|news|latest|search|find|\\?",
        Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> BLOCKED_DOMAINS = Set.of(
        "facebook.com", "instagram.com", "twitter.com", "x.com",
        "youtube.com", "tiktok.com", "linkedin.com"
    );

    // 크롤링 전용 스레드 풀 (최대 6 스레드)
    private final ExecutorService crawlPool = Executors.newFixedThreadPool(6);

    public boolean needsWebSearch(String message) {
        return SEARCH_TRIGGER.matcher(message).find();
    }

    /**
     * 검색 → 병렬 크롤링 → 컨텍스트 문자열 반환.
     * 전체 소요 시간은 PARALLEL_TIMEOUT_S 초 이내로 제한됩니다.
     */
    public String getWebContext(String query) {
        try {
            List<String> urls = searchDuckDuckGo(query);
            if (urls.isEmpty()) return "";

            // 모든 URL 병렬 크롤링
            List<CompletableFuture<String>> futures = urls.stream()
                    .map(url -> CompletableFuture.supplyAsync(() -> crawlPage(url), crawlPool))
                    .toList();

            // 전체 결과를 최대 PARALLEL_TIMEOUT_S 초 안에 수집
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < futures.size(); i++) {
                try {
                    String text = futures.get(i).get(PARALLEL_TIMEOUT_S, TimeUnit.SECONDS);
                    if (!text.isBlank()) {
                        context.append("출처: ").append(urls.get(i)).append("\n");
                        context.append(text).append("\n\n");
                        if (context.length() >= MAX_CONTEXT_CHARS) break;
                    }
                } catch (Exception e) {
                    log.debug("[WebCrawler] 병렬 크롤 타임아웃: {}", urls.get(i));
                }
            }

            if (context.isEmpty()) return "";

            String truncated = context.length() > MAX_CONTEXT_CHARS
                    ? context.substring(0, MAX_CONTEXT_CHARS) + "..."
                    : context.toString();

            return "\n===\n웹 검색 결과 (참고용):\n" + truncated + "===\n";

        } catch (Exception e) {
            log.warn("[WebCrawler] 검색 실패: {} - {}", query, e.getMessage());
            return "";
        }
    }

    // ── 내부 메서드 ────────────────────────────────────────────────────────────

    private List<String> searchDuckDuckGo(String query) {
        List<String> urls = new ArrayList<>();
        try {
            String encoded   = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = "https://html.duckduckgo.com/html/?q=" + encoded;

            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENT)
                    .timeout(CONNECT_TIMEOUT_MS * 2)
                    .get();

            Elements links = doc.select("a.result__url, .result__a");
            for (Element link : links) {
                String href = link.attr("href");
                if (href.startsWith("http") && isAllowedUrl(href)) {
                    urls.add(href);
                    if (urls.size() >= MAX_URLS) break;
                }
            }

            if (urls.isEmpty()) {
                Elements uddgLinks = doc.select("a[href*=uddg=]");
                for (Element link : uddgLinks) {
                    String href = extractUddgUrl(link.attr("href"));
                    if (href != null && isAllowedUrl(href)) {
                        urls.add(href);
                        if (urls.size() >= MAX_URLS) break;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("[WebCrawler] DuckDuckGo 검색 오류: {}", e.getMessage());
        }
        return urls;
    }

    private String crawlPage(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(CONNECT_TIMEOUT_MS)
                    .ignoreHttpErrors(true)
                    .get();

            doc.select("script, style, nav, header, footer, iframe, aside, .ad, .advertisement").remove();

            String text = "";
            for (String selector : List.of("article", "main", ".content", ".post", "#content", "body")) {
                Element el = doc.selectFirst(selector);
                if (el != null) {
                    text = el.text();
                    if (text.length() > 200) break;
                }
            }
            if (text.length() < 100 && doc.body() != null) text = doc.body().text();

            text = text.replaceAll("\\s{3,}", " ").trim();
            return text.length() > 800 ? text.substring(0, 800) : text;

        } catch (Exception e) {
            log.debug("[WebCrawler] 페이지 크롤 실패: {} - {}", url, e.getMessage());
            return "";
        }
    }

    private boolean isAllowedUrl(String url) {
        return BLOCKED_DOMAINS.stream().noneMatch(url::contains);
    }

    private String extractUddgUrl(String href) {
        try {
            int idx = href.indexOf("uddg=");
            if (idx < 0) return null;
            String encoded = href.substring(idx + 5);
            int amp = encoded.indexOf("&");
            if (amp > 0) encoded = encoded.substring(0, amp);
            return java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
