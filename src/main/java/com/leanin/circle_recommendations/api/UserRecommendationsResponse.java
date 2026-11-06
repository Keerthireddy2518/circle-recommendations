package com.leanin.circle_recommendations.api;

import java.util.List;

public record UserRecommendationsResponse(
        Long userId,
        List<RecommendationDto> recommendations
) {
}
