# GlobalTrade SCM — Complete REST API Reference

This document provides a comprehensive reference for all **37 RESTful API endpoints** implemented in GlobalTrade SCM across 7 resource classes.

---

## 1. API Architecture & Global Conventions

### 1.1 Endpoint Inventory Summary (37 Unique Endpoints)

| Resource Class | Base Path | Unique Endpoints | Category | Authentication |
| :--- | :--- | :---: | :--- | :--- |
| **`DatabaseHealthResource`** | `/health/database` | **1** | Health Check API | Public (Unsecured) |
| **`BusinessSecurityVerificationResource`** | `/business-security/*` | **5** | Core Business Security API | HTTP Basic (Role-Secured) |
| **`SecurityVerificationResource`** | `/security/*` | **9** | RBAC Verification API | HTTP Basic (Role-Secured) |
| **`TransactionVerificationResource`** | `/transactions/*` | **6** | Transaction Verification API | HTTP Basic (`ADMIN`) |
| **`InterceptorVerificationResource`** | `/interceptors/*` | **5** | Interceptor Verification API | HTTP Basic (`ADMIN`) |
| **`TimerVerificationResource`** | `/timers/*` | **6** | Timer Verification API | Public (Diagnostics) |
| **`ExceptionVerificationResource`** | `/exceptions/*` | **5** | Exception Verification API | HTTP Basic (`ADMIN`) |
| **TOTAL UNIQUE ENDPOINTS** | | **37** | | |

### 1.2 Base URL
- **Local Application Base URL**: `http://localhost:8080/globaltrade/api`

### 1.3 Global Headers
- **Content-Type**: `application/json`
- **Accept**: `application/json`
- **Authorization**: `Basic <credentials>` *(Required for all protected endpoints)*

### 1.4 Pre-Seeded Demo Credentials (Academic Prototype)
| Role | Username | Seeded Password (schema.sql) | Access Level |
| :--- | :--- | :--- | :--- |
| **Enterprise Administrator** | `gt_admin` | `Password@123` | Global administrative clearance |
| **Logistics Coordinator** | `gt_coordinator` | `Password@123` | Shipment & vendor logistics |
| **Customs Clearance Officer** | `gt_customs` | `Password@123` | Customs declarations review |
| **Warehouse Manager** | `gt_warehouse` | `Password@123` | Stock replenishment & dispatch |
| **Vendor Representative** | `gt_vendor` | `Password@123` | Mapped strictly to Vendor #1 |
| **Consignment Customer** | `gt_customer` | `Password@123` | Read-only tracking |

---

## 2. Health & System Diagnostics Endpoints (1 Endpoint)

### `GET /health/database`
- **Category**: Health Check API
- **Full URL**: `http://localhost:8080/globaltrade/api/health/database`
- **Authentication**: None (Public)
- **Purpose**: Verifies Payara JPA connectivity to MySQL and returns the total registered vendor count.
- **Related EJB**: `SupplyChainDataService`
- **Success Status**: `200 OK`
- **Sample Response**:
  ```json
  {
    "databaseConnected": true,
    "vendorCount": 3,
    "status": "UP"
  }
  ```

---

## 3. Real Business Security Endpoints (5 Endpoints — `/api/business-security/*`)

### `GET /business-security/vendor/{id}`
- **Category**: Business Security API (Fine-Grained Vendor Isolation)
- **Full URL**: `http://localhost:8080/globaltrade/api/business-security/vendor/1`
- **Authentication**: HTTP Basic
- **Required Roles**: `ADMIN`, `LOGISTICS_COORDINATOR`, or `VENDOR_REPRESENTATIVE` (for assigned vendor only)
- **Path Parameters**: `id` (Long, required) — Target Vendor ID.
- **Related EJB**: `VendorAuthorizationServiceBean`
- **Status Codes**: `200 OK`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`.
- **Sample Response (`200 OK`)**:
  ```json
  {
    "status": "SUCCESS",
    "authorizedCaller": "gt_vendor",
    "vendorId": 1,
    "vendorCode": "VND-APEX-001",
    "companyName": "Apex Global Logistics",
    "country": "Singapore",
    "status": "ACTIVE",
    "performanceRating": 4.85
  }
  ```

---

### `GET /business-security/inventory/{id}`
- **Category**: Business Security API
- **Full URL**: `http://localhost:8080/globaltrade/api/business-security/inventory/1`
- **Authentication**: HTTP Basic
- **Required Roles**: All authenticated roles
- **Path Parameters**: `id` (Long, required) — Target Inventory Item ID.
- **Related EJB**: `InventoryServiceBean`
- **Status Codes**: `200 OK`, `401 Unauthorized`, `404 Not Found`.
- **Sample Response (`200 OK`)**:
  ```json
  {
    "status": "SUCCESS",
    "caller": "gt_warehouse",
    "itemId": 1,
    "sku": "SKU-ELEC-4091",
    "itemName": "Industrial Sensor Controller",
    "quantity": 100,
    "reorderLevel": 20,
    "unitPrice": 450.00
  }
  ```

