-- =============================================================================
-- GlobalTrade SCM — Phase 11B: Route Optimization Demo Cleanup Script
-- Purpose: Safely removes demo route options and recommendations while preserving
--          audit log history for evaluators.
-- =============================================================================

-- 1. Remove recommendations associated with demo routes
DELETE FROM route_optimization_recommendations
WHERE route_option_id IN (
    SELECT id FROM route_options WHERE route_code IN ('RT-TYO-SIN-EXP', 'RT-TYO-SIN-STD', 'RT-TYO-SIN-ECO')
);

-- 2. Remove demo route options
DELETE FROM route_options
WHERE route_code IN ('RT-TYO-SIN-EXP', 'RT-TYO-SIN-STD', 'RT-TYO-SIN-ECO');

-- 3. Log Cleanup Event
INSERT INTO audit_logs (action, entity_type, entity_id, performed_by, timestamp, details)
VALUES
    ('DEMO_CLEANUP_PHASE11B', 'RouteOption', NULL, 'DEMO_USER', NOW(), 'Cleaned up Phase 11B demo route options and recommendations.');
