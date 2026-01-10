package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "batch_config")
public class BatchConfig {

    @Id
    @Column(length = 50)
    private String batchId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 100)
    private String cronExpression;

    @Column(length = 50)
    private String scheduleText;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 50)
    private String targetEntity;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority priority = Priority.MEDIUM;

    private LocalDateTime lastExecutedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExecutionResult lastResult;

    private String lastResultMessage;

    private Integer lastAffectedCount;

    private Long lastExecutionTimeMs;

    @Column(nullable = false)
    private Boolean implemented = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    public enum ExecutionResult {
        SUCCESS, FAIL, RUNNING
    }

    public BatchConfig(String batchId, String name, String description, 
                       String cronExpression, String scheduleText,
                       String targetEntity, Priority priority, boolean implemented) {
        this.batchId = batchId;
        this.name = name;
        this.description = description;
        this.cronExpression = cronExpression;
        this.scheduleText = scheduleText;
        this.targetEntity = targetEntity;
        this.priority = priority;
        this.implemented = implemented;
        this.enabled = true;
    }

    public void recordExecution(ExecutionResult result, String message, int affectedCount, long executionTimeMs) {
        this.lastExecutedAt = LocalDateTime.now();
        this.lastResult = result;
        this.lastResultMessage = message;
        this.lastAffectedCount = affectedCount;
        this.lastExecutionTimeMs = executionTimeMs;
    }
}