---

### `POST /business-security/customs/{id}/review`
- **Category**: Business Security API (Customs Clearance)
- **Full URL**: `http://localhost:8080/globaltrade/api/business-security/customs/1/review`
- **Authentication**: HTTP Basic
- **Required Roles**: `ADMIN`, `CUSTOMS_AGENT`
- **Path Parameters**: `id` (Long, required) — Customs Document ID.
- **Related EJB**: `CustomsServiceBean`
- **Status Codes**: `200 OK`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`.
- **Sample Response (`200 OK`)**:
  ```json
  {
    "status": "SUCCESS",
    "operation": "REVIEW_CUSTOMS_DOCUMENT",
    "requiredRoles": "ADMIN, CUSTOMS_AGENT",
    "caller": "gt_customs",
    "documentId": 1,
    "documentNumber": "CD-2026-SG-001",
    "newStatus": "APPROVED"
  }
  ```

---

### `POST /business-security/inventory/{id}/replenish`
- **Category**: Business Security API (Stock Replenishment)
- **Full URL**: `http://localhost:8080/globaltrade/api/business-security/inventory/1/replenish?quantity=50`
- **Authentication**: HTTP Basic
- **Required Roles**: `ADMIN`, `WAREHOUSE_MANAGER`
- **Path Parameters**: `id` (Long, required) — Inventory Item ID.
- **Query Parameters**: `quantity` (Integer, optional, default: 50) — Units to add.
- **Related EJB**: `InventoryServiceBean`
- **Status Codes**: `200 OK`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`.
- **Sample Response (`200 OK`)**:
  ```json
  {
    "status": "SUCCESS",
    "operation": "REPLENISH_INVENTORY_STOCK",
    "requiredRoles": "ADMIN, WAREHOUSE_MANAGER",
    "caller": "gt_warehouse",
    "itemId": 1,
    "sku": "SKU-ELEC-4091",
    "replenishedUnits": 50,
    "newQuantity": 150
  }
  ```

---

### `POST /business-security/shipment/{id}/dispatch`
- **Category**: Business Security API (Multi-Step Atomic Dispatch)
- **Full URL**: `http://localhost:8080/globaltrade/api/business-security/shipment/1/dispatch?inventoryId=1&quantity=10`
- **Authentication**: HTTP Basic
- **Required Roles**: `ADMIN`, `LOGISTICS_COORDINATOR`, `WAREHOUSE_MANAGER`
- **Path Parameters**: `id` (Long, required) — Shipment ID.
- **Query Parameters**:
  - `inventoryId` (Long, optional, default: 1) — Inventory Item ID.
  - `quantity` (Integer, optional, default: 10) — Units to deduct.
