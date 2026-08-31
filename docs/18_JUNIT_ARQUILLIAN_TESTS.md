# GlobalTrade SCM — JUnit 5 & Arquillian Integration Tests Guide

This document details the architecture, design patterns, and test methods of the automated in-container integration test suite in GlobalTrade SCM.

---

## 1. JUnit 5 & Arquillian Fundamentals

### 1.1 What is JUnit 5?
**JUnit 5** is the modern testing framework for Java, providing annotations such as `@Test`, `@DisplayName`, and the `@ExtendWith` extension model.

### 1.2 What is Arquillian?
**Arquillian** is an enterprise testing platform for Jakarta EE. Rather than mocking container services, Arquillian:
1. Bundles test classes, entities, services, and XML descriptors into a lightweight archive using **ShrinkWrap**.
2. Deploys the archive to a live running application server (Payara Server 6).
3. Executes the test methods directly within the container (or as an external HTTP client).
4. Captures assertions, reports results to Maven/JUnit, and undeploys the test archive.

```mermaid
graph TD
    subgraph MavenRunner["Maven Test Runner (Client JVM)"]
        JUnit5["JUnit 5 Jupiter Engine"]
        ArqExt["ArquillianExtension (@ExtendWith)"]
        Adapter["arquillian-payara-server-remote"]
    end

    subgraph PayaraServer["Payara Server 6 (localhost:4848 / 8080)"]
        AdminPort["Admin Port (4848): Deployment Manager"]
        HTTPPort["HTTP Port (8080): Web & REST Endpoints"]
        TestWAR["Deployed Test Micro-Archive<br/>(ShrinkWrap WebArchive)"]
        ContainerEJBs["EJB Container & Interceptors"]
        JPA["EntityManager (GlobalTradePU)"]
        Realm["GlobalTradeCustomRealm (JAAS)"]
    end

    subgraph Database["MySQL Data Tier"]
        MySQL[(MySQL: jdbc/GlobalTradeDS)]
    end

    JUnit5 --> ArqExt
    ArqExt --> Adapter
    Adapter -->|Deploy Test WAR via REST Admin API| AdminPort
    AdminPort --> TestWAR
    TestWAR --> ContainerEJBs
    TestWAR --> Realm
    ContainerEJBs --> JPA
    JPA --> MySQL
    Realm --> MySQL
    Adapter -.->|@RunAsClient HTTP Basic Requests| HTTPPort
    HTTPPort --> TestWAR
```

---

## 2. In-Container vs. Client-Side Testing Modes

Arquillian supports two execution modes:

| Mode | Annotation | Injection Mechanism | Use in GlobalTrade SCM |
| :--- | :--- | :--- | :--- |
| **In-Container** | Default (No annotation) | `@EJB` injected directly into test class | Used in `PersistenceIntegrationIT`, `TransactionRollbackIntegrationIT`, `BusinessValidationInterceptorIT`, and `ArquillianContainerSmokeIT`. |
| **Client-Side** | `@RunAsClient` | `@ArquillianResource URL deploymentUrl` | Used in `SecurityAuthenticationIT` to send real HTTP requests over the network with `Authorization: Basic` headers. |

---

## 3. Detailed Audit of the 16 Integration Tests

### 3.1 `ArquillianContainerSmokeIT.java` (1 Test)
*Proves foundational JUnit 5 + Arquillian connectivity to remote Payara Server.*

- **`shouldInjectSystemHealthBeanAndVerifyStatus()`**:
  - **Purpose**: Verifies that Arquillian can deploy a test archive, that Payara's EJB container injects `SystemHealthBean`, and that its method executes.
  - **Setup**: ShrinkWrap creates `globaltrade-smoke-test.war` with `SystemHealthBean.class` and `beans.xml`.
  - **Action**: Invokes `systemHealthBean.getStatus()`.
  - **Expected Result**: Injected bean is non-null; returns `"GlobalTrade EJB Module is running"`.

---

### 3.2 `PersistenceIntegrationIT.java` (3 Tests)
*Proves JPA / EclipseLink connectivity, EntityManager injection, and MySQL query execution.*

