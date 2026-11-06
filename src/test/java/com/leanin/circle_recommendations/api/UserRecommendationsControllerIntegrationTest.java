package com.leanin.circle_recommendations.api;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
import com.leanin.circle_recommendations.service.RecommendationScoringProperties;

@SpringBootTest
class UserRecommendationsControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

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
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userActivityRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        circleRepository.deleteAll();
        topicRepository.deleteAll();
        scoringProperties.setMaxMembersPerCircle(25);
    }

    @Test
    void shouldReturnRecommendationsForScenario42() throws Exception {
        Topic leadership = topic("Leadership");
        Topic mentorship = topic("Mentorship");
        Topic networking = topic("Networking");
        User user = createUser("User 42", "user42@example.com", leadership, mentorship, networking);
        Circle leadershipCircle = createCircle("Women in Leadership", "Leadership and mentorship support", leadership, mentorship, networking);
        createCircle("Career Growth Circle", "Mentorship and networking coaching", mentorship, networking);

        mockMvc.perform(get("/api/v1/users/{userId}/recommendations", user.getId()).param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.recommendations[0].circleId").value(leadershipCircle.getId()))
                .andExpect(jsonPath("$.recommendations[0].score").isNumber())
                .andExpect(jsonPath("$.recommendations[0].reasons").isArray());
    }

    @Test
    void shouldReturnRecommendationsForScenario7() throws Exception {
        User user = createUser("User 7", "user7@example.com");
        Topic wellness = topic("Wellness");
        Circle wellnessCircle = createCircle("Wellness & Balance", "Healthy habits and balance", wellness);
        userActivityRepository.save(new UserActivity(user, wellnessCircle, UserActivity.EventType.ATTEND_EVENT, Instant.now().minusSeconds(86400L), "attended"));

        String content = mockMvc.perform(get("/api/v1/users/{userId}/recommendations", user.getId()).param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("Wellness & Balance");
    }

    @Test
    void shouldReturnRecommendationsForScenario15() throws Exception {
        Topic leadership = topic("Leadership");
        Topic mentorship = topic("Mentorship");
        User user = createUser("User 15", "user15@example.com", leadership, mentorship);
        Circle joinedCircle = createCircle("Current Circle", "Already a member", leadership, mentorship);
        createCircle("New Mentorship Circle", "Fresh support and mentoring", mentorship);
        membershipRepository.save(new Membership(user, joinedCircle, Membership.Role.MEMBER, Membership.Status.ACTIVE));

        String body = mockMvc.perform(get("/api/v1/users/{userId}/recommendations", user.getId()).param("limit", "5"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("New Mentorship Circle");
        assertThat(body).doesNotContain("Current Circle");
    }

    @Test
    void shouldReturnRecommendationsForScenario23() throws Exception {
        scoringProperties.setMaxMembersPerCircle(2);
        User user = createUser("User 23", "user23@example.com");
        Circle fullCircle = createCircle("Full Circle", "At cap", topic("Leadership"));
        createCircle("Open Leadership Circle", "Available seats", topic("Leadership"));
        User memberOne = createUser("Member 1", "member1@example.com");
        User memberTwo = createUser("Member 2", "member2@example.com");
        membershipRepository.save(new Membership(memberOne, fullCircle, Membership.Role.MEMBER, Membership.Status.ACTIVE));
        membershipRepository.save(new Membership(memberTwo, fullCircle, Membership.Role.MEMBER, Membership.Status.ACTIVE));

        String body = mockMvc.perform(get("/api/v1/users/{userId}/recommendations", user.getId()).param("limit", "5"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("Full Circle");
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
}
