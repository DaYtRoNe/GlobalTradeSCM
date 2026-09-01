-- =============================================================================
-- GlobalTrade Supply Chain Management System
-- Phase 11A Demo Cleanup: Remaining Supply Chain Alerts
-- Restores Vendor Rating and Sets Demo Records to Clean Resolved States
-- (Preserves Full Entity History and Referencing Alert Audit Trails)
-- =============================================================================

USE globaltrade_db;

-- -----------------------------------------------------------------------------
-- 1. RESTORE VENDOR PERFORMANCE RATING
-- Restores original rating from backup table, then drops temporary backup table
-- -----------------------------------------------------------------------------

-- Restore Vendor #1 Performance Rating from Backup
UPDATE vendors v
JOIN phase11_demo_vendor_backup b ON v.id = b.vendor_id
SET v.performance_rating = b.original_rating;

-- Fallback if backup table was not present or already dropped
UPDATE vendors
SET performance_rating = 4.85
WHERE id = 1 AND performance_rating < 3.00;

-- Drop temporary backup table now that restoration is complete
DROP TABLE IF EXISTS phase11_demo_vendor_backup;

-- -----------------------------------------------------------------------------
-- 2. TRANSITION DEMO BUSINESS ENTITIES TO RESOLVED STATES
-- Note: We preserve the physical records to maintain referential integrity for
-- historical RESOLVED alert records in supply_chain_alerts and audit_logs.
-- -----------------------------------------------------------------------------

-- 1. Clear Shipment Delay: Mark Demo Consignment as DELIVERED
UPDATE shipments
SET shipment_status = 'DELIVERED',
    actual_delivery_date = CURDATE()
WHERE tracking_number = 'TRK-DEMO-11A-01';

-- 2. Clear Customs Deadline: Mark Demo Declaration as APPROVED
UPDATE customs_documents
SET status = 'APPROVED'
WHERE document_number = 'DOC-DEMO-11A-01';

-- -----------------------------------------------------------------------------
-- 3. VERIFICATION CHECK OUTPUT
-- -----------------------------------------------------------------------------
SELECT 
    'DEMO CLEANUP COMPLETED: CONDITIONS RESOLVED & ENTITY HISTORY PRESERVED' AS cleanup_status,
    (SELECT performance_rating FROM vendors WHERE id = 1) AS vendor_1_restored_rating,
    (SELECT shipment_status FROM shipments WHERE tracking_number = 'TRK-DEMO-11A-01') AS demo_shipment_status,
    (SELECT status FROM customs_documents WHERE document_number = 'DOC-DEMO-11A-01') AS demo_customs_doc_status;
