package com.leanin.circle_recommendations.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.leanin.circle_recommendations.domain.entity.Circle;

public interface CircleRepository extends JpaRepository<Circle, Long> {

    @EntityGraph(attributePaths = {"topics"})
    List<Circle> findByActiveTrue();

    @EntityGraph(attributePaths = {"topics"})
    Optional<Circle> findWithTopicsById(Long id);
}
