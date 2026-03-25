package com.soulpal.service;

import com.soulpal.model.Message;
import com.soulpal.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 대화 이력을 분석하여 Ollama에 전달할 개인화 컨텍스트를 구성합니다.
 * - 사용자 발화 패턴(빈도 키워드, 감정 톤, 대화 깊이)을 분석해 시스템 프롬프트에 주입
 * - 현재 메시지와의 키워드 유사도를 기반으로 관련성 높은 이전 대화를 선별
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextBuilderService {

    private final MessageRepository messageRepository;

    // 분석에서 제외할 불용어 (한국어 + 영어)
    private static final Set<String> STOP_WORDS = Set.of(
            "이", "가", "을", "를", "은", "는", "의", "에", "에서", "로", "으로",
            "와", "과", "도", "만", "이야", "야", "해", "해줘", "주세요", "좀",
            "그", "저", "나", "내", "네", "뭐", "어", "응", "아", "오", "음", "흠",
            "the", "a", "an", "is", "are", "was", "were", "i", "you", "it",
            "and", "or", "but", "in", "on", "at", "to", "for", "of", "that", "this"
    );

    private static final Set<String> POSITIVE_WORDS = Set.of(
            "좋아", "행복", "기뻐", "신나", "재밌", "즐거", "사랑", "감사", "최고", "완벽",
            "설레", "기대", "웃", "귀여", "멋있", "대단", "훌륭",
            "happy", "great", "love", "good", "fun", "amazing", "wonderful", "excited"
    );

    private static final Set<String> NEGATIVE_WORDS = Set.of(
            "슬퍼", "힘들", "지쳐", "외로", "무서", "불안", "걱정", "싫어", "짜증", "화나",
            "우울", "답답", "괴로", "아파", "힘내", "울고", "눈물", "피곤",
            "sad", "tired", "lonely", "scared", "anxious", "hate", "angry", "depressed", "stressed"
    );

    /**
     * 대화 이력을 분석해 시스템 프롬프트에 추가할 개인화 컨텍스트 문자열을 반환합니다.
     * 결과는 5분간 캐싱됩니다 (Caffeine).
     * 대화가 충분하지 않으면 빈 문자열을 반환합니다.
     */
    @Cacheable(value = "userContext", key = "#characterId")
    public String buildUserContext(String characterId) {
        long totalMessages = messageRepository.countByCharacterId(characterId);
        if (totalMessages < 4) {
            log.debug("[CONTEXT] 메시지 부족으로 컨텍스트 생략: characterId={}, count={}", characterId, totalMessages);
            return "";
        }

        List<Message> sample = messageRepository.findByCharacterIdOrderByCreatedAtDesc(
                characterId, PageRequest.of(0, 120));

        List<Message> userMessages = sample.stream()
                .filter(Message::isUser)
                .collect(Collectors.toList());

        if (userMessages.isEmpty()) return "";

        List<String> topKeywords = extractTopKeywords(userMessages, 6);
        String emotionalContext = detectEmotionalTone(userMessages);
        String depthContext = buildDepthContext(totalMessages);

        log.info("[CONTEXT] 개인화 컨텍스트 생성: characterId={}, totalMsgs={}, keywords={}, emotion={}",
                characterId, totalMessages,
                topKeywords.isEmpty() ? "없음" : String.join(",", topKeywords),
                emotionalContext.isEmpty() ? "neutral" : emotionalContext.split("—")[0].trim());

        StringBuilder sb = new StringBuilder("\n\n[사용자 분석 — 이 정보를 반드시 대화에 반영해줘]");
        sb.append("\n- ").append(depthContext);

        if (!topKeywords.isEmpty()) {
            sb.append("\n- 사용자가 자주 언급하는 주제/관심사: ").append(String.join(", ", topKeywords));
        }
        if (!emotionalContext.isEmpty()) {
            sb.append("\n- 최근 감정 상태: ").append(emotionalContext);
        }

        sb.append("\n위 맥락을 자연스럽게 녹여 개인화된 응답을 해줘.");
        return sb.toString();
    }

    /**
     * 현재 메시지와 관련성이 높은 과거 대화를 선별하여 반환합니다.
     * 최근 N개는 항상 포함하고, 이전 대화 중 키워드 유사도가 높은 것을 추가합니다.
     *
     * @param characterId  캐릭터 ID
     * @param currentMsg   현재 사용자 메시지
     * @param recentCount  항상 포함할 최신 메시지 수
     * @param extraCount   추가로 포함할 관련 과거 메시지 쌍 수 (user+AI 쌍)
     */
    public List<Message> getRelevantHistory(String characterId, String currentMsg,
                                            int recentCount, int extraCount) {
        List<Message> recent = messageRepository.findByCharacterIdOrderByCreatedAtDesc(
                characterId, PageRequest.of(0, recentCount));
        Collections.reverse(recent);

        long total = messageRepository.countByCharacterId(characterId);
        if (total <= recentCount || extraCount <= 0) {
            log.debug("[CONTEXT] 히스토리: characterId={}, count={} (recent only)", characterId, recent.size());
            return recent;
        }

        Set<String> currentKeywords = extractKeywords(currentMsg);
        if (currentKeywords.isEmpty()) {
            log.debug("[CONTEXT] 히스토리: characterId={}, count={} (no keywords)", characterId, recent.size());
            return recent;
        }

        // 더 오래된 메시지 풀을 가져와 관련성 점수 계산
        List<Message> pool = messageRepository.findByCharacterIdOrderByCreatedAtDesc(
                characterId, PageRequest.of(0, 300));

        Set<String> recentIds = recent.stream().map(Message::getId).collect(Collectors.toSet());

        // user 메시지만 점수 산정 — 점수 있는 것만 추출
        List<Message> scoredUserMsgs = pool.stream()
                .filter(m -> !recentIds.contains(m.getId()))
                .filter(Message::isUser)
                .filter(m -> scoreRelevance(m.getContent(), currentKeywords) > 0)
                .sorted(Comparator.comparingInt((Message m) ->
                        scoreRelevance(m.getContent(), currentKeywords)).reversed())
                .limit(extraCount)
                .collect(Collectors.toList());

        if (scoredUserMsgs.isEmpty()) {
            log.debug("[CONTEXT] 히스토리: characterId={}, count={} (no relevant extras)", characterId, recent.size());
            return recent;
        }

        // 관련 user 메시지를 시간순으로 정렬 후 앞에 추가
        scoredUserMsgs.sort(Comparator.comparing(Message::getCreatedAt));

        List<Message> combined = new ArrayList<>(scoredUserMsgs);
        combined.addAll(recent);

        log.debug("[CONTEXT] 히스토리: characterId={}, total={}, recent={}, extra={}",
                characterId, combined.size(), recent.size(), scoredUserMsgs.size());
        return combined;
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private List<String> extractTopKeywords(List<Message> messages, int limit) {
        Map<String, Integer> freq = new HashMap<>();
        for (Message msg : messages) {
            for (String word : tokenize(msg.getContent())) {
                if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                    freq.merge(word, 1, Integer::sum);
                }
            }
        }
        return freq.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Set<String> extractKeywords(String text) {
        Set<String> result = new HashSet<>();
        for (String w : tokenize(text)) {
            if (w.length() >= 2 && !STOP_WORDS.contains(w)) result.add(w);
        }
        return result;
    }

    private String[] tokenize(String text) {
        return text.toLowerCase()
                .replaceAll("[^가-힣a-z0-9\\s]", " ")
                .trim()
                .split("\\s+");
    }

    private int scoreRelevance(String content, Set<String> keywords) {
        String lower = content.toLowerCase();
        int score = 0;
        for (String kw : keywords) {
            if (lower.contains(kw)) score++;
        }
        return score;
    }

    private String detectEmotionalTone(List<Message> messages) {
        List<Message> recent = messages.stream().limit(12).collect(Collectors.toList());
        int pos = 0, neg = 0;

        for (Message msg : recent) {
            String c = msg.getContent().toLowerCase();
            for (String w : POSITIVE_WORDS) {
                if (c.contains(w)) { pos++; break; }
            }
            for (String w : NEGATIVE_WORDS) {
                if (c.contains(w)) { neg++; break; }
            }
        }

        if (pos == 0 && neg == 0) return "";
        if (pos > neg * 2) return "밝고 긍정적 — 활기차고 즐겁게 대화해줘";
        if (neg > pos * 2) return "다소 힘들어 보임 — 따뜻하게 공감하고 위로해줘";
        if (neg > pos) return "약간 감정 기복 있음 — 조심스럽게 공감하며 대화해줘";
        return "";
    }

    private String buildDepthContext(long totalMessages) {
        if (totalMessages < 10) return "아직 초반 대화 — 서로 알아가는 단계이니 자연스럽게 첫 인사처럼 대화해줘";
        if (totalMessages < 50) return "어느 정도 친해진 사이 — 편하게 대화해줘";
        if (totalMessages < 200) return "오래 대화해온 친한 사이 — 격식 없이 편하게 말해줘";
        return "아주 오랜 친구처럼 서로 잘 아는 사이 — 매우 친밀하고 자연스럽게 대화해줘";
    }
}
