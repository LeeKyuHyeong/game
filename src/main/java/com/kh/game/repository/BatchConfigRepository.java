package com.kh.game.repository;

import com.kh.game.entity.BatchConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchConfigRepository extends JpaRepository<BatchConfig, String> {

    List<BatchConfig> findByEnabledTrue();

    List<BatchConfig> findByImplementedTrueAndEnabledTrue();

    List<BatchConfig> findByPriorityOrderByNameAsc(BatchConfig.Priority priority);

    List<BatchConfig> findAllByOrderByPriorityAscNameAsc();
}
