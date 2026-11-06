package com.leanin.circle_recommendations.api;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.leanin.circle_recommendations.service.CircleRecommendationService;

@RestController
@RequestMapping("/api/v1")
public class UserRecommendationsController {

    private final CircleRecommendationService recommendationService;

    public UserRecommendationsController(CircleRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/users/{userId}/recommendations")
    public ResponseEntity<UserRecommendationsResponse> getRecommendations(
            @PathVariable Long userId,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }

        List<RecommendationDto> recommendations = (limit == null
                ? recommendationService.recommendForUser(userId)
                : recommendationService.recommendForUser(userId, limit)
        ).stream()
                .map(circle -> new RecommendationDto(
                        circle.circleId(),
                        circle.name(),
                        circle.score(),
                        toReasons(circle.explanation())
                ))
                .toList();

        return ResponseEntity.ok(new UserRecommendationsResponse(userId, recommendations));
    }

    private List<String> toReasons(String explanation) {
        if (explanation == null || explanation.isBlank()) {
            return List.of();
        }
        return Arrays.stream(explanation.split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(reason -> !reason.isEmpty())
                .toList();
    }
}
