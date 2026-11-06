package com.leanin.circle_recommendations.domain.entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "user_activities",
        indexes = {
                @Index(name = "idx_activity_user_time", columnList = "user_id, event_at"),
                @Index(name = "idx_activity_circle_time", columnList = "circle_id, event_at"),
                @Index(name = "idx_activity_type", columnList = "event_type")
        }
)
public class UserActivity {

    public enum EventType {
        LOGIN,
        JOIN_CIRCLE,
        ATTEND_EVENT,
        POST,
        COMMENT,
        LIKE,
        RSVP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_activity_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id",
            foreignKey = @ForeignKey(name = "fk_activity_circle"))
    private Circle circle;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Column(name = "details", length = 1000)
    private String details;

    protected UserActivity() {
        // JPA constructor
    }

    public UserActivity(User user, EventType eventType, Instant eventAt, String details) {
        this.user = user;
        this.eventType = eventType;
        this.eventAt = eventAt;
        this.details = details;
    }

    public UserActivity(User user, Circle circle, EventType eventType, Instant eventAt, String details) {
        this.user = user;
        this.circle = circle;
        this.eventType = eventType;
        this.eventAt = eventAt;
        this.details = details;
    }

    @PrePersist
    protected void onCreate() {
        if (eventAt == null) {
            eventAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Instant getEventAt() {
        return eventAt;
    }

    public void setEventAt(Instant eventAt) {
        this.eventAt = eventAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserActivity that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
