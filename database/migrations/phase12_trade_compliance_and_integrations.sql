-- =============================================================================
-- GlobalTrade SCM - Phase 12 Database Migration Script
-- Purpose: Add International Trade Agreement Rules table for trade compliance
-- =============================================================================

CREATE TABLE IF NOT EXISTS trade_agreement_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agreement_code VARCHAR(50) NOT NULL,
    agreement_name VARCHAR(150) NOT NULL,
    origin_country VARCHAR(100) NOT NULL,
    destination_country VARCHAR(100) NOT NULL,
    document_type_required VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(300) NULL,
    INDEX idx_trade_corridor (origin_country, destination_country, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Baseline International Trade Agreement Rules
INSERT INTO trade_agreement_rules (agreement_code, agreement_name, origin_country, destination_country, document_type_required, active, description)
VALUES
    ('JSEPA', 'Japan-Singapore Economic Partnership Agreement', 'Japan', 'Singapore', 'IMPORT_DECLARATION', TRUE, 'Mandates electronic import filing for Japan-Singapore bilateral maritime corridor'),
    ('JSEPA-COO', 'Japan-Singapore Preferential Origin Protocol', 'Japan', 'Singapore', 'CERTIFICATE_OF_ORIGIN', TRUE, 'Statutory certificate of origin required for tariff concessions'),
    ('EU-SGP-FTA', 'EU-Singapore Free Trade Agreement', 'Germany', 'Singapore', 'IMPORT_DECLARATION', TRUE, 'Mandatory declaration for European consignments bound for Singapore Hub'),
    ('USMCA', 'United States-Mexico-Canada Agreement', 'United States', 'Canada', 'COMMERCIAL_INVOICE', TRUE, 'Cross-border North American commercial invoice statutory requirement')
ON DUPLICATE KEY UPDATE agreement_name = VALUES(agreement_name);
