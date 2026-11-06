package com.leanin.circle_recommendations.domain.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "topics",
        indexes = {
                @Index(name = "idx_topics_name", columnList = "name", unique = true),
                @Index(name = "idx_topics_category", columnList = "category")
        }
)
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "category")
    private String category;

    @ManyToMany(mappedBy = "topics")
    private Set<User> users = new HashSet<>();

    @ManyToMany(mappedBy = "topics")
    private Set<Circle> circles = new HashSet<>();

    protected Topic() {
        // JPA constructor
    }

    public Topic(String name, String category) {
        this.name = name;
        this.category = category;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Set<Circle> getCircles() {
        return circles;
    }

    public void setCircles(Set<Circle> circles) {
        this.circles = circles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Topic topic)) {
            return false;
        }
        return id != null && Objects.equals(id, topic.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
