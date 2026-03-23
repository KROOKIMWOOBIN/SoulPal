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
import java.util.regex.Pattern;

/**
 * 웹 검색 + 크롤링 서비스 (RAG 용도)
 * - DuckDuckGo Lite HTML 파싱으로 검색 결과 URL 수집
 * - JSoup으로 각 페이지 크롤 후 핵심 텍스트 추출
 */
@Slf4j
@Service
public class WebCrawlerService {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36";
    private static final int    CONNECT_TIMEOUT_MS = 5_000;
    private static final int    MAX_CONTEXT_CHARS  = 2_000;
    private static final int    MAX_URLS           = 3;

    // 검색이 필요한 한국어/영어 키워드 패턴
    private static final Pattern SEARCH_TRIGGER = Pattern.compile(
        "뭐야|뭔지|알려줘|찾아줘|검색해줘|최신|뉴스|날씨|정보|어떻게|언제|어디|무엇|누구|" +
        "what|who|when|where|how|news|latest|search|find|\\?",
        Pattern.CASE_INSENSITIVE
    );

    // 크롤링 제외 도메인 (광고, 로그인 필요 사이트 등)
    private static final Set<String> BLOCKED_DOMAINS = Set.of(
        "facebook.com", "instagram.com", "twitter.com", "x.com",
        "youtube.com", "tiktok.com", "linkedin.com"
    );

    /** 메시지가 웹 검색이 필요한 내용인지 자동 판단 */
    public boolean needsWebSearch(String message) {
        return SEARCH_TRIGGER.matcher(message).find();
    }

    /**
     * 검색 쿼리로 웹 컨텍스트 생성
     * @return "===\n웹 검색 결과:\n...\n===" 형태의 문자열, 실패 시 빈 문자열
     */
    public String getWebContext(String query) {
        try {
            List<String> urls = searchDuckDuckGo(query);
            if (urls.isEmpty()) return "";

            StringBuilder context = new StringBuilder();
            for (String url : urls) {
                String text = crawlPage(url);
                if (!text.isBlank()) {
                    context.append("출처: ").append(url).append("\n");
                    context.append(text).append("\n\n");
                    if (context.length() >= MAX_CONTEXT_CHARS) break;
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
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = "https://html.duckduckgo.com/html/?q=" + encoded;

            Document doc = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .timeout(CONNECT_TIMEOUT_MS)
                .get();

            // DuckDuckGo lite 결과 링크 파싱
            Elements links = doc.select("a.result__url, .result__a");
            for (Element link : links) {
                String href = link.attr("href");
                if (href.startsWith("http") && isAllowedUrl(href)) {
                    urls.add(href);
                    if (urls.size() >= MAX_URLS) break;
                }
            }

            // 결과가 없으면 uddg= 파라미터 방식 시도
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

            // <script>, <style>, <nav>, <header>, <footer> 제거
            doc.select("script, style, nav, header, footer, iframe, aside, .ad, .advertisement").remove();

            // 본문 후보 순서로 시도
            String text = "";
            for (String selector : List.of("article", "main", ".content", ".post", "#content", "body")) {
                Element el = doc.selectFirst(selector);
                if (el != null) {
                    text = el.text();
                    if (text.length() > 200) break;
                }
            }

            // 너무 짧으면 전체 body
            if (text.length() < 100) {
                text = doc.body() != null ? doc.body().text() : "";
            }

            // 연속 공백/줄바꿈 정리
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
