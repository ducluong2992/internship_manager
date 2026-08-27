package com.qlnv.modules.ai.repository;

import com.qlnv.modules.ai.entity.AIConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIConfigRepository extends JpaRepository<AIConfig, Integer> {
    Optional<AIConfig> findFirstByOrderByIdAsc();
}
