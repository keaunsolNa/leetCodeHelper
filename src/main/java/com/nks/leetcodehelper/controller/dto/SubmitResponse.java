package com.nks.leetcodehelper.controller.dto;

public record SubmitResponse(
        String status,
        boolean accepted,
        String runtime,
        String memory,
        String error
) {}
