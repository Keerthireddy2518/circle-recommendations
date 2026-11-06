package com.leanin.circle_recommendations.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leanin.circle_recommendations.domain.entity.Topic;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByNameIgnoreCase(String name);
}
