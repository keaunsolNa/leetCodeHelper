package com.nks.leetcodehelper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class CodeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CodeAnalysisService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public CodeAnalysisService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${groq.api-key}") String apiKey) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.groq.com")
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public String analyze(String titleSlug, String difficulty, String tags, String lang, String code) {
        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", buildPrompt(titleSlug, difficulty, tags, lang, code)
                ))
        );

        try {
            String response = restClient.post()
                    .uri("/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").path(0)
                    .path("message").path("content")
                    .asText("분석 결과를 가져올 수 없습니다.");
        } catch (Exception e) {
            log.warn("코드 분석 실패: {}", e.getMessage());
            return "코드 분석 실패: " + e.getMessage();
        }
    }

    private String buildPrompt(String titleSlug, String difficulty, String tags, String lang, String code) {
        return """
                You are an expert software engineer reviewing a LeetCode solution.
                IMPORTANT: You MUST write the entire review in Korean (한국어). Do not use Chinese or any other language.

                Problem: %s
                Difficulty: %s
                Tags: %s
                Language: %s

                ```%s
                %s
                ```

                Please write a concise code review in Korean (한국어) covering:
                1. **시간 복잡도** — Big-O 표기와 설명
                2. **공간 복잡도** — Big-O 표기와 설명
                3. **풀이 접근법** — 사용된 알고리즘/패턴 간단 설명
                4. **잘된 점** — 코드에서 잘 구현된 부분
                5. **개선 사항** — 최적화 가능한 부분이나 대안적 접근법 (있는 경우)
                """.formatted(titleSlug, difficulty, tags, lang, lang, code);
    }
}
