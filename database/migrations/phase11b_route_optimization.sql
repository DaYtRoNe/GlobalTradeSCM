-- =============================================================================
-- GlobalTrade SCM — Phase 11B: Automated Route Optimization Service Migration
-- Description: Non-destructive DDL migration adding route_options and
--              route_optimization_recommendations tables with foreign keys and indices.
-- Execution: Manual execution by DBA / developer against MySQL schema.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: route_options (Candidate transport corridors offered by freight carriers)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS route_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_code VARCHAR(50) NOT NULL UNIQUE,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    carrier_name VARCHAR(100) NOT NULL,
    carrier_code VARCHAR(50) NULL,
    transport_mode VARCHAR(30) NOT NULL,
    estimated_transit_hours INT NOT NULL,
    estimated_cost DECIMAL(12, 2) NOT NULL,
    operational_risk_score DECIMAL(3, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    INDEX idx_route_corridor (origin, destination, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: route_optimization_recommendations (One current optimal recommendation per shipment)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS route_optimization_recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL UNIQUE,
    route_option_id BIGINT NOT NULL,
    optimization_score DECIMAL(5, 4) NOT NULL,
    transit_time_hours INT NOT NULL,
    estimated_cost DECIMAL(12, 2) NOT NULL,
    risk_score DECIMAL(3, 2) NOT NULL,
    evaluated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evaluation_source VARCHAR(50) NOT NULL,
    summary_rationale VARCHAR(500) NULL,
    INDEX idx_rec_evaluated (evaluated_at),
    CONSTRAINT fk_rec_shipment FOREIGN KEY (shipment_id)
        REFERENCES shipments (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_rec_route_option FOREIGN KEY (route_option_id)
        REFERENCES route_options (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Audit Log initialization record for Phase 11B migration
-- -----------------------------------------------------------------------------
INSERT INTO audit_logs (action, entity_type, entity_id, performed_by, timestamp, details)
VALUES
    ('MIGRATION_PHASE11B', 'SYSTEM', NULL, 'DBA', NOW(), 'Phase 11B route optimization schema objects created successfully.');
