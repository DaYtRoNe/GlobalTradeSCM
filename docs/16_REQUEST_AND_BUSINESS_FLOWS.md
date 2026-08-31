# GlobalTrade SCM — Request & Business Flows Guide

This document traces the complete end-to-end execution paths of all major request scenarios in GlobalTrade SCM, illustrating how security, interceptors, transactions, JPA persistence, and exception handling coordinate across the multi-tier architecture.

---

## 1. End-to-End Runtime Flows

### Flow 1: Unauthenticated Secured Request (HTTP 401)
- **Scenario**: A client attempts to call a protected endpoint without providing the `Authorization` header.

```mermaid
sequenceDiagram
    autonumber
    actor Client as HTTP Client / Postman
    participant Payara as Payara Server (Port 8080)
    participant WebXML as web.xml Security Constraint

    Client->>Payara: GET /api/security/whoami (No Auth Header)
    Payara->>WebXML: Match URL pattern /api/security/*
    WebXML-->>Payara: Requires authenticated role (ADMIN, LOGISTICS_COORDINATOR, etc.)
    Payara-->>Client: HTTP 401 Unauthorized (Header: WWW-Authenticate: Basic realm="GlobalTradeCustomRealm")
```

---

### Flow 2: Successful Administrator Authentication (HTTP 200)
- **Scenario**: An administrator authenticates with `gt_admin` and requests identity summary.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin User (gt_admin)
    participant Payara as Payara Server
    participant Realm as GlobalTradeCustomRealm
    participant LM as GlobalTradeLoginModule
    participant DB as MySQL Database
    participant REST as SecurityVerificationResource

    Admin->>Payara: GET /api/security/whoami (Auth: Basic gt_admin:Password@123)
    Payara->>Realm: Delegate to custom realm
    Realm->>LM: Execute JAAS LoginModule "GlobalTradeCustomJaas"
    LM->>DB: Query app_users for gt_admin
    DB-->>LM: Password hash & active=true
    LM->>LM: Verify SHA-256 hash
    LM->>DB: Query user_roles for gt_admin
    DB-->>LM: Role: "ADMIN"
    LM->>Payara: commitUserAuthentication(["ADMIN"])
    Payara->>REST: Dispatch to getCallerIdentity()
    REST-->>Admin: HTTP 200 OK (Principal: "gt_admin", Roles: {"ADMIN": true})
```

---

### Flow 3: RBAC Role Authorization Rejection (HTTP 403)
- **Scenario**: A Customs Clearance Officer (`gt_customs`) attempts to execute an admin-only diagnostic operation.

```mermaid
sequenceDiagram
    autonumber
    actor Customs as Customs Agent (gt_customs)
    participant Payara as Payara Server
    participant REST as SecurityVerificationResource
    participant Service as SecurityVerificationServiceBean

    Customs->>Payara: POST /api/security/admin (Auth: gt_customs)
    Note over Payara: JAAS Authenticates: Principal "gt_customs", Role "CUSTOMS_AGENT"
    Payara->>REST: Dispatch to testAdminAccess()
    REST->>Service: performAdminOperation()
    Note over Service: Container checks @RolesAllowed(ADMIN)
    Note over Service: Caller lacks ADMIN role! Container throws EJBAccessException
    Service-->>REST: Throws EJBAccessException
    REST-->>Customs: HTTP 403 Forbidden ("Access Denied: Caller lacks required role")
