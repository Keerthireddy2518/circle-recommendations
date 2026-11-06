package com.leanin.circle_recommendations.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import jakarta.validation.ValidationException;

@Component
public class JsonDataLoader implements ApplicationRunner, ResourceLoaderAware {

    private static final Logger log = LoggerFactory.getLogger(JsonDataLoader.class);

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final CircleRepository circleRepository;
    private final MembershipRepository membershipRepository;
    private final UserActivityRepository userActivityRepository;
    private final ObjectMapper objectMapper;
    private ResourceLoader resourceLoader;

    public JsonDataLoader(
            TopicRepository topicRepository,
            UserRepository userRepository,
            CircleRepository circleRepository,
            MembershipRepository membershipRepository,
            UserActivityRepository userActivityRepository
    ) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.circleRepository = circleRepository;
        this.membershipRepository = membershipRepository;
        this.userActivityRepository = userActivityRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Long> expectedCounts = readExpectedCounts();
        if (isDataAlreadyLoaded(expectedCounts)) {
            log.info("Seed data already exists. Skipping JSON bootstrap.");
            logSeedSummary();
            return;
        }

        Map<String, Topic> topicByName = new HashMap<>();
        Map<Long, User> userById = new HashMap<>();
        Map<Long, Circle> circleById = new HashMap<>();

        List<TopicSeed> topicSeeds = readList("topics.json", new TypeReference<>() {});
        for (TopicSeed seed : topicSeeds) {
            String topicName = seed.resolvedName();
            if (topicName == null || topicName.isBlank()) {
                continue;
            }
            String topicCategory = seed.resolvedCategory();
            Topic topic = topicRepository.findByNameIgnoreCase(topicName)
                    .orElseGet(() -> new Topic(topicName, topicCategory));
            topic.setCategory(topicCategory);
            topicRepository.save(topic);
            topicByName.put(topic.getName().toLowerCase(Locale.ROOT), topic);
        }

        List<UserSeed> userSeeds = readList("users.json", new TypeReference<>() {});
        for (UserSeed userSeed : userSeeds) {
            String fullName = userSeed.resolvedName();
            String normalizedEmail = userSeed.email() == null || userSeed.email().isBlank()
                    ? generateSyntheticEmail(userSeed)
                    : userSeed.email().trim();

            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseGet(() -> new User(fullName, normalizedEmail));
            user.setName(fullName);
            user.setEmail(normalizedEmail);
            user.getTopics().clear();
            for (String topicName : userSeed.topics()) {
                Topic topic = findOrCreateTopic(topicByName, topicRepository, topicName, null);
                user.getTopics().add(topic);
            }
            userRepository.save(user);
            if (userSeed.id() != null) {
                userById.put(userSeed.id(), user);
            }
        }

        List<CircleSeed> circleSeeds = readList("circles.json", new TypeReference<>() {});
        for (CircleSeed circleSeed : circleSeeds) {
            String circleName = circleSeed.resolvedName();
            Circle circle = circleRepository.findById(circleSeed.id())
                    .orElseGet(() -> new Circle(circleName, circleSeed.description()));
            circle.setName(circleName);
            circle.setDescription(circleSeed.description());
            circle.setActive(circleSeed.active() != null ? circleSeed.active() : true);
            circle.getTopics().clear();
            for (String topicName : circleSeed.topics()) {
                Topic topic = findOrCreateTopic(topicByName, topicRepository, topicName, null);
                circle.getTopics().add(topic);
            }
            circleRepository.save(circle);
            if (circleSeed.id() != null) {
                circleById.put(circleSeed.id(), circle);
            }
        }

