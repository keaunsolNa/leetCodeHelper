package com.nks.leetcodehelper.scheduler;

import com.nks.leetcodehelper.service.ProblemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProblemFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProblemFetchScheduler.class);

    private final ProblemService problemService;

    public ProblemFetchScheduler(ProblemService problemService) {
        this.problemService = problemService;
    }

    @Scheduled(cron = "${leetcode.schedule-cron}")
    public void fetchDailyProblem() {
        log.info("Scheduler triggered: fetching daily LeetCode problem");
        try {
            problemService.fetchAndSaveDailyProblem();
        } catch (Exception e) {
            log.error("Failed to fetch daily problem", e);
        }
    }

    @Scheduled(cron = "${leetcode.all-fetch-cron}")
    public void fetchAllMissingProblems() {
        log.info("Scheduler triggered: fetching all missing LeetCode problems");
        try {
            problemService.fetchAndSaveAllMissing();
        } catch (Exception e) {
            log.error("Failed to fetch all missing problems", e);
        }
    }
}
