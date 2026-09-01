-- =============================================================================
-- GlobalTrade Supply Chain Management System
-- Phase 11A Demo Setup: Remaining Supply Chain Alerts
-- (SHIPMENT_DELAY, VENDOR_PERFORMANCE_RISK, CUSTOMS_DOCUMENT_DEADLINE)
-- =============================================================================

USE globaltrade_db;

-- -----------------------------------------------------------------------------
-- 1. VENDOR PERFORMANCE RISK DEMO SETUP
-- Target: Vendor #1 ('Pacific Cargo Ltd'), mapped to 'gt_vendor'
-- Threshold: performance_rating < 3.00 (Set to 2.65)
-- -----------------------------------------------------------------------------

-- Create backup table to preserve original rating for full reversible cleanup
CREATE TABLE IF NOT EXISTS phase11_demo_vendor_backup (
    vendor_id BIGINT PRIMARY KEY,
    original_rating DECIMAL(3,2) NOT NULL,
    backup_timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Backup current rating only if not already backed up
INSERT IGNORE INTO phase11_demo_vendor_backup (vendor_id, original_rating, backup_timestamp)
SELECT id, performance_rating, NOW()
FROM vendors
WHERE id = 1;

-- Temporarily lower performance rating to 2.65 (< 3.00 threshold)
UPDATE vendors
SET performance_rating = 2.65
WHERE id = 1;

-- -----------------------------------------------------------------------------
-- 2. SHIPMENT DELAY DEMO SETUP
-- Target: Dedicated demo shipment owned by 'gt_customer'
-- Condition: expected_delivery_date < CURDATE() AND status NOT IN ('DELIVERED', 'CANCELLED')
-- -----------------------------------------------------------------------------

INSERT INTO shipments (
    tracking_number,
    origin,
    destination,
    shipment_status,
    expected_delivery_date,
    actual_delivery_date,
    vendor_id,
    customer_username,
    created_at
)
VALUES (
    'TRK-DEMO-11A-01',
    'Tokyo, Japan',
    'Port of Colombo, Sri Lanka',
    'IN_TRANSIT',
    DATE_SUB(CURDATE(), INTERVAL 3 DAY),
    NULL,
    1,
    'gt_customer',
    NOW()
)
ON DUPLICATE KEY UPDATE
    expected_delivery_date = VALUES(expected_delivery_date),
    shipment_status = VALUES(shipment_status),
    actual_delivery_date = NULL,
    customer_username = VALUES(customer_username);

-- -----------------------------------------------------------------------------
-- 3. CUSTOMS DOCUMENT DEADLINE DEMO SETUP
-- Target: Dedicated customs declaration attached to demo shipment
-- Condition: submission_deadline <= CURDATE() AND status NOT IN ('APPROVED', 'REJECTED')
-- -----------------------------------------------------------------------------

INSERT INTO customs_documents (
    document_number,
    document_type,
    status,
    submission_deadline,
    shipment_id,
    created_at
)
SELECT
    'DOC-DEMO-11A-01',
    'IMPORT_DECLARATION',
    'SUBMITTED',
    DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    s.id,
    NOW()
FROM shipments s
WHERE s.tracking_number = 'TRK-DEMO-11A-01'
ON DUPLICATE KEY UPDATE
    submission_deadline = VALUES(submission_deadline),
    status = VALUES(status);

-- -----------------------------------------------------------------------------
-- 4. Verification Check Output
-- -----------------------------------------------------------------------------
SELECT 
    'DEMO DATA SEEDED SUCCESSFULLY' AS setup_status,
    (SELECT COUNT(*) FROM shipments WHERE tracking_number = 'TRK-DEMO-11A-01') AS demo_shipment_count,
    (SELECT performance_rating FROM vendors WHERE id = 1) AS vendor_1_rating,
    (SELECT COUNT(*) FROM customs_documents WHERE document_number = 'DOC-DEMO-11A-01') AS demo_customs_doc_count;
