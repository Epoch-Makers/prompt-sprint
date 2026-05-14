package com.retroai.repository;

import com.retroai.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByCardIdAndUserId(Long cardId, Long userId);
    long countByCardId(Long cardId);
    long countByUserIdAndRetroId(Long userId, Long retroId);
    boolean existsByCardIdAndUserId(Long cardId, Long userId);
}
