package com.retroai.repository;

import com.retroai.entity.JiraInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JiraInsightRepository extends JpaRepository<JiraInsight, Long> {
    List<JiraInsight> findByRetroId(Long retroId);
}