- **`shouldVerifyDatabaseConnectivity()`**:
  - **Purpose**: Verifies live database connectivity through EntityManager via `SELECT 1`.
  - **Action**: Invokes `supplyChainDataService.isDatabaseConnected()`.
  - **Expected Result**: Returns `true`.
- **`shouldQueryVendorCountSuccessfully()`**:
  - **Purpose**: Verifies entity mapping and JPQL query execution against the seeded database.
  - **Action**: Invokes `supplyChainDataService.getVendorCount()`.
  - **Expected Result**: Count $\ge 1$.
- **`shouldConfirmPersistenceUnitStatus()`**:
  - **Purpose**: Verifies active `GlobalTradePU` persistence unit configuration.
  - **Action**: Invokes `supplyChainDataService.getPersistenceStatus()`.
  - **Expected Result**: Returns string containing `"GlobalTradePU"`.

---

### 3.3 `TransactionRollbackIntegrationIT.java` (1 Test)
*Proves EJB Container-Managed Transaction (CMT) rollback semantics and autonomous REQUIRES_NEW audit logging.*

- **`shouldRollbackBusinessTransactionAndCommitRequiresNewAudit()`**:
  - **Purpose**: Verifies that an impossible dispatch rolls back the parent transaction while the independent audit log commits.
  - **Setup**: Captures initial inventory quantity for Item #1, initial status for Shipment #1, and initial audit log count.
  - **Action**: Calls `adminTestInvoker.processShipmentDispatch(1L, 1L, initialQty + 999999, "ARQUILLIAN_IT_RUNNER")`.
  - **Expected Result**:
    1. Throws `InsufficientInventoryException` (`@ApplicationException(rollback = true)`).
    2. Inventory quantity and shipment status remain unmodified in MySQL (CMT `REQUIRED` rollback).
    3. Final audit log count increases by 1 because `AuditServiceBean` executes under `REQUIRES_NEW`.

---

### 3.4 `BusinessValidationInterceptorIT.java` (1 Test)
*Proves EJB interceptor chaining and fast-fail input validation.*

- **`shouldInterceptAndRejectInvalidRating()`**:
  - **Purpose**: Verifies that `BusinessValidationInterceptor` intercepts and rejects an invalid vendor rating before the business method executes.
  - **Setup**: Captures initial rating of Vendor #1.
  - **Action**: Calls `adminTestInvoker.updatePerformanceRating(1L, 9.99, "ARQUILLIAN_IT_RUNNER")`.
  - **Expected Result**: Throws `IllegalArgumentException` with message mentioning rating range; vendor rating in database remains unchanged.

---

### 3.5 `SecurityAuthenticationIT.java` (10 Tests — `@RunAsClient`)
*Proves real Payara container security, JAAS Realm, HTTP Basic Auth, RBAC, and vendor data isolation.*

| Test Method | Request Path | Credentials | Expected Status | What It Proves |
| :--- | :--- | :--- | :---: | :--- |
| **`testNoCredentials_shouldReturn401`** | `/security-test/whoami` | None | **`401`** | Unauthenticated requests are challenged with HTTP 401. |
| **`testValidAdminCredentials_shouldReturn200`** | `/security-test/whoami` | `gt_admin:Password@123` | **`200`** | JAAS authenticates admin and confirms `ADMIN` role. |
| **`testWrongPassword_shouldReturn401`** | `/security-test/whoami` | `gt_admin:WrongPassword@999` | **`401`** | Invalid password rejected by SHA-256 comparison. |
| **`testCustomsUserOnAdminEndpoint_shouldReturn403`** | `/security-test/admin` | `gt_customs:Password@123` | **`403`** | Non-admin user denied access to admin-only endpoint. |
| **`testAdminOnAdminEndpoint_shouldReturn200`** | `/security-test/admin` | `gt_admin:Password@123` | **`200`** | Admin user granted access to admin endpoint. |
| **`testCustomsUserOnCustomsEndpoint_shouldReturn200`**| `/security-test/customs` | `gt_customs:Password@123` | **`200`** | Customs agent granted access to customs endpoint. |
| **`testWarehouseUserOnCustomsEndpoint_shouldReturn403`**| `/security-test/customs` | `gt_warehouse:Password@123`| **`403`** | Warehouse manager denied access to customs endpoint. |
| **`testVendorAccessingMappedVendor_shouldReturn200`** | `/security-test/vendor/1` | `gt_vendor:Password@123` | **`200`** | Vendor user authorized to access mapped Vendor #1. |
| **`testVendorAccessingUnmappedVendor_shouldReturn403`** | `/security-test/vendor/2` | `gt_vendor:Password@123` | **`403`** | Vendor user denied cross-vendor access to Vendor #2. |
| **`testAdminAccessingVendor2_shouldReturn200`** | `/security-test/vendor/2` | `gt_admin:Password@123` | **`200`** | Admin user possesses global access to all vendors. |

