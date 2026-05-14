package com.retroai.repository;

import com.retroai.entity.Action;
import com.retroai.enums.ActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionRepository extends JpaRepository<Action, Long> {
    List<Action> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    List<Action> findByTeamIdAndStatusOrderByCreatedAtDesc(Long teamId, ActionStatus status);
    List<Action> findByRetroIdOrderByCreatedAtDesc(Long retroId);
    List<Action> findByRetroIdAndStatus(Long retroId, ActionStatus status);
    List<Action> findByRetroIdAndStatusNot(Long retroId, ActionStatus status);
}
