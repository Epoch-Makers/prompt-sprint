package com.retroai.repository;

import com.retroai.entity.TeamMaturityScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMaturityScoreRepository extends JpaRepository<TeamMaturityScore, Long> {
    Optional<TeamMaturityScore> findFirstByTeamIdOrderByCreatedAtDesc(Long teamId);
    Optional<TeamMaturityScore> findByRetroId(Long retroId);
    List<TeamMaturityScore> findByTeamIdOrderByCreatedAtDesc(Long teamId);
}
