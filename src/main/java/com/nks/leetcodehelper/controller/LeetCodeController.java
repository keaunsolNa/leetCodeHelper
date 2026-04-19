package com.nks.leetcodehelper.controller;

import com.nks.leetcodehelper.controller.dto.SubmitRequest;
import com.nks.leetcodehelper.controller.dto.SubmitResponse;
import com.nks.leetcodehelper.model.SubmissionResult;
import com.nks.leetcodehelper.service.ProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LeetCodeController {

    private final ProblemService problemService;

    public LeetCodeController(ProblemService problemService) {
        this.problemService = problemService;
    }

    /**
     * 솔루션 파일 제출.
     * IntelliJ External Tool 설정:
     *   Program: curl
     *   Arguments: -s -X POST http://localhost:8080/api/submit
     *              -H "Content-Type: application/json"
     *              -d "{\"filePath\": \"$FilePath$\"}"
     */
    @PostMapping("/submit")
    public ResponseEntity<SubmitResponse> submit(@RequestBody SubmitRequest request) {
        try {
            SubmissionResult result = problemService.submitSolution(request.filePath());
            return ResponseEntity.ok(new SubmitResponse(
                    result.statusMsg(),
                    result.accepted(),
                    result.runtime(),
                    result.memory(),
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new SubmitResponse("Error", false, null, null, e.getMessage()));
        }
    }

    /** 오늘의 일일 문제를 즉시 가져옵니다. */
    @PostMapping("/problem/daily")
    public ResponseEntity<String> fetchDaily() {
        try {
            Path dir = problemService.fetchAndSaveDailyProblem();
            return ResponseEntity.ok("Saved: " + dir);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /** 로컬에 없는 모든 문제를 LeetCode에서 가져와 저장합니다. */
    @PostMapping("/problem/all")
    public ResponseEntity<String> fetchAllMissing() {
        try {
            List<Path> saved = problemService.fetchAndSaveAllMissing();
            return ResponseEntity.ok("Saved " + saved.size() + " new problems.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /** 특정 문제를 slug로 가져옵니다. (예: two-sum, longest-substring-without-repeating-characters) */
    @PostMapping("/problem/{slug}")
    public ResponseEntity<String> fetchBySlug(@PathVariable String slug) {
        try {
            Path dir = problemService.fetchAndSaveProblem(slug);
            return ResponseEntity.ok("Saved: " + dir);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
