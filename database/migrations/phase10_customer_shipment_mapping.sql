-- =============================================================================
-- GlobalTrade Supply Chain Management System
-- Database Migration Script: Phase 10 Customer-to-Shipment Mapping
-- =============================================================================
-- PURPOSE:
-- Adds the `customer_username` column to the `shipments` table to enable
-- customer-scoped consignment tracking in the Customer Portal.
--
-- TARGET DATABASE: globaltrade_db (MySQL 8.0 / 8.4)
-- INSTRUCTIONS:
-- This is a ONE-TIME manual migration script. Run this script in HeidiSQL,
-- MySQL Workbench, or mysql CLI after reviewing Phase 10 backend changes.
-- =============================================================================

USE globaltrade_db;

-- 1. Add customer_username column to shipments table if not already present
ALTER TABLE shipments
    ADD COLUMN customer_username VARCHAR(50) NULL AFTER vendor_id;

-- 2. Add an index on customer_username for optimal customer query performance
CREATE INDEX idx_shipments_customer ON shipments (customer_username);

-- 3. Add foreign key constraint referencing app_users(username)
ALTER TABLE shipments
    ADD CONSTRAINT fk_shipment_customer
        FOREIGN KEY (customer_username)
        REFERENCES app_users (username)
        ON UPDATE CASCADE
        ON DELETE SET NULL;

-- 4. Seed Data Mapping:
-- Map shipment #1 ('TRK-2026-001') to 'gt_customer' to demonstrate ALLOW (200 OK)
UPDATE shipments
SET customer_username = 'gt_customer'
WHERE tracking_number = 'TRK-2026-001';

-- Shipment #2 ('TRK-2026-002') remains customer_username = NULL
-- to demonstrate DENY (403 Forbidden) when 'gt_customer' attempts unauthorized access.
UPDATE shipments
SET customer_username = NULL
WHERE tracking_number = 'TRK-2026-002';

-- Verification Query:
-- SELECT id, tracking_number, origin, destination, shipment_status, vendor_id, customer_username FROM shipments;
