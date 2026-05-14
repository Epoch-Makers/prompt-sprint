package com.retroai.repository;

import com.retroai.entity.AiThemeCluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiThemeClusterRepository extends JpaRepository<AiThemeCluster, Long> {
    List<AiThemeCluster> findByTeamId(Long teamId);
    List<AiThemeCluster> findByRetroId(Long retroId);
}
