package com.leanin.circle_recommendations.api;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