---

## 4. Test Helper Architecture

### 4.1 `AdminTestInvoker.java`
- **Location**: `globaltrade-ejb/src/test/java/com/jiat/globaltrade/test/AdminTestInvoker.java`
- **Purpose**: Annotated with `@RunAs(SecurityRoles.ADMIN)` and `@PermitAll`.
- **Why It Exists**: Allows in-container test runners (which run anonymously inside the container) to invoke business EJBs protected by `@RolesAllowed(ADMIN)` without modifying or weakening production security annotations.
- **Important**: This is strictly a **TEST-ONLY helper**. It does *not* replace real authentication tests. Real authentication is tested via HTTP in `SecurityAuthenticationIT`.

### 4.2 `SecurityTestProbeServlet.java`
- **Location**: `globaltrade-ejb/src/test/java/com/jiat/globaltrade/test/SecurityTestProbeServlet.java`
- **Purpose**: A test-only HTTP Servlet mapped to `/security-test/*`.
- **Why It Exists**: Exercises real HTTP Basic Authentication and calls `req.getUserPrincipal()`, `req.isUserInRole()`, and `vendorAuthService.getVendorForAuthorizedCaller(vendorId)` in a live web container environment.

### 4.3 `TestDeployments.java`
- **Location**: `globaltrade-ejb/src/test/java/com/jiat/globaltrade/test/TestDeployments.java`
- **Purpose**: A ShrinkWrap deployment factory that bundles entities, exceptions, interceptors, services, security definitions, `persistence.xml`, `beans.xml`, test `web.xml`, and `glassfish-web.xml`.
- **`findException(Throwable, Class<T>)`**: A utility method that traverses exception cause chains to unwrap container-wrapped exceptions (e.g. `EJBException`).

---

## 5. Execution Commands & Expected Output

### Run Full Integration Test Suite:
```bash
mvn -Parquillian-payara -pl globaltrade-ejb verify
```

### Expected Output:
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.jiat.globaltrade.test.ArquillianContainerSmokeIT
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.jiat.globaltrade.test.PersistenceIntegrationIT
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.jiat.globaltrade.test.TransactionRollbackIntegrationIT
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.jiat.globaltrade.test.BusinessValidationInterceptorIT
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.jiat.globaltrade.test.SecurityAuthenticationIT
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

---

## 6. Common Viva Questions & Model Answers

### Q1: Why did you use Arquillian instead of Mockito for testing?
> **Answer**: Mockito replaces real components with simulated objects, which only verify what the developer assumes. It cannot verify whether Payara Server actually rolls back a JTA transaction, whether our custom JAAS realm verifies password hashes against MySQL, or whether JPA entity mappings generate valid SQL. Arquillian deploys real archives into a running Payara server to prove live enterprise behavior.

### Q2: What is the purpose of ShrinkWrap?
> **Answer**: ShrinkWrap is a Java API for creating archive files (like `.war` or `.jar`) programmatically in memory. Instead of building and deploying the entire application for every test, ShrinkWrap creates micro-archives containing only the specific EJBs, entities, and XML descriptors required for that test suite, making integration tests faster and isolated.

### Q3: What does the `@RunAsClient` annotation do in Arquillian?
> **Answer**: By default, Arquillian executes test methods inside the application server container. The `@RunAsClient` annotation instructs Arquillian to execute the test on the client-side JVM, allowing it to send real HTTP requests over the network with headers like `Authorization: Basic` to test container security endpoints from an external client's perspective.
