package com.nks.leetcodehelper.service;

import com.nks.leetcodehelper.config.LeetCodeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);

    private final LeetCodeProperties props;

    public GitService(LeetCodeProperties props) {
        this.props = props;
    }

    /**
     * UnSolved/{dir} → Solved/{dir} 로 디렉터리를 이동합니다.
     */
    public Path moveProblemToSolved(Path problemDir, String difficultyDir) throws IOException {
        Path solvedBase = Paths.get(props.getRepoPath(), props.getLeetcodeDir(), props.getSolvedDir(), difficultyDir);
        Files.createDirectories(solvedBase);

        Path target = solvedBase.resolve(problemDir.getFileName());
        if (!Files.exists(target)) {
            Files.move(problemDir, target);
            log.info("Moved {} -> {}", problemDir, target);
        }
        return target;
    }

    public void commitAndPush(String message) throws IOException, InterruptedException {
        Path repoPath = Paths.get(props.getRepoPath());

        if (!props.getGitUsername().isBlank()) {
            runGit(repoPath, "config", "user.name", props.getGitUsername());
        }
        if (!props.getGitEmail().isBlank()) {
            runGit(repoPath, "config", "user.email", props.getGitEmail());
        }

        runGit(repoPath, "add", ".");

        // 커밋할 변경사항이 없으면 종료
        int diffCode = runGitGetCode(repoPath, "diff", "--staged", "--quiet");
        if (diffCode == 0) {
            log.info("Nothing to commit");
            return;
        }

        runGit(repoPath, "commit", "-m", message);
        runGit(repoPath, "push");
        log.info("Pushed: {}", message);
    }

    private void runGit(Path workDir, String... args) throws IOException, InterruptedException {
        int code = runGitGetCode(workDir, args);
        if (code != 0) {
            throw new RuntimeException("git " + String.join(" ", args) + " failed (exit code " + code + ")");
        }
    }

    private int runGitGetCode(Path workDir, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));

        Process process = new ProcessBuilder(cmd)
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start();

        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (!output.isBlank()) {
            log.debug("git {}: {}", String.join(" ", args), output.trim());
        }
        return exitCode;
    }
}
