package com.nks.leetcodehelper.service;

import com.nks.leetcodehelper.config.LeetCodeProperties;
import com.nks.leetcodehelper.model.Problem;
import com.nks.leetcodehelper.model.SubmissionResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarkdownService {

    private static final Map<String, String> LANG_EXTENSIONS = Map.ofEntries(
            // Algorithm languages
            Map.entry("java", "java"),
            Map.entry("python3", "py"),
            Map.entry("python", "py"),
            Map.entry("c", "c"),
            Map.entry("cpp", "cpp"),
            Map.entry("csharp", "cs"),
            Map.entry("javascript", "js"),
            Map.entry("typescript", "ts"),
            Map.entry("golang", "go"),
            Map.entry("kotlin", "kt"),
            Map.entry("rust", "rs"),
            Map.entry("swift", "swift"),
            Map.entry("scala", "scala"),
            Map.entry("ruby", "rb"),
            Map.entry("php", "php"),
            Map.entry("racket", "rkt"),
            Map.entry("erlang", "erl"),
            Map.entry("elixir", "ex"),
            Map.entry("dart", "dart"),
            Map.entry("bash", "sh"),
            // SQL languages
            Map.entry("mysql", "sql"),
            Map.entry("mssql", "sql"),
            Map.entry("oraclesql", "sql"),
            Map.entry("pythondata", "py")
    );

    private final LeetCodeProperties props;
    private final HtmlToMarkdownConverter htmlConverter;

    public MarkdownService(LeetCodeProperties props, HtmlToMarkdownConverter htmlConverter) {
        this.props = props;
        this.htmlConverter = htmlConverter;
    }

    /**
     * 문제를 UnSolved/{id}-{slug}/ 디렉터리에 저장합니다.
     * - problem.md : 문제 설명
     * - Solution.{ext} : 스타터 코드 템플릿
     */
    static String toDifficultyDir(String difficulty) {
        return switch (difficulty) {
            case "Medium" -> "Med";
            case "Hard" -> "Hard";
            default -> "Easy";
        };
    }

    public Path saveProblem(Problem problem) throws IOException {
        String diffDir = toDifficultyDir(problem.difficulty());
        String dirName = String.format("%04d-%s", Integer.parseInt(problem.id()), problem.titleSlug());
        Path dir = Paths.get(props.getRepoPath(), props.getLeetcodeDir(), props.getUnsolvedDir(), diffDir, dirName);
        Files.createDirectories(dir);

        Path mdFile = dir.resolve("problem.md");
        if (!Files.exists(mdFile)) {
            Files.writeString(mdFile, buildMarkdown(problem));
        }

        String ext = LANG_EXTENSIONS.getOrDefault(problem.langSlug(), "txt");
        Path solutionFile = dir.resolve("Solution." + ext);
        if (!Files.exists(solutionFile)) {
            Files.writeString(solutionFile, problem.starterCode());
        }

        return dir;
    }

    /**
     * 솔루션 파일로부터 문제 정보와 코드를 읽어 반환합니다.
     * 솔루션 파일은 반드시 problem.md 와 같은 디렉터리에 있어야 합니다.
     */
    public Optional<ProblemFileInfo> readProblemInfo(Path solutionFile) throws IOException {
        Path dir = solutionFile.getParent();
        if (dir == null) return Optional.empty();

        Path mdFile = dir.resolve("problem.md");
        if (!Files.exists(mdFile)) return Optional.empty();

        String mdContent = Files.readString(mdFile);
        String id = extractFrontmatter(mdContent, "id");
        String internalId = extractFrontmatter(mdContent, "internal_id");
        String slug = extractFrontmatter(mdContent, "slug");
        String difficulty = extractFrontmatter(mdContent, "difficulty");
        String lang = extractFrontmatter(mdContent, "lang");
        if (id == null || slug == null || difficulty == null) return Optional.empty();

        String effectiveInternalId = (internalId != null) ? internalId : id;
        String code = Files.readString(solutionFile);
        return Optional.of(new ProblemFileInfo(id, effectiveInternalId, slug, lang, code, dir, toDifficultyDir(difficulty)));
    }

    public Set<String> collectExistingProblemIds() throws IOException {
        Set<String> ids = new HashSet<>();
        Path base = Paths.get(props.getRepoPath(), props.getLeetcodeDir());
        for (String statusDir : List.of(props.getUnsolvedDir(), props.getSolvedDir())) {
            for (String diffDir : List.of("Easy", "Med", "Hard")) {
                Path dir = base.resolve(statusDir).resolve(diffDir);
                if (!Files.exists(dir)) continue;
                try (var stream = Files.list(dir)) {
                    stream.filter(Files::isDirectory)
                          .map(p -> p.getFileName().toString().split("-")[0])
                          .map(s -> String.valueOf(Integer.parseInt(s)))
                          .forEach(ids::add);
                }
            }
        }
        return ids;
    }

    private String buildMarkdown(Problem problem) {
        String tags = String.join(", ", problem.tags());
        String content = htmlConverter.convert(problem.content());

        return """
                ---
                id: %s
                internal_id: %s
                slug: %s
                title: %s
                difficulty: %s
                tags: %s
                date: %s
                lang: %s
                ---

                # %s. %s

                **Difficulty:** %s | **Tags:** %s

                ## Description

                %s
                """.formatted(
                problem.id(), problem.internalId(), problem.titleSlug(), problem.title(),
                problem.difficulty(), tags, LocalDate.now(), problem.langSlug(),
                problem.id(), problem.title(), problem.difficulty(), tags,
                content
        );
    }

    public void saveAnalysis(Path solvedDir, ProblemFileInfo info, SubmissionResult result, String aiReview) throws IOException {
        String submittedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String langDisplay = info.lang() != null ? info.lang() : "N/A";
        String content = """
                # Analysis

                | Item | Value |
                |------|-------|
                | Submitted | %s |
                | Language | %s |
                | Runtime | %s (Beats %.1f%%) |
                | Memory | %s (Beats %.1f%%) |

                ## Submission

                [View on LeetCode](https://leetcode.com/problems/%s/submissions/)

                ## Code Review

                %s
                """.formatted(
                submittedAt,
                langDisplay,
                result.runtime(), result.runtimePercentile(),
                result.memory(), result.memoryPercentile(),
                info.slug(),
                aiReview
        );
        Files.writeString(solvedDir.resolve("analysis.md"), content);
    }

    private String extractFrontmatter(String content, String key) {
        Matcher matcher = Pattern.compile("^" + key + ":\\s*(.+)$", Pattern.MULTILINE).matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    public record ProblemFileInfo(String id, String internalId, String slug, String lang, String code, Path directory, String difficultyDir) {}
}
