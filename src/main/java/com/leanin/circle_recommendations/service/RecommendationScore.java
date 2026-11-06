package com.leanin.circle_recommendations.service;

import java.util.Map;

public record RecommendationScore(double totalScore, String explanation, Map<String, Double> breakdown) {
}
