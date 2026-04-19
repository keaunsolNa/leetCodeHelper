package com.nks.leetcodehelper.model;

public record SubmissionResult(
        String state,
        String statusMsg,
        boolean accepted,
        String runtime,
        String memory,
        int statusCode
) {}