        List<MembershipSeed> membershipSeeds = readList("memberships.json", new TypeReference<>() {});
        for (MembershipSeed membershipSeed : membershipSeeds) {
            Long userId = membershipSeed.userId();
            Long circleId = membershipSeed.circleId();
            if (userId == null || circleId == null) {
                continue;
            }

            User user = userById.getOrDefault(userId, userRepository.findById(userId)
                    .orElseThrow(() -> new ValidationException("Membership references missing user id: " + userId)));
            Circle circle = circleById.getOrDefault(circleId, circleRepository.findById(circleId)
                    .orElseThrow(() -> new ValidationException("Membership references missing circle id: " + circleId)));

            Membership membership = membershipRepository.findByUserIdAndCircleId(user.getId(), circle.getId())
                    .orElseGet(() -> new Membership(user, circle, mapMembershipRole(membershipSeed.role()), mapMembershipStatus(membershipSeed.status())));
            membership.setUser(user);
            membership.setCircle(circle);
            membership.setRole(mapMembershipRole(membershipSeed.role()));
            membership.setStatus(mapMembershipStatus(membershipSeed.status()));
            membershipRepository.save(membership);
        }

        List<ActivitySeed> activitySeeds = readList("activities.json", new TypeReference<>() {});
        for (ActivitySeed activitySeed : activitySeeds) {
            Long userId = activitySeed.resolvedUserId();
            if (userId == null) {
                continue;
            }

            User user = userById.getOrDefault(userId, userRepository.findById(userId)
                    .orElseThrow(() -> new ValidationException("Activity references missing user id: " + userId)));
            Circle circle = null;
            Long circleId = activitySeed.circleId();
            if (circleId != null) {
                circle = circleById.getOrDefault(circleId, circleRepository.findById(circleId)
                        .orElseThrow(() -> new ValidationException("Activity references missing circle id: " + circleId)));
            }

            UserActivity activity = new UserActivity(
                    user,
                    circle,
                    mapActivityEventType(activitySeed.eventType()),
                    activitySeed.eventAt() == null ? Instant.now() : activitySeed.eventAt(),
                    activitySeed.details() == null ? activitySeed.body() : activitySeed.details()
            );
            userActivityRepository.save(activity);
        }

