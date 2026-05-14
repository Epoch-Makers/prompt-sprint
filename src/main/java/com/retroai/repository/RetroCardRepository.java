package com.retroai.repository;

import com.retroai.entity.RetroCard;
import com.retroai.enums.RetroColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetroCardRepository extends JpaRepository<RetroCard, Long> {
    List<RetroCard> findByRetroIdOrderByCreatedAtAsc(Long retroId);
    long countByRetroIdAndColumn(Long retroId, RetroColumn column);
    long countByRetroIdAndAuthorUserId(Long retroId, Long authorUserId);
}
