package com.leanin.circle_recommendations.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "circles",
        indexes = {
                @Index(name = "idx_circles_name", columnList = "name"),
                @Index(name = "idx_circles_active", columnList = "is_active")
        }
)
public class Circle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToMany
    @JoinTable(
            name = "circle_topics",
            joinColumns = @JoinColumn(name = "circle_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"),
            indexes = {
                    @Index(name = "idx_circle_topics_circle_id", columnList = "circle_id"),
                    @Index(name = "idx_circle_topics_topic_id", columnList = "topic_id")
            }
    )
    private Set<Topic> topics = new HashSet<>();

    @OneToMany(mappedBy = "circle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membership> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "circle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserActivity> activities = new ArrayList<>();

    protected Circle() {
        // JPA constructor
    }

    public Circle(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Topic> getTopics() {
        return topics;
    }

    public void setTopics(Set<Topic> topics) {
        this.topics = topics;
    }

    public void addTopic(Topic topic) {
        this.topics.add(topic);
    }

    public List<Membership> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<Membership> memberships) {
        this.memberships = memberships;
    }

    public void addMembership(Membership membership) {
        memberships.add(membership);
        membership.setCircle(this);
    }

    public List<UserActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<UserActivity> activities) {
        this.activities = activities;
    }

    public void addActivity(UserActivity activity) {
        activities.add(activity);
        activity.setCircle(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Circle circle)) {
            return false;
        }
        return id != null && Objects.equals(id, circle.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
