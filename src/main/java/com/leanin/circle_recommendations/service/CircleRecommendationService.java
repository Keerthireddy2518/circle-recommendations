package com.leanin.circle_recommendations.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.leanin.circle_recommendations.domain.entity.Circle;
import com.leanin.circle_recommendations.domain.entity.Membership;
import com.leanin.circle_recommendations.domain.entity.User;
import com.leanin.circle_recommendations.domain.entity.UserActivity;
import com.leanin.circle_recommendations.domain.repository.CircleRepository;
import com.leanin.circle_recommendations.domain.repository.MembershipRepository;
import com.leanin.circle_recommendations.domain.repository.UserActivityRepository;
import com.leanin.circle_recommendations.domain.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CircleRecommendationService {

    private final UserRepository userRepository;
    private final CircleRepository circleRepository;
    private final MembershipRepository membershipRepository;
    private final UserActivityRepository userActivityRepository;
    private final CircleRecommendationScorer scorer;
    private final RecommendationScoringProperties properties;

    public CircleRecommendationService(
            UserRepository userRepository,
            CircleRepository circleRepository,
            MembershipRepository membershipRepository,
            UserActivityRepository userActivityRepository,
            CircleRecommendationScorer scorer,
            RecommendationScoringProperties properties
    ) {
        this.userRepository = userRepository;
        this.circleRepository = circleRepository;
        this.membershipRepository = membershipRepository;
        this.userActivityRepository = userActivityRepository;
        this.scorer = scorer;
        this.properties = properties;
    }

    public List<RecommendedCircle> recommendForUser(Long userId) {
        return recommendForUser(userId, properties.getMaxResults());
    }

    public List<RecommendedCircle> recommendForUser(Long userId, int limit) {
        User user = userRepository.findWithTopicsById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Set<Long> existingMembershipCircleIds = membershipRepository.findByUserIdAndStatus(userId, Membership.Status.ACTIVE)
                .stream()
                .map(membership -> membership.getCircle().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<Circle> candidateCircles = circleRepository.findByActiveTrue()
                .stream()
                .filter(circle -> !existingMembershipCircleIds.contains(circle.getId()))
                .filter(circle -> !isCircleFull(circle.getId()))
                .filter(circle -> !isCircleInactive(circle.getId()))
                .toList();

        if (candidateCircles.isEmpty()) {
            return List.of();
        }

        List<RecommendedCircle> scoredCandidates = new ArrayList<>();
        Set<String> userInterestTags = scorer.extractUserInterestTags(user);
        boolean coldStartUser = userInterestTags.isEmpty() && membershipRepository.findByUserId(userId).isEmpty();

        for (Circle circle : candidateCircles) {
            Set<String> circleTags = scorer.extractCircleTags(circle);
            int memberCount = membershipRepository.findByCircleId(circle.getId()).stream()
                    .filter(membership -> membership.getStatus() == Membership.Status.ACTIVE)
                    .toList()
                    .size();
            int recentCircleActivityCount = userActivityRepository.findByCircleIdOrderByEventAtDesc(circle.getId()).stream()
                    .filter(activity -> Duration.between(activity.getEventAt(), Instant.now()).toDays() <= properties.getRecentActivityWindowDays())
                    .toList()
                    .size();
            int recentUserCircleActivityCount = userActivityRepository.findByUserIdOrderByEventAtDesc(userId).stream()
                    .filter(activity -> activity.getCircle() != null && activity.getCircle().getId().equals(circle.getId()))
                    .toList()
                    .size();
            Instant lastActivityAt = userActivityRepository.findByCircleIdOrderByEventAtDesc(circle.getId()).stream()
                    .findFirst()
                    .map(UserActivity::getEventAt)
                    .orElse(circle.getCreatedAt());

            RecommendationScore score = scorer.score(
                    user,
                    circle,
                    userInterestTags,
                    memberCount,
                    recentCircleActivityCount,
                    recentUserCircleActivityCount,
                    lastActivityAt,
                    properties.getMaxMembersPerCircle(),
                    coldStartUser,
                    memberCount >= properties.getMaxMembersPerCircle(),
                    isCircleInactive(circle.getId())
            );

            if (score.totalScore() <= 0.0) {
                continue;
            }

            String explanation = score.explanation();
            scoredCandidates.add(new RecommendedCircle(circle.getId(), circle.getName(), score.totalScore(), explanation, circleTags, memberCount));
        }

        List<RecommendedCircle> deduplicated = deduplicateNearIdenticalCircles(scoredCandidates);
        deduplicated.sort(Comparator.comparingDouble(RecommendedCircle::score).reversed());

        return deduplicated.stream().limit(Math.max(1, limit)).toList();
    }

    private boolean isCircleFull(Long circleId) {
        long activeMembers = membershipRepository.findByCircleId(circleId).stream()
                .filter(m -> m.getStatus() == Membership.Status.ACTIVE)
                .count();
        return activeMembers >= properties.getMaxMembersPerCircle();
    }

    private boolean isCircleInactive(Long circleId) {
        List<UserActivity> circleActivities = userActivityRepository.findByCircleIdOrderByEventAtDesc(circleId);
        if (circleActivities.isEmpty()) {
            return false;
        }
        Instant latestActivity = circleActivities.getFirst().getEventAt();
        return Duration.between(latestActivity, Instant.now()).toDays() > properties.getInactiveDaysThreshold();
    }

    private List<RecommendedCircle> deduplicateNearIdenticalCircles(List<RecommendedCircle> candidates) {
        List<RecommendedCircle> result = new ArrayList<>();
        for (RecommendedCircle current : candidates) {
            boolean duplicate = false;
            for (RecommendedCircle existing : result) {
                double similarity = calculateTagSimilarity(existing.tags(), current.tags());
                if (similarity >= properties.getNearDuplicateSimilarityThreshold()) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                result.add(current);
            }
        }
        return result;
    }

    private double calculateTagSimilarity(Set<String> left, Set<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        if (union.isEmpty()) {
            return 0.0;
        }
        return (double) intersection.size() / union.size();
    }

    public record RecommendedCircle(
            Long circleId,
            String name,
            double score,
            String explanation,
            Set<String> tags,
            int memberCount
    ) {
    }
}