```

---

### Flow 4: Fine-Grained Vendor Authorization (Allowed vs. Denied)
- **Scenario A (Allowed)**: `gt_vendor` requests assigned Vendor #1.
- **Scenario B (Denied)**: `gt_vendor` attempts to access competitor Vendor #2.

```mermaid
sequenceDiagram
    autonumber
    actor VendorUser as Vendor Rep (gt_vendor)
    participant REST as BusinessSecurityVerificationResource
    participant AuthBean as VendorAuthorizationServiceBean
    participant DB as MySQL (vendor_user_access)

    Note over VendorUser, DB: Scenario A: Accessing Assigned Profile (Vendor #1)
    VendorUser->>REST: GET /api/business-security/vendor/1 (Auth: gt_vendor)
    REST->>AuthBean: getVendorForAuthorizedCaller(1)
    AuthBean->>DB: SELECT COUNT(*) FROM vendor_user_access WHERE username='gt_vendor' AND vendor_id=1
    DB-->>AuthBean: Count = 1 (Authorized!)
    AuthBean->>DB: SELECT * FROM vendors WHERE id = 1
    DB-->>AuthBean: Vendor #1 Entity
    REST-->>VendorUser: HTTP 200 OK (Vendor #1 JSON)

    Note over VendorUser, DB: Scenario B: Accessing Competitor Profile (Vendor #2)
    VendorUser->>REST: GET /api/business-security/vendor/2 (Auth: gt_vendor)
    REST->>AuthBean: getVendorForAuthorizedCaller(2)
    AuthBean->>DB: SELECT COUNT(*) FROM vendor_user_access WHERE username='gt_vendor' AND vendor_id=2
    DB-->>AuthBean: Count = 0 (Violation!)
    AuthBean-->>REST: Throws VendorAccessDeniedException
    REST-->>VendorUser: HTTP 403 Forbidden ("Access denied to the requested vendor.")
```

---

### Flow 5: Multi-Step Shipment Dispatch Success
- **Scenario**: Logistics Coordinator dispatches a shipment with sufficient stock.

```mermaid
sequenceDiagram
    autonumber
    actor Coord as Logistics Coordinator
    participant REST as BusinessSecurityVerificationResource
    participant Interceptors as Interceptor Pipeline
    participant ShipBean as ShipmentServiceBean (REQUIRED)
    participant InvBean as InventoryServiceBean (MANDATORY)
    participant AuditBean as AuditServiceBean (REQUIRES_NEW)
    participant DB as MySQL Database

    Coord->>REST: POST /api/business-security/shipment/1/dispatch?quantity=10
    REST->>ShipBean: processShipmentDispatch(1, 1, 10, "gt_coordinator")
    Note over ShipBean: 1. CMT REQUIRED Transaction Begins
    ShipBean->>Interceptors: Execute validation, compliance, timing, audit
    Interceptors-->>ShipBean: Interceptors Cleared
    
    ShipBean->>InvBean: adjustStockInternal(1, -10) (MANDATORY)
    InvBean->>DB: SELECT quantity FROM inventory_items WHERE id = 1
    DB-->>InvBean: Current: 100
    InvBean->>DB: UPDATE inventory_items SET quantity = 90
    InvBean-->>ShipBean: Stock Adjusted
    
    ShipBean->>DB: UPDATE shipments SET shipment_status = 'IN_TRANSIT'
    
    ShipBean->>AuditBean: logAction("SHIPMENT_DISPATCH_SUCCESS", ...)
    Note over AuditBean: 2. REQUIRES_NEW Autonomous TX Commits to DB
    AuditBean->>DB: INSERT INTO audit_logs
    
    Note over ShipBean: 3. Parent REQUIRED TX Commits to DB
    ShipBean-->>REST: Updated Shipment
    REST-->>Coord: HTTP 200 OK (Status: IN_TRANSIT)
```

---

### Flow 6: Shipment Dispatch Rollback on Insufficient Stock
- **Scenario**: Dispatch requested with quantity exceeding available stock.

```mermaid
sequenceDiagram
    autonumber
    actor Coord as Logistics Coordinator
    participant ShipBean as ShipmentServiceBean (REQUIRED)
    participant InvBean as InventoryServiceBean (MANDATORY)
    participant AuditBean as AuditServiceBean (REQUIRES_NEW)
    participant Mapper as InsufficientInventoryExceptionMapper
    participant DB as MySQL Database

    Coord->>ShipBean: processShipmentDispatch(1, 1, 99999)
    Note over ShipBean: 1. Parent CMT REQUIRED Starts
    
    ShipBean->>InvBean: adjustStockInternal(1, -99999) (MANDATORY)
    InvBean->>DB: SELECT quantity FROM inventory_items WHERE id = 1
    DB-->>InvBean: Available: 100
    Note over InvBean: Shortage detected! Throws InsufficientInventoryException
    InvBean-->>ShipBean: Throws InsufficientInventoryException
    
    ShipBean->>AuditBean: logAction("DISPATCH_FAILED_INSUFFICIENT_STOCK", ...)
    Note over AuditBean: 2. REQUIRES_NEW Commits Failure Audit to MySQL!
    AuditBean->>DB: INSERT INTO audit_logs
    
    Note over ShipBean: 3. Parent REQUIRED Transaction ROLLED BACK!
    Note over DB: Stock remains 100, Shipment remains PENDING.
    
    ShipBean-->>Mapper: InsufficientInventoryException propagates
    Mapper-->>Coord: HTTP 409 Conflict (Structured ApiErrorResponse JSON)
```

---

### Flow 7: Fast-Fail Interceptor Validation (Rating > 5.00)
- **Scenario**: An operator submits a vendor rating of `9.50` (outside valid `0.00 - 5.00` range).

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrator
    participant ValInterceptor as BusinessValidationInterceptor
    participant Service as VendorServiceBean
    participant DB as MySQL Database

    Admin->>ValInterceptor: updatePerformanceRating(1, 9.50, "gt_admin")
    ValInterceptor->>ValInterceptor: Validate rating: 9.50 > 5.00 (Failed!)
    Note over ValInterceptor: Does NOT call context.proceed()!
    ValInterceptor-->>Admin: Throws IllegalArgumentException ("Rating must be between 0.00 and 5.00")
    Note over Service, DB: VendorServiceBean and MySQL are NEVER touched!
```

---

### Flow 8: Unknown REST Endpoint Route (Preserved HTTP 404)
- **Scenario**: A client requests an unmapped URL path.

```mermaid
sequenceDiagram
    autonumber
    actor Client as HTTP Client
    participant JAXRS as JAX-RS Routing Engine
    participant Mapper as WebApplicationExceptionMapper

    Client->>JAXRS: GET /api/unknown-service
    Note over JAXRS: Route unmapped! Throws NotFoundException
    JAXRS->>Mapper: toResponse(NotFoundException)
    Mapper-->>Client: HTTP 404 Not Found (ApiErrorResponse: "HTTP_404")
```

---

### Flow 9: Background Automated Timer Cycle
- **Scenario**: Payara Timer Engine triggers the 5-minute supply chain health check.

```mermaid
sequenceDiagram
    autonumber
    participant TimerEngine as Payara Timer Engine
    participant TimerBean as SupplyChainMonitoringTimerBean
    participant EM as EntityManager
    participant AuditBean as AuditServiceBean
    participant DB as MySQL Database

    TimerEngine->>TimerBean: @Schedule automaticMonitoringSchedule()
    Note over TimerBean: Container Starts CMT REQUIRED Transaction
    TimerBean->>EM: Query items where quantity <= reorderLevel
    EM->>DB: SELECT * FROM inventory_items WHERE quantity <= reorder_level
    DB-->>EM: List of low-stock items
    
    TimerBean->>TimerBean: Check 30-min alertCooldownCache
    
    TimerBean->>AuditBean: logAction("LOW_STOCK_DETECTED", ...)
    Note over AuditBean: REQUIRES_NEW Autonomous Commit
    AuditBean->>DB: INSERT INTO audit_logs
    Note over TimerBean: Monitoring cycle completed.
```

---

## 2. One Complete Mental Model (The Unified Enterprise Chain)

Every production request in GlobalTrade SCM travels through this unified architectural pipeline:

```mermaid
graph TD
    A["1. Client / Test Suite"] -->|HTTP Request + Basic Auth Header| B["2. Payara HTTP Listener (Port 8080)"]
    B --> C["3. JAAS Custom Security Realm (GlobalTradeCustomRealm)"]
    C -->|Validate against app_users & user_roles| D[("MySQL Database")]
    C -->|Attach Principal & Roles to SecurityContext| E["4. Web Layer: JAX-RS Resource (/api/*)"]
    E --> F["5. Declarative Security Check (@RolesAllowed)"]
    F --> G["6. Programmatic Fine-Grained Check (VendorAuthorizationServiceBean)"]
    G --> H["7. Interceptor Pipeline (Validation -> Compliance -> Metrics -> Audit)"]
    H --> I["8. JTA Transaction Coordinator (CMT REQUIRED / MANDATORY)"]
    I --> J["9. JPA Persistence Layer (EntityManager / GlobalTradePU)"]
    J --> K[("10. Relational Data Storage (MySQL: jdbc/GlobalTradeDS)")]
    H -.->|On Success or Failure| L["11. Autonomous Audit Logger (AuditServiceBean: REQUIRES_NEW)"]
    L --> K
    E -.->|On Exceptions| M["12. Centralized Exception Mappers (400, 403, 404, 409, 500)"]
    M -->|Sanitized JSON Error| A
    E -->|Formatted JSON Result| A
```
