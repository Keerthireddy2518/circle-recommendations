package com.leanin.circle_recommendations.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.leanin.circle_recommendations.domain.entity.UserActivity;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    @EntityGraph(attributePaths = {"circle"})
    List<UserActivity> findByUserIdOrderByEventAtDesc(Long userId);

    @EntityGraph(attributePaths = {"circle"})
    List<UserActivity> findByCircleIdOrderByEventAtDesc(Long circleId);
}
