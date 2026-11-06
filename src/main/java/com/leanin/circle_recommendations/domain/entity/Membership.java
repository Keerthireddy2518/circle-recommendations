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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "memberships",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_membership_user_circle", columnNames = {"user_id", "circle_id"})
        },
        indexes = {
                @Index(name = "idx_membership_user", columnList = "user_id"),
                @Index(name = "idx_membership_circle", columnList = "circle_id"),
                @Index(name = "idx_membership_status", columnList = "status")
        }
)
public class Membership {

    public enum Role {
        MEMBER,
        ADMIN,
        MENTOR
    }

    public enum Status {
        ACTIVE,
        PENDING,
        LEFT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_membership_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circle_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_membership_circle"))
    private Circle circle;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected Membership() {
        // JPA constructor
    }

    public Membership(User user, Circle circle, Role role, Status status) {
        this.user = user;
        this.circle = circle;
        this.role = role;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Membership that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
