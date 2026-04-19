package com.nks.leetcodehelper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeetCodeHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeetCodeHelperApplication.class, args);
    }
}
