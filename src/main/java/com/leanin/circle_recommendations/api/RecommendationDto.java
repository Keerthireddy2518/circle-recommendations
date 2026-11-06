package com.leanin.circle_recommendations.api;

import java.util.List;

public record RecommendationDto(
        Long circleId,
        String name,
        double score,
        List<String> reasons
) {
}
