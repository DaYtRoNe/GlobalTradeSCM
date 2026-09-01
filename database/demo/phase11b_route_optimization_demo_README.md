# Phase 11B: Automated Route Optimization Service — Demonstration & Viva Guide

This guide provides end-to-end instructions for demonstrating the **Automated Route Optimization Service** in GlobalTrade SCM.

---

## 1. Architecture & Algorithm Overview

### Multi-Criteria Scoring Formula
For all candidate routes matching the same origin and destination:
$$\text{Score} = (0.45 \times S_{\text{speed}}) + (0.35 \times S_{\text{cost}}) + (0.20 \times S_{\text{risk}})$$

* **$S_{\text{speed}}$ (Delivery Speed)**: Normalized from transit hours ($T_i$) where lowest hours = 1.0.
* **$S_{\text{cost}}$ (Cost Efficiency)**: Normalized from estimated cost ($C_i$) where lowest cost = 1.0.
* **$S_{\text{risk}}$ (Operational Reliability)**: Normalized from risk score ($R_i$) where lowest risk = 1.0.

### Transaction & Scheduling Architecture
* **Automatic EJB Timer**: Runs every 10 minutes (`@Schedule(hour="*", minute="*/10", second="0", persistent=true)`).
* **Failure Isolation**: `RouteOptimizationCoordinatorBean` (`NOT_SUPPORTED`) orchestrates `RouteOptimizationWorkerBean` (`REQUIRES_NEW`), guaranteeing that an error on one shipment does not abort evaluations for other consignments.
* **Idempotency**: Exactly **one recommendation** per active shipment exists in `route_optimization_recommendations`. Repeated scheduled runs update timestamps without creating duplicate rows or duplicate audit entries.

---

## 2. Step-by-Step Viva Demonstration Workflow

### Step A: Seed Candidate Route Options
Execute [`phase11b_route_optimization_demo_setup.sql`](file:///d:/IntelliJ%20Projects/BCD_2/GlobalTradeSCM/database/demo/phase11b_route_optimization_demo_setup.sql) against MySQL:
* Corridors created for **`Tokyo, Japan -> Singapore`** (matching active shipment `TRK-2026-001`, `id = 1`):
  1. `RT-TYO-SIN-EXP`: Air Cargo (18 hours, \$3,200.00, Risk 0.05)
  2. `RT-TYO-SIN-STD`: Sea Standard (96 hours, \$1,200.00, Risk 0.08)
  3. `RT-TYO-SIN-ECO`: Sea Economy (168 hours, \$750.00, Risk 0.25)

### Step B: Trigger Optimization via REST
Invoke manual evaluation as `LOGISTICS_COORDINATOR` or `ADMIN`:
```http
POST http://localhost:8080/globaltrade/api/route-optimizations/shipment/1/evaluate
Authorization: Basic Z3RfY29vcmRpbmF0b3I6UGFzc3dvcmRAMTIz
```
*(Credentials: `gt_coordinator` / `Password@123`)*

**Response (`200 OK`)**:
```json
{
  "shipmentId": 1,
  "trackingNumber": "TRK-2026-001",
  "origin": "Tokyo, Japan",
  "destination": "Singapore",
  "selectedRouteCode": "RT-TYO-SIN-STD",
  "carrierName": "Pacific Maritime Express",
  "transportMode": "SEA",
  "optimizationScore": 0.6718,
  "transitTimeHours": 96,
  "estimatedCost": 1200.00,
  "riskScore": 0.08,
  "evaluationSource": "MANUAL_REST_TRIGGER"
}
```

### Step C: Verify Single Recommendation & Audit Entry
Query MySQL:
```sql
SELECT * FROM route_optimization_recommendations WHERE shipment_id = 1;
SELECT * FROM audit_logs WHERE action LIKE 'ROUTE_RECOMMENDATION%' ORDER BY id DESC;
```
* Observe: Exactly 1 row in `route_optimization_recommendations` and 1 audit entry `ROUTE_RECOMMENDATION_CREATED`.

### Step D: Prove Re-Evaluation Idempotency
Call `POST http://localhost:8080/globaltrade/api/route-optimizations/shipment/1/evaluate` again.
* Observe: No new recommendation row is inserted (row is merged/updated in place) and no duplicate audit entry is created.

### Step E: Prove Re-Optimization When Route Conditions Change
Update the air express route cost to make it overwhelmingly optimal:
```sql
UPDATE route_options SET estimated_cost = 900.00 WHERE route_code = 'RT-TYO-SIN-EXP';
```
Trigger evaluation again:
```http
POST http://localhost:8080/globaltrade/api/route-optimizations/shipment/1/evaluate
```
* Observe: `selectedRouteCode` changes to `RT-TYO-SIN-EXP`.
* Query audit log:
```sql
SELECT action, details, performed_by, timestamp FROM audit_logs WHERE action = 'ROUTE_RECOMMENDATION_CHANGED' ORDER BY id DESC LIMIT 1;
```
* Output proves: `ROUTE_RECOMMENDATION_CHANGED` logged with previous and new route codes.

---

## 3. Demo Cleanup

Execute [`phase11b_route_optimization_demo_cleanup.sql`](file:///d:/IntelliJ%20Projects/BCD_2/GlobalTradeSCM/database/demo/phase11b_route_optimization_demo_cleanup.sql):
```sql
SOURCE database/demo/phase11b_route_optimization_demo_cleanup.sql;
```
Removes demo route options and recommendations while preserving historical audit logs.
