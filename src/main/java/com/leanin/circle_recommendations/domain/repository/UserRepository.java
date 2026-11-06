package com.leanin.circle_recommendations.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.leanin.circle_recommendations.domain.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"topics"})
    Optional<User> findWithTopicsById(Long id);
}
