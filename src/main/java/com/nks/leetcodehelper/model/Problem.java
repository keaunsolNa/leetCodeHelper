package com.nks.leetcodehelper.model;

import java.util.List;

public record Problem(
        String id,
        String internalId,
        String title,
        String titleSlug,
        String difficulty,
        List<String> tags,
        String content,
        String starterCode,
        String langSlug
) {}
