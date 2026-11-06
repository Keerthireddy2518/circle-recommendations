package com.leanin.circle_recommendations.service;

import com.leanin.circle_recommendations.domain.entity.Circle;
import com.leanin.circle_recommendations.domain.entity.Topic;
import com.leanin.circle_recommendations.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircleRecommendationScorerTest {

    private final RecommendationScoringProperties properties = new RecommendationScoringProperties();
    private final CircleRecommendationScorer scorer = new CircleRecommendationScorer(properties);

    @Test
    void shouldScoreTopicMatchesHighly() {
        User user = new User("User A", "user@example.com");
        user.getTopics().add(new Topic("Leadership", "Career"));
        user.getTopics().add(new Topic("Mentorship", "Career"));

        Circle circle = new Circle("Women in Leadership", "Leadership and mentorship support");
        circle.getTopics().add(new Topic("Leadership", "Career"));
        circle.getTopics().add(new Topic("Mentorship", "Career"));
        circle.getTopics().add(new Topic("Networking", "Career"));

        RecommendationScore score = scorer.score(
                user,
                circle,
                Set.of(),
                4,
                2,
                1,
                Instant.now(),
                30,
                false,
                false,
                false
        );

        assertTrue(score.totalScore() > 0.5);
        assertFalse(score.explanation().isBlank());
        assertTrue(score.explanation().toLowerCase().contains("leadership") || score.explanation().toLowerCase().contains("mentorship"));
    }

    @Test
    void shouldReturnLowScoreForColdStartUsers() {
        User user = new User("New User", "new@example.com");
        Circle circle = new Circle("Wellness Circle", "A supportive wellness group");
        circle.getTopics().add(new Topic("Wellness", "Health"));

        RecommendationScore score = scorer.score(
                user,
                circle,
                Set.of(),
                3,
                1,
                0,
                Instant.now(),
                14,
                true,
                false,
                false
        );

        assertTrue(score.totalScore() >= 0.0);
        assertTrue(score.explanation().toLowerCase().contains("cold"));
    }

    @Test
    void shouldMarkDuplicateCirclesAsLowerPriority() {
        User user = new User("User", "user2@example.com");
        user.getTopics().add(new Topic("Leadership", "Career"));

        Circle first = new Circle("Leadership Circle", "Leadership support");
        first.getTopics().add(new Topic("Leadership", "Career"));
        first.getTopics().add(new Topic("Mentorship", "Career"));

        Circle second = new Circle("Leadership Mentorship", "Leadership mentorship");
        second.getTopics().add(new Topic("Leadership", "Career"));
        second.getTopics().add(new Topic("Mentorship", "Career"));

        RecommendationScore firstScore = scorer.score(
                user,
                first,
                Set.of(),
                4,
                1,
                1,
                Instant.now(),
                15,
                false,
                false,
                false
        );

        RecommendationScore secondScore = scorer.score(
                user,
                second,
                Set.of(),
                4,
                1,
                1,
                Instant.now(),
                15,
                false,
                false,
                false
        );

        assertTrue(firstScore.totalScore() >= secondScore.totalScore());
        assertFalse(firstScore.explanation().isBlank());
        assertFalse(secondScore.explanation().isBlank());
    }
}
