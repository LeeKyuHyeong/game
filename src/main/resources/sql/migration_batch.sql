-- migration_batch.sql
-- 배치 관리 관련 테이블

-- ========================================
-- batch_config 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS batch_config (
    batch_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    cron_expression VARCHAR(100) NOT NULL,
    schedule_text VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    target_entity VARCHAR(50),
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    last_executed_at TIMESTAMP NULL,
    last_result VARCHAR(20),
    last_result_message VARCHAR(500),
    last_affected_count INT,
    last_execution_time_ms BIGINT,
    implemented BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ========================================
-- batch_execution_history 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS batch_execution_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL,
    batch_name VARCHAR(100) NOT NULL,
    execution_type VARCHAR(20) NOT NULL,
    result VARCHAR(20) NOT NULL,
    message VARCHAR(1000),
    affected_count INT,
    execution_time_ms BIGINT,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_history_batch_id (batch_id),
    INDEX idx_history_executed_at (executed_at)
);

-- ========================================
-- 인덱스
-- ========================================
CREATE INDEX IF NOT EXISTS idx_batch_enabled ON batch_config(enabled);
CREATE INDEX IF NOT EXISTS idx_batch_implemented ON batch_config(implemented);
