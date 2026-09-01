-- =============================================================================
-- GlobalTrade Supply Chain Management System
-- Database Migration Script: Phase 11 - Supply Chain Alerts & Monitoring
-- Target Table: supply_chain_alerts
-- =============================================================================

USE globaltrade_db;

-- -----------------------------------------------------------------------------
-- 1. Create Table: supply_chain_alerts
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS supply_chain_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_key VARCHAR(100) NOT NULL UNIQUE,
    alert_type VARCHAR(50) NOT NULL,
    alert_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    detected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_detected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at DATETIME NULL,
    resolved_at DATETIME NULL,
    acknowledged_by VARCHAR(100) NULL,
    INDEX idx_alerts_status (alert_status),
    INDEX idx_alerts_type (alert_type),
    INDEX idx_alerts_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Audit Trail Entry for Migration
-- -----------------------------------------------------------------------------
INSERT INTO audit_logs (action, entity_type, entity_id, performed_by, timestamp, details)
VALUES (
    'SCHEMA_MIGRATION',
    'DATABASE',
    NULL,
    'SYSTEM',
    NOW(),
    'Applied Phase 11 migration: created supply_chain_alerts table with unique alert_key and indexes.'
);
