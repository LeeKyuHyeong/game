package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "batch_execution_history")
public class BatchExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String batchId;

    @Column(nullable = false, length = 100)
    private String batchName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionType executionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchConfig.ExecutionResult result;

    @Column(length = 1000)
    private String message;

    private Integer affectedCount;

    private Long executionTimeMs;

    @CreationTimestamp
    private LocalDateTime executedAt;

    public enum ExecutionType {
        SCHEDULED, MANUAL
    }

    public BatchExecutionHistory(String batchId, String batchName, ExecutionType executionType) {
        this.batchId = batchId;
        this.batchName = batchName;
        this.executionType = executionType;
    }

    public void complete(BatchConfig.ExecutionResult result, String message, int affectedCount, long executionTimeMs) {
        this.result = result;
        this.message = message;
        this.affectedCount = affectedCount;
        this.executionTimeMs = executionTimeMs;
    }
}
