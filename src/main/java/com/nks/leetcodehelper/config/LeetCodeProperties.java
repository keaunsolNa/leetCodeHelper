package com.nks.leetcodehelper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "leetcode")
public class LeetCodeProperties {

    private String session = "";
    private String csrfToken = "";
    private String algLanguage = "java";
    private String sqlLanguage = "mysql";
    private String repoPath = ".";
    private String leetcodeDir = "leetcode";
    private String unsolvedDir = "UnSolved";
    private String solvedDir = "Solved";
    private String gitUsername = "";
    private String gitEmail = "";
    private String scheduleCron = "0 0 9 * * MON-FRI";
    private String allFetchCron = "0 0 6 * * MON";

    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }

    public String getCsrfToken() { return csrfToken; }
    public void setCsrfToken(String csrfToken) { this.csrfToken = csrfToken; }

    public String getAlgLanguage() { return algLanguage; }
    public void setAlgLanguage(String algLanguage) { this.algLanguage = algLanguage; }

    public String getSqlLanguage() { return sqlLanguage; }
    public void setSqlLanguage(String sqlLanguage) { this.sqlLanguage = sqlLanguage; }

    public String getRepoPath() { return repoPath; }
    public void setRepoPath(String repoPath) { this.repoPath = repoPath; }

    public String getLeetcodeDir() { return leetcodeDir; }
    public void setLeetcodeDir(String leetcodeDir) { this.leetcodeDir = leetcodeDir; }

    public String getUnsolvedDir() { return unsolvedDir; }
    public void setUnsolvedDir(String unsolvedDir) { this.unsolvedDir = unsolvedDir; }

    public String getSolvedDir() { return solvedDir; }
    public void setSolvedDir(String solvedDir) { this.solvedDir = solvedDir; }

    public String getGitUsername() { return gitUsername; }
    public void setGitUsername(String gitUsername) { this.gitUsername = gitUsername; }

    public String getGitEmail() { return gitEmail; }
    public void setGitEmail(String gitEmail) { this.gitEmail = gitEmail; }

    public String getScheduleCron() { return scheduleCron; }
    public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }

    public String getAllFetchCron() { return allFetchCron; }
    public void setAllFetchCron(String allFetchCron) { this.allFetchCron = allFetchCron; }
}
