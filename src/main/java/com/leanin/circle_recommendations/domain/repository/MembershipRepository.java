package com.leanin.circle_recommendations.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.leanin.circle_recommendations.domain.entity.Membership;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    @EntityGraph(attributePaths = {"circle"})
    List<Membership> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "circle"})
    List<Membership> findByCircleId(Long circleId);

    @EntityGraph(attributePaths = {"circle"})
    Optional<Membership> findByUserIdAndCircleId(Long userId, Long circleId);

    @EntityGraph(attributePaths = {"circle"})
    List<Membership> findByUserIdAndStatus(Long userId, Membership.Status status);
}
