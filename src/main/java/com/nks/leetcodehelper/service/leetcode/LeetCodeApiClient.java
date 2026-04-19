package com.nks.leetcodehelper.service.leetcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nks.leetcodehelper.config.LeetCodeProperties;
import com.nks.leetcodehelper.model.Problem;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



@Service
public class LeetCodeApiClient {

    private static final String DAILY_QUERY = """
            query questionOfToday {
              activeDailyCodingChallengeQuestion {
                question {
                  questionFrontendId
                  title
                  titleSlug
                  difficulty
                  content
                  topicTags { name }
                  codeSnippets { lang langSlug code }
                }
              }
            }
            """;

    private static final String ALL_PROBLEMS_QUERY = """
            query problemList($skip: Int, $limit: Int) {
              problemsetQuestionList: questionList(
                categorySlug: ""
                limit: $limit
                skip: $skip
                filters: {}
              ) {
                total: totalNum
                questions: data {
                  questionFrontendId
                  titleSlug
                  difficulty
                }
              }
            }
            """;

    private static final String SLUG_QUERY = """
            query questionData($titleSlug: String!) {
              question(titleSlug: $titleSlug) {
                questionFrontendId
                title
                titleSlug
                difficulty
                content
                topicTags { name }
                codeSnippets { lang langSlug code }
              }
            }
            """;

    private final LeetCodeProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LeetCodeApiClient(LeetCodeProperties props, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl("https://leetcode.com")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public Problem fetchDailyProblem() {
        String response = restClient.post()
                .uri("/graphql")
                .header("Cookie", buildCookie())
                .body(Map.of("query", DAILY_QUERY))
                .retrieve()
                .body(String.class);

        JsonNode question = parseJson(response)
                .path("data")
                .path("activeDailyCodingChallengeQuestion")
                .path("question");
        return parseProblem(question);
    }

    public record ProblemSummary(String id, String titleSlug, String difficulty) {}

    public List<ProblemSummary> fetchAllProblems() {
        List<ProblemSummary> all = new ArrayList<>();
        int skip = 0;
        int limit = 100;
        int total;
        do {
            String response = restClient.post()
                    .uri("/graphql")
                    .header("Cookie", buildCookie())
                    .body(Map.of("query", ALL_PROBLEMS_QUERY, "variables", Map.of("skip", skip, "limit", limit)))
                    .retrieve()
                    .body(String.class);

            JsonNode root = parseJson(response).path("data").path("problemsetQuestionList");
            total = root.path("total").asInt();
            root.path("questions").forEach(q -> all.add(new ProblemSummary(
                    q.path("questionFrontendId").asText(),
                    q.path("titleSlug").asText(),
                    q.path("difficulty").asText()
            )));
            skip += limit;
        } while (skip < total);
        return all;
    }

    public Problem fetchProblemBySlug(String titleSlug) {
        String response = restClient.post()
                .uri("/graphql")
                .header("Cookie", buildCookie())
                .body(Map.of("query", SLUG_QUERY, "variables", Map.of("titleSlug", titleSlug)))
                .retrieve()
                .body(String.class);

        JsonNode question = parseJson(response).path("data").path("question");
        return parseProblem(question);
    }

    private Problem parseProblem(JsonNode question) {
        String id = question.path("questionFrontendId").asText();
        String title = question.path("title").asText();
        String titleSlug = question.path("titleSlug").asText();
        String difficulty = question.path("difficulty").asText();
        String content = question.path("content").asText();

        List<String> tags = new ArrayList<>();
        question.path("topicTags").forEach(tag -> tags.add(tag.path("name").asText()));

        String starterCode = "";
        String langSlug = props.getLanguage();
        for (JsonNode snippet : question.path("codeSnippets")) {
            if (snippet.path("langSlug").asText().equals(langSlug)) {
                starterCode = snippet.path("code").asText();
                break;
            }
        }

        return new Problem(id, title, titleSlug, difficulty, tags, content, starterCode, langSlug);
    }

    private String buildCookie() {
        return "LEETCODE_SESSION=" + props.getSession() + "; csrftoken=" + props.getCsrfToken();
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LeetCode API response", e);
        }
    }
}