- **Related EJB**: `ShipmentServiceBean`
- **Status Codes**: `200 OK`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`.
- **Sample Response (`200 OK`)**:
  ```json
  {
    "status": "SUCCESS",
    "operation": "PROCESS_SHIPMENT_DISPATCH",
    "requiredRoles": "ADMIN, LOGISTICS_COORDINATOR, WAREHOUSE_MANAGER",
    "caller": "gt_coordinator",
    "shipmentId": 1,
    "trackingNumber": "TRK-EXP-2026-001",
    "newShipmentStatus": "IN_TRANSIT"
  }
  ```

---

## 4. RBAC Verification Endpoints (9 Endpoints — `/api/security/*`)

| # | Method | Endpoint Path | Required Roles | Purpose |
| :-: | :---: | :--- | :--- | :--- |
| 1 | `GET` | `/api/security/whoami` | Any Authenticated | Returns caller identity, principal name, and role map. |
| 2 | `POST` | `/api/security/admin` | `ADMIN` | Verifies `@RolesAllowed(ADMIN)`. |
| 3 | `POST` | `/api/security/customs` | `ADMIN`, `CUSTOMS_AGENT` | Verifies customs clearance permission. |
| 4 | `POST` | `/api/security/warehouse` | `ADMIN`, `WAREHOUSE_MANAGER` | Verifies warehouse management permission. |
| 5 | `POST` | `/api/security/coordinator` | `ADMIN`, `LOGISTICS_COORDINATOR` | Verifies logistics management permission. |
| 6 | `POST` | `/api/security/vendor` | `ADMIN`, `VENDOR_REPRESENTATIVE`| Verifies vendor representative permission. |
| 7 | `GET` | `/api/security/public` | `@PermitAll` | Verifies public endpoint accessible by all. |
| 8 | `POST` | `/api/security/restricted` | `@DenyAll` | Verifies method is completely rejected. |
| 9 | `GET` | `/api/security/programmatic` | Dynamic Check | Tests `sessionContext.isCallerInRole(...)`. |

---

## 5. Transaction Verification Endpoints (6 Endpoints — `/api/transactions/*`)

*All transaction verification endpoints require the `ADMIN` role.*

| # | Method | Endpoint Path | Purpose | Key Response Fields |
| :-: | :---: | :--- | :--- | :--- |
| 1 | `GET` | `/api/transactions/state` | Inspects current database state for Shipment #1 and Item #1. | `shipment`, `inventoryItem`, `totalAuditLogs`, `recentAuditLogs` |
| 2 | `POST` | `/api/transactions/dispatch/success` | Executes valid CMT dispatch (`REQUIRED`) deducting 10 units. | `status: "SUCCESS"`, `shipmentStatus: "IN_TRANSIT"`, `remainingStock` |
| 3 | `POST` | `/api/transactions/dispatch/fail` | Tests CMT rollback on stock shortage and verifies independent audit survival (`REQUIRES_NEW`). | `status: "TRANSACTION_ROLLED_BACK"`, `rollbackVerified: true`, `independentAuditCommitted: true` |
| 4 | `POST` | `/api/transactions/mandatory-test` | Tests `MANDATORY` attribute by calling `adjustStockInternal` without a transaction. | `mandatoryEnforced: true`, `expectedException: "EJBTransactionRequiredException"` |
| 5 | `POST` | `/api/transactions/reconcile/commit` | Tests BMT programmatic commit under allowed variance threshold. | `bmtResult: "COMMITTED"`, `reconciledCount` |
| 6 | `POST` | `/api/transactions/reconcile/rollback` | Tests BMT programmatic rollback when variance exceeds threshold. | `bmtResult: "PROGRAMMATICALLY_ROLLED_BACK"`, `rollbackVerified: true` |

---

## 6. Interceptor Verification Endpoints (5 Endpoints — `/api/interceptors/*`)

*All interceptor verification endpoints require the `ADMIN` role.*

| # | Method | Endpoint Path | Purpose | Key Response Fields |
| :-: | :---: | :--- | :--- | :--- |
| 1 | `GET` | `/api/interceptors/metrics` | Retrieves latency stats from `PerformanceMonitoringInterceptor`. | `totalInvocations`, `averageExecutionMicros`, `maxExecutionMicros` |
| 2 | `POST` | `/api/interceptors/vendor-valid` | Tests valid rating update through full interceptor chain. | `status: "SUCCESS"`, `interceptorsApplied` |
| 3 | `POST` | `/api/interceptors/vendor-invalid` | Tests `BusinessValidationInterceptor` rejecting rating $> 5.00$. | `validationRejected: true`, `businessMethodExecuted: false` |
| 4 | `POST` | `/api/interceptors/compliance-valid` | Tests valid customs document passing `TradeComplianceInterceptor`. | `complianceCleared: true`, `status: "SUBMITTED"` |
| 5 | `POST` | `/api/interceptors/compliance-invalid` | Tests `TradeComplianceInterceptor` rejecting invalid document code. | `complianceRejected: true`, `complianceViolationMessage` |

---

## 7. Timer Verification Endpoints (6 Endpoints — `/api/timers/*`)

| # | Method | Endpoint Path | Authentication | Purpose |
| :-: | :---: | :--- | :---: | :--- |
| 1 | `GET` | `/api/timers/status` | Public | Inspects declarative 5-min timer status and active single-action timers. |
| 2 | `POST` | `/api/timers/run-monitoring` | Public | Manually runs the monitoring cycle without waiting 5 minutes. |
| 3 | `POST` | `/api/timers/shipment-alert/{shipmentId}` | Public | Schedules single-action shipment expedite alert (`delaySeconds=5`). |
| 4 | `DELETE` | `/api/timers/shipment-alert/{shipmentId}` | Public | Cancels active shipment alert timer. |
| 5 | `POST` | `/api/timers/customs-reminder/{documentId}` | Public | Schedules single-action customs filing reminder (`delaySeconds=5`). |
| 6 | `DELETE` | `/api/timers/customs-reminder/{documentId}` | Public | Cancels active customs reminder timer. |

---

## 8. Exception Verification Endpoints (5 Endpoints — `/api/exceptions/*`)

*All exception verification endpoints require the `ADMIN` role.*

| # | Method | Endpoint Path | Thrown Exception | Expected HTTP Code | Error Code in JSON |
| :-: | :---: | :--- | :--- | :---: | :--- |
| 1 | `GET` | `/api/exceptions/not-found` | `ResourceNotFoundException` | **`404 Not Found`** | `RESOURCE_NOT_FOUND` |
| 2 | `POST` | `/api/exceptions/validation` | `IllegalArgumentException` | **`400 Bad Request`** | `VALIDATION_ERROR` |
| 3 | `POST` | `/api/exceptions/inventory-conflict` | `InsufficientInventoryException` | **`409 Conflict`** | `INSUFFICIENT_INVENTORY` |
| 4 | `POST` | `/api/exceptions/business-rule` | `BusinessRuleViolationException` | **`400 Bad Request`** | `BUSINESS_RULE_VIOLATION` |
| 5 | `GET` | `/api/exceptions/system-error` | `RuntimeException` (Simulated) | **`500 Internal Error`**| `INTERNAL_SERVER_ERROR` |
