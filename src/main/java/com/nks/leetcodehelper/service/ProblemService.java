package com.nks.leetcodehelper.service;

import com.nks.leetcodehelper.config.LeetCodeProperties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.nks.leetcodehelper.model.Problem;
import com.nks.leetcodehelper.model.SubmissionResult;
import com.nks.leetcodehelper.service.leetcode.LeetCodeApiClient;
import com.nks.leetcodehelper.service.leetcode.LeetCodeSubmitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
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
    private final LeetCodeProperties props;
    private final CodeAnalysisService codeAnalysisService;

    public ProblemService(LeetCodeApiClient apiClient, LeetCodeSubmitService submitService,
                          MarkdownService markdownService, GitService gitService,
                          LeetCodeProperties props, CodeAnalysisService codeAnalysisService) {
        this.apiClient = apiClient;
        this.submitService = submitService;
        this.markdownService = markdownService;
        this.gitService = gitService;
        this.props = props;
        this.codeAnalysisService = codeAnalysisService;
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

    public SubmissionResult submitSolution(String filePath) throws IOException, InterruptedException {
        Path solutionFile = Paths.get(filePath);

        MarkdownService.ProblemFileInfo info = markdownService.readProblemInfo(solutionFile)
                .orElseThrow(() -> new IllegalArgumentException(
                        "problem.md를 찾을 수 없습니다. 파일이 UnSolved/{id}-{slug}/ 하위에 있는지 확인하세요: " + filePath));

        String lang = info.lang() != null ? info.lang()
                : (info.slug().contains("sql") ? props.getSqlLanguage() : props.getAlgLanguage());
        log.info("Submitting problem #{} (internal: {}, slug: {}, lang: {})", info.id(), info.internalId(), info.slug(), lang);
        SubmissionResult result = submitService.submit(info.slug(), info.internalId(), lang, info.code());

        if (result.accepted()) {
            log.info("Accepted! Runtime: {} (beats {}%), Memory: {} (beats {}%)",
                    result.runtime(), String.format("%.1f", result.runtimePercentile()),
                    result.memory(), String.format("%.1f", result.memoryPercentile()));
            Path solvedDir = gitService.moveProblemToSolved(info.directory(), info.difficultyDir());

            log.info("Requesting AI code review...");
            String mdContent = Files.readString(solvedDir.resolve("problem.md"));
            String difficulty = extractFrontmatter(mdContent, "difficulty");
            String tags = extractFrontmatter(mdContent, "tags");
            String aiReview = codeAnalysisService.analyze(info.slug(), difficulty, tags, lang, info.code());
            markdownService.saveAnalysis(solvedDir, info, result, aiReview);

            String commitMsg = String.format("solve: #%s %s | runtime: %s (%.1f%%), memory: %s (%.1f%%)",
                    info.id(), info.slug(),
                    result.runtime(), result.runtimePercentile(),
                    result.memory(), result.memoryPercentile());
            gitService.commitAndPush(commitMsg);
            log.info("Committed and pushed to Solved: {}", solvedDir);
        } else {
            log.warn("Not accepted: {} (status code: {})", result.statusMsg(), result.statusCode());
        }

        return result;
    }

    private String extractFrontmatter(String content, String key) {
        Matcher matcher = Pattern.compile("^" + key + ":\\s*(.+)$", Pattern.MULTILINE).matcher(content);
        return matcher.find() ? matcher.group(1).trim() : "N/A";
    }
}
