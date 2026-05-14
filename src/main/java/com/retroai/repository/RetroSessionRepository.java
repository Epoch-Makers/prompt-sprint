package com.retroai.repository;

import com.retroai.entity.RetroSession;
import com.retroai.enums.RetroStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetroSessionRepository extends JpaRepository<RetroSession, Long> {
    List<RetroSession> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    Optional<RetroSession> findFirstByTeamIdAndStatus(Long teamId, RetroStatus status);
    Optional<RetroSession> findFirstByTeamIdOrderByCreatedAtDesc(Long teamId);
    List<RetroSession> findByTeamIdAndIdNotOrderByCreatedAtDesc(Long teamId, Long id);
    Optional<RetroSession> findByGuestToken(String guestToken);
}