        validateLoadedCounts(expectedCounts);
        logSeedSummary();
    }

    private Map<String, Long> readExpectedCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("users", (long) readList("users.json", new TypeReference<>() {}).size());
        counts.put("circles", (long) readList("circles.json", new TypeReference<>() {}).size());
        counts.put("memberships", (long) readList("memberships.json", new TypeReference<>() {}).size());
        counts.put("activities", (long) readList("activities.json", new TypeReference<>() {}).size());
        counts.put("topics", (long) collectTopicNamesAcrossSeeds().size());
        return counts;
    }

    private Set<String> collectTopicNamesAcrossSeeds() {
        Set<String> topicNames = new HashSet<>();

        List<TopicSeed> topicSeeds = readList("topics.json", new TypeReference<List<TopicSeed>>() {});
        for (TopicSeed topicSeed : topicSeeds) {
            String topicName = topicSeed.resolvedName();
            if (topicName != null && !topicName.isBlank()) {
                topicNames.add(topicName.trim().toLowerCase(Locale.ROOT));
            }
        }

        List<UserSeed> userSeeds = readList("users.json", new TypeReference<List<UserSeed>>() {});
        for (UserSeed userSeed : userSeeds) {
            for (String topicName : userSeed.topics()) {
                if (topicName != null && !topicName.isBlank()) {
                    topicNames.add(topicName.trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        List<CircleSeed> circleSeeds = readList("circles.json", new TypeReference<List<CircleSeed>>() {});
        for (CircleSeed circleSeed : circleSeeds) {
            for (String topicName : circleSeed.topics()) {
                if (topicName != null && !topicName.isBlank()) {
                    topicNames.add(topicName.trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        return topicNames;
    }

    private boolean isDataAlreadyLoaded(Map<String, Long> expectedCounts) {
        return expectedCounts.entrySet().stream()
                .allMatch(entry -> getPersistedCount(entry.getKey()) >= entry.getValue());
    }

    private long getPersistedCount(String entityName) {
        return switch (entityName) {
            case "topics" -> topicRepository.count();
            case "users" -> userRepository.count();
            case "circles" -> circleRepository.count();
            case "memberships" -> membershipRepository.count();
            case "activities" -> userActivityRepository.count();
            default -> throw new ValidationException("Unsupported seed entity: " + entityName);
        };
    }

    private void validateLoadedCounts(Map<String, Long> expectedCounts) {
        for (Map.Entry<String, Long> expected : expectedCounts.entrySet()) {
            long actual = getPersistedCount(expected.getKey());
            if (actual != expected.getValue()) {
                throw new IllegalStateException(
                        "Seed validation failed for " + expected.getKey() + ": expected " + expected.getValue() + " rows but persisted " + actual);
            }
        }
    }

    private void logSeedSummary() {
        long users = userRepository.count();
        long circles = circleRepository.count();
        long memberships = membershipRepository.count();
        long topics = topicRepository.count();
        long activities = userActivityRepository.count();

        log.info("Seed validation summary: users={}, circles={}, memberships={}, topics={}, activities={}",
                users, circles, memberships, topics, activities);
    }

    private Topic findOrCreateTopic(Map<String, Topic> topicByName, TopicRepository topicRepository, String explicitName, String category) {
        String normalizedName = explicitName == null ? null : explicitName.trim();
        if (normalizedName == null || normalizedName.isBlank()) {
            throw new ValidationException("Topic name cannot be blank");
        }
        return topicByName.computeIfAbsent(normalizedName.toLowerCase(Locale.ROOT), key -> {
            Optional<Topic> existing = topicRepository.findByNameIgnoreCase(normalizedName);
            if (existing.isPresent()) {
                return existing.get();
            }
            Topic created = new Topic(normalizedName, category);
            return topicRepository.save(created);
        });
    }

    private String generateSyntheticEmail(UserSeed userSeed) {
        String base = userSeed.resolvedName();
        if (base == null || base.isBlank()) {
            base = "user";
        }
        String sanitized = base.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("(^\\.|\\.$)", "");
        if (sanitized.isBlank()) {
            sanitized = "user";
        }
        Long userId = userSeed.id();
        return userId == null ? sanitized + "@example.com" : sanitized + "." + userId + "@example.com";
    }

    private Membership.Role mapMembershipRole(String rawRole) {
        if (rawRole == null) {
            return Membership.Role.MEMBER;
        }
        String normalized = rawRole.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "leader", "admin", "organizer" -> Membership.Role.ADMIN;
            case "mentor" -> Membership.Role.MENTOR;
            default -> Membership.Role.MEMBER;
        };
    }

    private Membership.Status mapMembershipStatus(String rawStatus) {
        if (rawStatus == null) {
            return Membership.Status.ACTIVE;
        }
        String normalized = rawStatus.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pending" -> Membership.Status.PENDING;
            case "left", "inactive" -> Membership.Status.LEFT;
            default -> Membership.Status.ACTIVE;
        };
    }

    private UserActivity.EventType mapActivityEventType(String rawType) {
        if (rawType == null) {
            return UserActivity.EventType.POST;
        }
        String normalized = rawType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "login" -> UserActivity.EventType.LOGIN;
            case "join_circle", "join-circle", "join" -> UserActivity.EventType.JOIN_CIRCLE;
            case "attend_event", "attend-event", "attendance" -> UserActivity.EventType.ATTEND_EVENT;
            case "comment" -> UserActivity.EventType.COMMENT;
            case "like", "reaction" -> UserActivity.EventType.LIKE;
            case "rsvp" -> UserActivity.EventType.RSVP;
            default -> UserActivity.EventType.POST;
        };
    }

    private <T> List<T> readList(String fileName, TypeReference<List<T>> type) {
        Resource resource = resolveDataResource(fileName);
        log.info("Loading seed file {} from {}", fileName, resource);
        try (InputStream inputStream = resource.getInputStream()) {
            List<T> values = objectMapper.readValue(inputStream, type);
            if (values == null) {
                return new ArrayList<>();
            }
            return values;
        } catch (IOException e) {
            throw new ValidationException("Unable to read JSON seed file: " + fileName, e);
        }
    }

    private Resource resolveDataResource(String fileName) {
        List<String> candidates = candidateNames(fileName);

        for (String candidate : candidates) {
            Resource classpath = new ClassPathResource("data/" + candidate);
            if (classpath.exists()) {
                return classpath;
            }

            if (resourceLoader != null) {
                Resource resource = resourceLoader.getResource("classpath:data/" + candidate);
                if (resource.exists()) {
                    return resource;
                }
            }

            Resource localFile = new FileSystemResource("src/main/resources/data/" + candidate);
            if (localFile.exists()) {
                return localFile;
            }
        }

        throw new ValidationException("Missing seed file in classpath or project resources: " + fileName + ". Tried: " + candidates);
    }

    private List<String> candidateNames(String fileName) {
        List<String> candidates = new ArrayList<>();
        candidates.add(fileName);

        String base = fileName;
        int extensionIndex = base.lastIndexOf('.');
        if (extensionIndex > 0) {
            base = base.substring(0, extensionIndex);
        }

        if (base.endsWith("s")) {
            candidates.add(base.substring(0, base.length() - 1) + ".json");
        }
        if (base.endsWith("ies")) {
            candidates.add(base.substring(0, base.length() - 3) + "y.json");
        }

        return candidates.stream().distinct().toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopicSeed(
            @JsonAlias({"topicId", "topic_id"}) Long id,
            String name,
            String category,
            @JsonAlias({"slug"}) String slug,
            @JsonAlias({"label"}) String label,
            @JsonAlias({"domain"}) String domain
    ) {
        public String resolvedName() {
            String candidate = name == null ? label : name;
            if (candidate == null || candidate.isBlank()) {
                candidate = slug;
            }
            return candidate == null ? "" : candidate.trim();
        }

        public String resolvedCategory() {
            return category == null ? domain : category;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserSeed(
            @JsonAlias({"userId", "user_id"}) Long id,
            String name,
            @JsonAlias({"first_name"}) String firstName,
            @JsonAlias({"last_name"}) String lastName,
            String email,
            @JsonAlias({"topic_interests"}) List<String> topicInterests,
            List<String> topics
    ) {
        public String resolvedName() {
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
            if (firstName != null && lastName != null) {
                return (firstName.trim() + " " + lastName.trim()).trim();
            }
            if (firstName != null) {
                return firstName.trim();
            }
            if (lastName != null) {
                return lastName.trim();
            }
            return "Unknown User";
        }

        public List<String> topics() {
            List<String> values = new ArrayList<>();
            if (topics != null) {
                values.addAll(topics);
            }
            if (topicInterests != null) {
                values.addAll(topicInterests);
            }
            return values.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CircleSeed(
            @JsonAlias({"circleId", "circle_id"}) Long id,
            String name,
            @JsonAlias({"slug"}) String slug,
            String description,
            Boolean active,
            @JsonAlias({"tags"}) List<String> tags,
            @JsonAlias({"topic"}) String topic,
            List<String> topics
    ) {
        public String resolvedName() {
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
            return slug == null ? "Untitled Circle" : slug.trim();
        }

        public List<String> topics() {
            List<String> values = new ArrayList<>();
            if (topics != null) {
                values.addAll(topics);
            }
            if (topic != null && !topic.isBlank()) {
                values.add(topic);
            }
            if (tags != null) {
                values.addAll(tags);
            }
            return values.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MembershipSeed(
            @JsonAlias({"membershipId", "membership_id"}) Long id,
            @JsonAlias({"userId", "user_id", "user_id"}) Long userId,
            @JsonAlias({"circleId", "circle_id", "circle_id"}) Long circleId,
            String role,
            String status,
            @JsonAlias({"joined_at"}) Instant joinedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ActivitySeed(
            @JsonAlias({"activityId", "activity_id"}) Long id,
            @JsonAlias({"userId", "user_id", "author_user_id"}) Long userId,
            @JsonAlias({"circleId", "circle_id"}) Long circleId,
            @JsonAlias({"eventType", "event_type", "type"}) String eventType,
            @JsonAlias({"eventAt", "event_at", "created_at"}) Instant eventAt,
            String details,
            String body,
            @JsonAlias({"author_user_id"}) Long authorUserId
    ) {
        public Long resolvedUserId() {
            if (userId != null) {
                return userId;
            }
            return authorUserId;
        }
    }
}
