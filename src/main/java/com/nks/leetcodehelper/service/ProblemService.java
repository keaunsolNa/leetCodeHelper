package com.nks.leetcodehelper.service;

import com.nks.leetcodehelper.model.Problem;
import com.nks.leetcodehelper.model.SubmissionResult;
import com.nks.leetcodehelper.service.leetcode.LeetCodeApiClient;
import com.nks.leetcodehelper.service.leetcode.LeetCodeSubmitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ProblemService {

    private static final Logger log = LoggerFactory.getLogger(ProblemService.class);

    private final LeetCodeApiClient apiClient;
    private final LeetCodeSubmitService submitService;
    private final MarkdownService markdownService;
    private final GitService gitService;

    public ProblemService(LeetCodeApiClient apiClient, LeetCodeSubmitService submitService,
                          MarkdownService markdownService, GitService gitService) {
        this.apiClient = apiClient;
        this.submitService = submitService;
        this.markdownService = markdownService;
        this.gitService = gitService;
    }

    public Path fetchAndSaveDailyProblem() throws IOException {
        log.info("Fetching today's daily problem...");
        Problem problem = apiClient.fetchDailyProblem();
        Path dir = markdownService.saveProblem(problem);
        log.info("Saved: #{} {} -> {}", problem.id(), problem.title(), dir);
        return dir;
    }

    public List<Path> fetchAndSaveAllMissing() throws IOException, InterruptedException {
        log.info("Fetching full problem list from LeetCode...");
        List<LeetCodeApiClient.ProblemSummary> all = apiClient.fetchAllProblems();
        Set<String> existing = markdownService.collectExistingProblemIds();
        log.info("Total: {} problems, already saved: {}", all.size(), existing.size());

        List<Path> saved = new ArrayList<>();
        for (LeetCodeApiClient.ProblemSummary summary : all) {
            if (existing.contains(summary.id())) continue;
            try {
                Thread.sleep(300);
                Problem problem = apiClient.fetchProblemBySlug(summary.titleSlug());
                Path dir = markdownService.saveProblem(problem);
                saved.add(dir);
                log.info("[{} saved] #{} {}", saved.size(), problem.id(), problem.title());
            } catch (Exception e) {
                log.warn("Skip #{} {}: {}", summary.id(), summary.titleSlug(), e.getMessage());
            }
        }
        log.info("Done. {} new problems saved.", saved.size());
        return saved;
    }

    public Path fetchAndSaveProblem(String slug) throws IOException {
        log.info("Fetching problem: {}", slug);
        Problem problem = apiClient.fetchProblemBySlug(slug);
        Path dir = markdownService.saveProblem(problem);
        log.info("Saved: #{} {} -> {}", problem.id(), problem.title(), dir);
        return dir;
    }

    /**
     * 솔루션 파일을 LeetCode에 제출합니다.
     * Accepted일 경우 파일을 Solved/로 이동하고 Git에 푸시합니다.
     *
     * @param filePath 제출할 Solution.java (또는 해당 언어 파일)의 절대 경로
     */
    public SubmissionResult submitSolution(String filePath) throws IOException, InterruptedException {
        Path solutionFile = Paths.get(filePath);

        MarkdownService.ProblemFileInfo info = markdownService.readProblemInfo(solutionFile)
                .orElseThrow(() -> new IllegalArgumentException(
                        "problem.md를 찾을 수 없습니다. 파일이 UnSolved/{id}-{slug}/ 하위에 있는지 확인하세요: " + filePath));

        log.info("Submitting problem #{} ({})", info.id(), info.slug());
        SubmissionResult result = submitService.submit(info.slug(), info.id(), info.code());

        if (result.accepted()) {
            log.info("Accepted! Runtime: {}, Memory: {}", result.runtime(), result.memory());
            Path solvedDir = gitService.moveProblemToSolved(info.directory(), info.difficultyDir());

            String commitMsg = String.format("solve: #%s %s | runtime: %s, memory: %s",
                    info.id(), info.slug(), result.runtime(), result.memory());
            gitService.commitAndPush(commitMsg);
            log.info("Committed and pushed to Solved: {}", solvedDir);
        } else {
            log.warn("Not accepted: {} (status code: {})", result.statusMsg(), result.statusCode());
        }

        return result;
    }
}
