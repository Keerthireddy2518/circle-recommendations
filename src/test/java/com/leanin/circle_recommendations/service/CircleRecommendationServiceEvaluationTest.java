package com.leanin.circle_recommendations.service;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.leanin.circle_recommendations.domain.entity.Circle;
import com.leanin.circle_recommendations.domain.entity.Membership;
import com.leanin.circle_recommendations.domain.entity.Topic;
import com.leanin.circle_recommendations.domain.entity.User;
import com.leanin.circle_recommendations.domain.entity.UserActivity;
import com.leanin.circle_recommendations.domain.repository.CircleRepository;
import com.leanin.circle_recommendations.domain.repository.MembershipRepository;
import com.leanin.circle_recommendations.domain.repository.TopicRepository;
import com.leanin.circle_recommendations.domain.repository.UserActivityRepository;
import com.leanin.circle_recommendations.domain.repository.UserRepository;

@SpringBootTest
class CircleRecommendationServiceEvaluationTest {

    @Autowired
    private CircleRecommendationService recommendationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CircleRepository circleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private RecommendationScoringProperties scoringProperties;

    @BeforeEach
    void setUp() {
        userActivityRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        circleRepository.deleteAll();
        topicRepository.deleteAll();
        scoringProperties.setMaxMembersPerCircle(25);
    }

    @Test
    void shouldEvaluateScenario42() {
        Topic leadership = topic("Leadership");
        Topic mentorship = topic("Mentorship");
        Topic networking = topic("Networking");

        User user = createUser("User 42", "user42@example.com", leadership, mentorship, networking);
        createCircle("Women in Leadership", "Leadership and mentorship support", leadership, mentorship, networking);
        createCircle("Career Growth Circle", "Mentorship and networking coaching", mentorship, networking);

        List<CircleRecommendationService.RecommendedCircle> recommendations = recommendationService.recommendForUser(user.getId(), 3);

        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations.getFirst().name()).isEqualTo("Women in Leadership");
        assertThat(recommendations.getFirst().score()).isGreaterThan(0.5);
        String explanation = recommendations.getFirst().explanation();
        assertThat(explanation).isNotBlank();
        assertThat(explanation.contains("Topic match") || explanation.contains("Recent activity")).isTrue();
    }

    @Test
    void shouldEvaluateScenario7() {
        User user = createUser("User 7", "user7@example.com");
        Circle wellness = createCircle("Wellness & Balance", "Healthy habits and balance", topic("Wellness"));
        addActivity(user, wellness, UserActivity.EventType.ATTEND_EVENT, Instant.now().minusSeconds(86400L));

        List<CircleRecommendationService.RecommendedCircle> recommendations = recommendationService.recommendForUser(user.getId(), 3);

        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations.stream().allMatch(item -> item.score() > 0.0)).isTrue();
        assertThat(recommendations.getFirst().explanation().toLowerCase()).contains("cold");
    }

    @Test
    void shouldEvaluateScenario15() {
        Topic leadership = topic("Leadership");
        Topic mentorship = topic("Mentorship");
        User user = createUser("User 15", "user15@example.com", leadership, mentorship);
        Circle currentCircle = createCircle("Current Circle", "Already a member", leadership, mentorship);
        createCircle("New Mentorship Circle", "Fresh support and mentoring", mentorship);
        membershipRepository.save(new Membership(user, currentCircle, Membership.Role.MEMBER, Membership.Status.ACTIVE));

        List<CircleRecommendationService.RecommendedCircle> recommendations = recommendationService.recommendForUser(user.getId(), 5);

        assertThat(recommendations.stream().map(CircleRecommendationService.RecommendedCircle::circleId)).doesNotContain(currentCircle.getId());
        assertThat(recommendations.stream().map(CircleRecommendationService.RecommendedCircle::name)).contains("New Mentorship Circle");
    }

    @Test
    void shouldEvaluateScenario23() {
        scoringProperties.setMaxMembersPerCircle(2);
        User user = createUser("User 23", "user23@example.com");
        Circle fullCircle = createCircle("Full Circle", "At cap", topic("Leadership"));
        createCircle("Open Leadership Circle", "Available seats", topic("Leadership"));

        User memberOne = createUser("Member 1", "member1@example.com");
        User memberTwo = createUser("Member 2", "member2@example.com");
        membershipRepository.save(new Membership(memberOne, fullCircle, Membership.Role.MEMBER, Membership.Status.ACTIVE));
        membershipRepository.save(new Membership(memberTwo, fullCircle, Membership.Role.MEMBER, Membership.Status.ACTIVE));

        List<CircleRecommendationService.RecommendedCircle> recommendations = recommendationService.recommendForUser(user.getId(), 5);

        assertThat(recommendations.stream().map(CircleRecommendationService.RecommendedCircle::circleId)).doesNotContain(fullCircle.getId());
        assertThat(recommendations.stream().map(CircleRecommendationService.RecommendedCircle::name)).contains("Open Leadership Circle");
    }

    private User createUser(String name, String email, Topic... topics) {
        User user = new User(name, email);
        for (Topic topic : topics) {
            user.getTopics().add(topic);
        }
        return userRepository.save(user);
    }

    private Circle createCircle(String name, String description, Topic... topics) {
        Circle circle = new Circle(name, description);
        for (Topic topic : topics) {
            circle.getTopics().add(topic);
        }
        return circleRepository.save(circle);
    }

    private Topic topic(String name) {
        return ensureTopic(name);
    }

    private Topic ensureTopic(String name) {
        return topicRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> topicRepository.save(new Topic(name, "general")));
    }

    private void addActivity(User user, Circle circle, UserActivity.EventType eventType, Instant eventAt) {
        UserActivity activity = new UserActivity(user, circle, eventType, eventAt, "activity");
        userActivityRepository.save(activity);
    }
}
