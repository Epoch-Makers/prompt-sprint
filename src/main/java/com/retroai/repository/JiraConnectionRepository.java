package com.retroai.repository;

import com.retroai.entity.JiraConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JiraConnectionRepository extends JpaRepository<JiraConnection, Long> {
    Optional<JiraConnection> findByTeamId(Long teamId);
}
