package com.nks.leetcodehelper.service.leetcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nks.leetcodehelper.config.LeetCodeProperties;
import com.nks.leetcodehelper.model.SubmissionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class LeetCodeSubmitService {

    private static final Logger log = LoggerFactory.getLogger(LeetCodeSubmitService.class);
    private static final int MAX_POLL_ATTEMPTS = 20;
    private static final long POLL_INTERVAL_MS = 1500;

    // LeetCode submission status codes
    private static final int STATUS_ACCEPTED = 10;

    private final LeetCodeProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LeetCodeSubmitService(LeetCodeProperties props, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl("https://leetcode.com")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public SubmissionResult submit(String titleSlug, String questionId, String code) throws InterruptedException {
        String cookie = buildCookie();

        String response = restClient.post()
                .uri("/problems/{slug}/submit/", titleSlug)
                .header("Cookie", cookie)
                .header("X-CSRFToken", props.getCsrfToken())
                .header("Referer", "https://leetcode.com/problems/" + titleSlug + "/")
                .body(Map.of(
                        "lang", props.getLanguage(),
                        "question_id", questionId,
                        "typed_code", code
                ))
                .retrieve()
                .body(String.class);

        String submissionId = parseJson(response).path("submission_id").asText();
        log.info("Submitted '{}' - submission ID: {}", titleSlug, submissionId);

        return pollResult(submissionId, cookie, titleSlug);
    }

    private SubmissionResult pollResult(String submissionId, String cookie, String titleSlug) throws InterruptedException {
        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MS);

            String response = restClient.get()
                    .uri("/submissions/detail/{id}/check/", submissionId)
                    .header("Cookie", cookie)
                    .header("Referer", "https://leetcode.com/problems/" + titleSlug + "/")
                    .retrieve()
                    .body(String.class);

            JsonNode node = parseJson(response);
            String state = node.path("state").asText();

            if ("SUCCESS".equals(state)) {
                int statusCode = node.path("status_code").asInt();
                String statusMsg = node.path("status_msg").asText();
                String runtime = node.path("status_runtime").asText("N/A");
                String memory = node.path("status_memory").asText("N/A");

                log.info("Result for '{}': {} (code: {})", titleSlug, statusMsg, statusCode);
                return new SubmissionResult(state, statusMsg, statusCode == STATUS_ACCEPTED, runtime, memory, statusCode);
            }

            log.debug("Waiting for judgment... attempt {}/{}, state: {}", i + 1, MAX_POLL_ATTEMPTS, state);
        }

        log.warn("Timed out waiting for submission result");
        return new SubmissionResult("TIMEOUT", "Timeout waiting for result", false, "N/A", "N/A", -1);
    }

    private String buildCookie() {
        return "LEETCODE_SESSION=" + props.getSession() + "; csrftoken=" + props.getCsrfToken();
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LeetCode response", e);
        }
    }
}
