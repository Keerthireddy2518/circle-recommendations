package com.leanin.circle_recommendations.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.leanin.circle_recommendations.domain.entity.Circle;
import com.leanin.circle_recommendations.domain.entity.Topic;
import com.leanin.circle_recommendations.domain.entity.User;

@Component
public class CircleRecommendationScorer {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "as", "at", "be", "by", "for", "from", "in", "into",
            "is", "it", "of", "on", "or", "our", "the", "their", "this", "to", "with",
            "women", "men", "people", "support", "group", "circle", "community"
    );

    private final RecommendationScoringProperties properties;

    public CircleRecommendationScorer(RecommendationScoringProperties properties) {
        this.properties = properties;
    }

    public RecommendationScore score(
            User user,
            Circle circle,
            Set<String> userInterestTags,
            int memberCount,
            int recentCircleActivityCount,
            int recentUserCircleActivityCount,
            Instant lastActivityAt,
            int maxMembersPerCircle,
            boolean coldStartUser,
            boolean isFull,
            boolean isInactive
    ) {
        if (isFull) {
            return new RecommendationScore(0.0, "Excluded: this circle is full.", Map.of("full", 0.0));
        }
        if (isInactive) {
            return new RecommendationScore(0.0, "Excluded: this circle has been inactive for more than 90 days.", Map.of("inactive", 0.0));
        }

        Set<String> normalizedUserTags = normalizeTags(userInterestTags);
        if (normalizedUserTags.isEmpty() && user != null) {
            normalizedUserTags = extractUserInterestTags(user);
        }

        Set<String> normalizedCircleTags = extractCircleTags(circle);
        Set<String> overlap = new HashSet<>(normalizedUserTags);
        overlap.retainAll(normalizedCircleTags);

        double topicSimilarity = normalizedUserTags.isEmpty() || normalizedCircleTags.isEmpty()
                ? 0.0
                : calculateJaccard(normalizedUserTags, normalizedCircleTags);

        double topicScore = 0.0;
        StringBuilder explanationBuilder = new StringBuilder();
        Map<String, Double> breakdown = new LinkedHashMap<>();

        if (!normalizedUserTags.isEmpty() && !normalizedCircleTags.isEmpty()) {
            topicScore = properties.getTopicMatchWeight() * topicSimilarity;
            breakdown.put("topic_match", topicScore);
            explanationBuilder.append("Topic match: ")
                    .append(overlap.isEmpty() ? "no shared interest tags" : "matched ")
                    .append(overlap.isEmpty() ? "" : String.join(", ", sorted(overlap)))
                    .append(".");
        } else if (coldStartUser) {
            breakdown.put("cold_start_fallback", 0.0);
            explanationBuilder.append("Cold-start fallback: no explicit interests or memberships were available.");
        }

        double activityScore = 0.0;
        if (recentCircleActivityCount > 0) {
            activityScore = properties.getActivityWeight() * Math.min(1.0, recentCircleActivityCount / 5.0);
            breakdown.put("circle_activity", activityScore);
            if (explanationBuilder.length() > 0) {
                explanationBuilder.append(" ");
            }
            explanationBuilder.append("Recent activity: ")
                    .append(recentCircleActivityCount)
                    .append(" active engagement events in the last 30 days.");
        }

        double engagementScore = 0.0;
        if (recentUserCircleActivityCount > 0) {
            engagementScore = properties.getEngagementWeight() * Math.min(1.0, recentUserCircleActivityCount / 3.0);
            breakdown.put("user_engagement", engagementScore);
            if (explanationBuilder.length() > 0) {
                explanationBuilder.append(" ");
            }
            explanationBuilder.append("Your recent engagement: ")
                    .append(recentUserCircleActivityCount)
                    .append(" relevant interaction(s) tied to this circle.");
        }

        double recencyScore = 0.0;
        if (lastActivityAt != null) {
            long daysSinceLastActivity = Math.max(0, Duration.between(lastActivityAt, Instant.now()).toDays());
            double recencyValue = 1.0 - Math.min(1.0, daysSinceLastActivity / 90.0);
            recencyScore = properties.getRecencyWeight() * recencyValue;
            breakdown.put("recency", recencyScore);
            if (explanationBuilder.length() > 0) {
                explanationBuilder.append(" ");
            }
            explanationBuilder.append("Recency signal: last activity was ")
                    .append(daysSinceLastActivity)
                    .append(" days ago.");
        }

        double popularityScore = 0.0;
        if (memberCount > 0) {
            double popularityValue = Math.min(1.0, memberCount / (double) maxMembersPerCircle);
            popularityScore = properties.getPopularityWeight() * popularityValue;
            breakdown.put("popularity", popularityScore);
        }

        double totalScore = topicScore + activityScore + engagementScore + recencyScore + popularityScore;

        if (coldStartUser && normalizedUserTags.isEmpty()) {
            double coldStartBoost = properties.getColdStartWeight() * (0.6 * Math.min(1.0, recentCircleActivityCount / 5.0) + 0.4 * Math.min(1.0, memberCount / (double) maxMembersPerCircle));
            totalScore += coldStartBoost;
            breakdown.put("cold_start_boost", coldStartBoost);
            if (explanationBuilder.length() > 0) {
                explanationBuilder.append(" ");
            }
            explanationBuilder.append("Cold-start boost in effect because there is no topic history yet.");
        }

        totalScore = Math.min(1.0, totalScore);
        if (totalScore <= 0.0) {
            explanationBuilder.append(" No meaningful match signals were found.");
        }

        if (explanationBuilder.length() == 0) {
            explanationBuilder.append("This circle matches your interests and recent engagement patterns.");
        }

        return new RecommendationScore(totalScore, explanationBuilder.toString().trim(), breakdown);
    }

    public double calculateJaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() && right.isEmpty()) {
            return 0.0;
        }
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        if (union.isEmpty()) {
            return 0.0;
        }
        return (double) intersection.size() / union.size();
    }

    public Set<String> normalizeTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> normalized = new HashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String cleaned = tag.trim().toLowerCase(Locale.ROOT);
            if (!cleaned.isEmpty()) {
                normalized.add(cleaned);
            }
        }
        return normalized;
    }

    public Set<String> extractCircleTags(Circle circle) {
        Set<String> tags = new HashSet<>();
        if (circle == null) {
            return tags;
        }

        List<String> sources = new ArrayList<>();
        if (StringUtils.hasText(circle.getName())) {
            sources.add(circle.getName());
        }
        if (StringUtils.hasText(circle.getDescription())) {
            sources.add(circle.getDescription());
        }

        for (String source : sources) {
            for (String fragment : source.split("[^a-zA-Z0-9]+")) {
                String normalized = normalizeTag(fragment);
                if (!normalized.isEmpty()) {
                    tags.add(normalized);
                }
            }
        }

        for (Topic topic : circle.getTopics()) {
            if (topic != null && topic.getName() != null) {
                String normalized = normalizeTag(topic.getName());
                if (!normalized.isEmpty()) {
                    tags.add(normalized);
                }
            }
        }
        return tags;
    }

    private String normalizeTag(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = value.trim().toLowerCase(Locale.ROOT);
        if (cleaned.isEmpty() || cleaned.length() < 2 || STOP_WORDS.contains(cleaned)) {
            return "";
        }
        return cleaned;
    }

    public Set<String> extractUserInterestTags(User user) {
        if (user == null) {
            return Collections.emptySet();
        }
        Set<String> tags = new HashSet<>();
        for (Topic topic : user.getTopics()) {
            if (topic != null && topic.getName() != null) {
                tags.add(topic.getName().toLowerCase(Locale.ROOT));
            }
        }
        return tags;
    }

    private List<String> sorted(Set<String> values) {
        List<String> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted;
    }
}
