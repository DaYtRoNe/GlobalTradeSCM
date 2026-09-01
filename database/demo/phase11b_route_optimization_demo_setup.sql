-- =============================================================================
-- GlobalTrade SCM — Phase 11B: Route Optimization Demo Setup Script
-- Purpose: Seeds 3 candidate route options for the active Tokyo -> Singapore corridor
--          matching active shipment TRK-2026-001 (Shipment #1).
--
-- Candidate Corridors:
-- 1. RT-TYO-SIN-EXP: Air Express (Fast: 18h, Cost: $3200.00, Risk: 0.05)
-- 2. RT-TYO-SIN-STD: Sea Standard (Balanced: 96h, Cost: $1200.00, Risk: 0.08)
-- 3. RT-TYO-SIN-ECO: Sea Economy (Economy: 168h, Cost: $750.00, Risk: 0.25)
-- =============================================================================

INSERT INTO route_options (
    route_code, origin, destination, carrier_name, carrier_code,
    transport_mode, estimated_transit_hours, estimated_cost, operational_risk_score,
    active, created_at
) VALUES
    ('RT-TYO-SIN-EXP', 'Tokyo, Japan', 'Singapore', 'Nippon Air Cargo', 'NAC-801', 'AIR', 18, 3200.00, 0.05, TRUE, NOW()),
    ('RT-TYO-SIN-STD', 'Tokyo, Japan', 'Singapore', 'Pacific Maritime Express', 'PME-204', 'SEA', 96, 1200.00, 0.08, TRUE, NOW()),
    ('RT-TYO-SIN-ECO', 'Tokyo, Japan', 'Singapore', 'Ocean Alliance Line', 'OAL-105', 'SEA', 168, 750.00, 0.25, TRUE, NOW())
ON DUPLICATE KEY UPDATE
    estimated_transit_hours = VALUES(estimated_transit_hours),
    estimated_cost = VALUES(estimated_cost),
    operational_risk_score = VALUES(operational_risk_score),
    active = VALUES(active);

-- Log Demo Setup Audit Entry
INSERT INTO audit_logs (action, entity_type, entity_id, performed_by, timestamp, details)
VALUES
    ('DEMO_SETUP_PHASE11B', 'RouteOption', NULL, 'DEMO_USER', NOW(), 'Seeded 3 candidate route options for corridor Tokyo, Japan -> Singapore.');
