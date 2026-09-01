-- =============================================================================
-- GlobalTrade Supply Chain Management System
-- Database Schema Setup Script (MySQL)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS globaltrade_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE globaltrade_db;

-- -----------------------------------------------------------------------------
-- Table: vendors
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vendors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_code VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(150) NOT NULL,
    contact_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(30),
    country VARCHAR(60),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    performance_rating DECIMAL(3, 2),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: warehouses
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS warehouses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    country VARCHAR(60) NOT NULL,
    city VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: inventory_items
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    item_name VARCHAR(150) NOT NULL,
    quantity INT NOT NULL,
    reorder_level INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: app_users (Authentication credentials for Payara JDBC Realm)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: security_roles (Role definitions for RBAC)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS security_roles (
    role_name VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: user_roles (User to Role mapping for Payara JDBC Realm group lookups)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_roles (
    username VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (username, role_name),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (username)
        REFERENCES app_users (username)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_name)
        REFERENCES security_roles (role_name)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: shipments
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_number VARCHAR(100) NOT NULL UNIQUE,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    shipment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expected_delivery_date DATE,
    actual_delivery_date DATE,
    vendor_id BIGINT NOT NULL,
    customer_username VARCHAR(50) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    INDEX idx_shipments_customer (customer_username),
    CONSTRAINT fk_shipment_vendor FOREIGN KEY (vendor_id)
        REFERENCES vendors (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_shipment_customer FOREIGN KEY (customer_username)
        REFERENCES app_users (username)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: customs_documents
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customs_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_number VARCHAR(100) NOT NULL UNIQUE,
    document_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    submission_deadline DATE,
    shipment_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customs_shipment FOREIGN KEY (shipment_id)
        REFERENCES shipments (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: audit_logs
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    performed_by VARCHAR(100),
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details VARCHAR(1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: vendor_user_access (Fine-Grained Vendor Data Authorization Mapping)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vendor_user_access (
    username VARCHAR(50) NOT NULL,
    vendor_id BIGINT NOT NULL,
    PRIMARY KEY (username, vendor_id),
    CONSTRAINT fk_vendor_access_user FOREIGN KEY (username)
        REFERENCES app_users (username)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_vendor_access_vendor FOREIGN KEY (vendor_id)
        REFERENCES vendors (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: supply_chain_alerts (Automated monitoring anomalies and operational alerts)
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

-- =============================================================================
-- Initial Seed Data (Optional for testing)
-- =============================================================================

INSERT INTO vendors (vendor_code, company_name, contact_name, email, phone, country, status, performance_rating, created_at)
VALUES
    ('VND-001', 'Pacific Cargo Ltd', 'Johnathan Smith', 'john@pacificcargo.com', '+1-555-0199', 'United States', 'ACTIVE', 4.85, NOW()),
    ('VND-002', 'SilkRoad Logistics GmbH', 'Greta Weber', 'g.weber@silkroad.de', '+49-30-123456', 'Germany', 'ACTIVE', 4.60, NOW()),
    ('VND-003', 'AsiaPacific Maritime', 'Kenji Sato', 'sato@apmaritime.jp', '+81-3-987654', 'Japan', 'UNDER_REVIEW', 3.90, NOW())
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO warehouses (warehouse_code, name, country, city, capacity, active)
VALUES
    ('WH-SIN-01', 'Singapore Central Hub', 'Singapore', 'Singapore', 50000, TRUE),
    ('WH-ROT-01', 'Rotterdam Port Terminal', 'Netherlands', 'Rotterdam', 75000, TRUE),
    ('WH-LAX-01', 'Los Angeles Gateway', 'United States', 'Los Angeles', 60000, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO inventory_items (sku, item_name, quantity, reorder_level, unit_price, warehouse_id, last_updated)
VALUES
    ('SKU-ELEC-001', 'Industrial Microcontroller Unit', 1200, 200, 45.50, 1, NOW()),
    ('SKU-AUTO-002', 'Hydraulic Brake Sensor Assembly', 450, 100, 120.00, 2, NOW()),
    ('SKU-SOL-003', 'Photovoltaic Inverter 5kW', 80, 25, 650.00, 3, NOW())
ON DUPLICATE KEY UPDATE item_name = VALUES(item_name);

INSERT INTO shipments (tracking_number, origin, destination, shipment_status, expected_delivery_date, actual_delivery_date, vendor_id, customer_username, created_at)
VALUES
    ('TRK-2026-001', 'Tokyo, Japan', 'Singapore', 'IN_TRANSIT', DATE_ADD(CURDATE(), INTERVAL 5 DAY), NULL, 3, 'gt_customer', NOW()),
    ('TRK-2026-002', 'Hamburg, Germany', 'Rotterdam, Netherlands', 'DELIVERED', DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2, NULL, NOW())
ON DUPLICATE KEY UPDATE origin = VALUES(origin);

INSERT INTO customs_documents (document_number, document_type, status, submission_deadline, shipment_id, created_at)
VALUES
    ('DOC-IMP-2026-001', 'IMPORT_DECLARATION', 'SUBMITTED', DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1, NOW()),
    ('DOC-INV-2026-002', 'COMMERCIAL_INVOICE', 'APPROVED', DATE_SUB(CURDATE(), INTERVAL 4 DAY), 2, NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO audit_logs (action, entity_type, entity_id, performed_by, timestamp, details)
VALUES
    ('SYSTEM_INIT', 'SYSTEM', NULL, 'SYSTEM', NOW(), 'GlobalTrade domain model initialized with baseline seed data.');

-- -----------------------------------------------------------------------------
-- Security Seed Data: Roles, Users, Role Assignments
-- Demo Passwords for all accounts: 'Password@123'
-- SHA-256 Digest (HEX): ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f
-- -----------------------------------------------------------------------------
INSERT INTO security_roles (role_name, description) VALUES
    ('ADMIN', 'Enterprise Global Administrator with full system privileges'),
    ('LOGISTICS_COORDINATOR', 'Coordinates shipment routes, dispatches, and carrier scheduling'),
    ('CUSTOMS_AGENT', 'Manages international trade documentation and statutory customs clearance'),
    ('WAREHOUSE_MANAGER', 'Manages warehouse facilities, inventory adjustments, and stock reconciliation'),
    ('VENDOR_REPRESENTATIVE', 'External supplier managing consignment handovers and catalog items'),
    ('CUSTOMER', 'End-client viewing consignment tracking status and delivery confirmations'),
    ('SYSTEM', 'Internal trusted background identity for timers and system jobs')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO app_users (username, password_hash, display_name, active, created_at) VALUES
    ('gt_admin', 'ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f', 'Global Administrator', TRUE, NOW()),
    ('gt_coordinator', 'ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f', 'Logistics Coordinator', TRUE, NOW()),
    ('gt_customs', 'ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f', 'Customs Clearance Officer', TRUE, NOW()),
    ('gt_warehouse', 'ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f', 'Warehouse Floor Manager', TRUE, NOW()),
    ('gt_vendor', 'ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f', 'Pacific Cargo Vendor Rep', TRUE, NOW()),
    ('gt_customer', 'ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f', 'Consignment Client', TRUE, NOW())
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO user_roles (username, role_name) VALUES
    ('gt_admin', 'ADMIN'),
    ('gt_coordinator', 'LOGISTICS_COORDINATOR'),
    ('gt_customs', 'CUSTOMS_AGENT'),
    ('gt_warehouse', 'WAREHOUSE_MANAGER'),
    ('gt_vendor', 'VENDOR_REPRESENTATIVE'),
    ('gt_customer', 'CUSTOMER')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- Fine-Grained Authorization Seed: Map gt_vendor to Vendor #1 ('Pacific Cargo Ltd')
INSERT INTO vendor_user_access (username, vendor_id) VALUES
    ('gt_vendor', 1)
ON DUPLICATE KEY UPDATE vendor_id = VALUES(vendor_id);
