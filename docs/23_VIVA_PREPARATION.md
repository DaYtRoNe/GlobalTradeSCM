# GlobalTrade SCM — Viva Examination Preparation Guide

This document is a comprehensive study guide designed to prepare a student to confidently explain and defend the GlobalTrade Supply Chain Management System during a viva examination.

---

## 1. Project Elevator Pitches

### 1.1 The 30-Second Explanation
> *"GlobalTrade SCM is an enterprise Jakarta EE 10 supply chain system deployed on Payara Server 6. It manages international shipments, customs documentation, and inventory across global warehouses. The project demonstrates core enterprise patterns: JPA persistence, Container-Managed Transactions with autonomous audit logging, EJB timer automation, decoupled interceptors, custom JAAS database authentication with fine-grained vendor data isolation, and automated in-container integration testing using JUnit 5 and Arquillian."*

### 1.2 The 2-Minute Explanation
> *"GlobalTrade SCM addresses the complexities of cross-border trade, where shipments depend on inventory availability, customs clearance, and multi-tenant supplier access.
> 
> Architecturally, it follows a clean multi-tier design:
> 1. **Web Tier**: JAX-RS REST endpoints provide 37 verification and business interfaces with centralized exception mappers.
> 2. **Business Tier**: Stateless EJBs orchestrate operations like multi-step shipment dispatches. Container-Managed Transactions (`REQUIRED` and `MANDATORY`) ensure stock adjustments and status updates are atomic, while `REQUIRES_NEW` guarantees audit trails commit even if business operations roll back.
> 3. **Automation & Cross-Cutting Tier**: Declarative 5-minute timers monitor low inventory and customs delays, while an `@AroundInvoke` interceptor pipeline handles fast-fail validation and latency metrics.
> 4. **Security Tier**: A custom Payara JAAS Realm and LoginModule authenticates users against SHA-256 password hashes in MySQL, enforcing both declarative `@RolesAllowed` and fine-grained data isolation preventing external vendors from viewing competitor records.
> 5. **Testing**: 16 in-container Arquillian integration tests prove live transaction rollbacks, interceptors, and JAAS security on a running Payara instance."*

### 1.3 The 5-Minute Technical Deep Dive
> (See `docs/04_SYSTEM_ARCHITECTURE.md` and `docs/16_REQUEST_AND_BUSINESS_FLOWS.md` for full end-to-end walkthrough).

---

## 2. Categorized Viva Questions & Model Answers

---

### LEVEL 1: Project Basics

#### Q1.1: What problem does GlobalTrade SCM solve?
- **Short Answer**: It automates and coordinates international supply chain operations, inventory tracking, customs clearance, and vendor collaboration with atomic transactions and role-based security.
- **Better Explanation**: In cross-border logistics, operations involve multiple external vendors, customs officials, and warehouse managers. A failure in one step (e.g. stock shortage) must not leave partial records. GlobalTrade ensures ACID compliance, data isolation between competing vendors, and independent audit logging.
- **Code Evidence**: `ShipmentServiceBean.java`, `InventoryServiceBean.java`, `schema.sql`.

---

### LEVEL 2: Java & Jakarta EE Basics

#### Q2.1: What is Jakarta EE, and why did we use Payara Server?
- **Short Answer**: Jakarta EE is the industry standard for enterprise Java application development. Payara Server 6 is a certified Jakarta EE 10 application server that provides built-in enterprise services (EJB pooling, JTA transactions, JAAS security, JPA/EclipseLink).
- **Better Explanation**: Unlike lightweight web servers like Tomcat that only provide a Servlet container, Payara is a Full Platform Application Server. It natively manages transaction coordinators, EJB lifecycle pooling, JNDI resource trees, and security realms without requiring third-party libraries.
- **Code Evidence**: `pom.xml` (`jakarta.jakartaee-api:10.0.0`), `glassfish-application.xml`.

#### Q2.2: What is Dependency Injection in Jakarta EE?
- **Short Answer**: It is a design pattern where the container injects dependencies (`@EJB`, `@PersistenceContext`, `@Resource`) into components at runtime instead of creating them manually with `new`.
- **Better Explanation**: Manual instantiation (`new MyService()`) bypasses container interceptors, security checks, and transaction boundaries. Injected references are proxied by Payara, enabling declarative features like `@RolesAllowed` and `@TransactionAttribute`.
- **Code Evidence**: `@EJB private ShipmentServiceBean shipmentService;` in `TransactionVerificationResource.java`.

---

### LEVEL 3: JPA Persistence

#### Q3.1: What is JPA, EntityManager, and a Persistence Unit?
- **Short Answer**: JPA is the Java standard for Object-Relational Mapping (ORM). `EntityManager` is the API used to interact with the database (persist, find, merge, remove). A persistence unit is a named configuration (`GlobalTradePU`) in `persistence.xml` pointing to DataSource `jdbc/GlobalTradeDS`.
- **Better Explanation**: JPA maps Java entity classes (`Vendor`, `Shipment`) to relational MySQL tables. The `EntityManager` manages the entity lifecycle in the Persistence Context (a first-level cache). At transaction commit, changes are synchronized to MySQL via SQL statements.
- **Code Evidence**: `globaltrade-ejb/src/main/resources/META-INF/persistence.xml`.

---

### LEVEL 4: EJB Session Beans

#### Q4.1: What is an EJB and why did we use `@Stateless` session beans?
- **Short Answer**: Enterprise JavaBeans (EJBs) are server-side components encapsulating business logic. `@Stateless` beans do not maintain client conversational state between method invocations.
- **Better Explanation**: Stateless session beans are pooled by Payara. Any available pooled instance can serve any incoming client request. This maximizes server throughput, reduces memory overhead, and allows Payara to wrap method executions with transactions and security interceptors automatically.
- **Code Evidence**: `@Stateless public class ShipmentServiceBean ...`.

---

### LEVEL 5: Transaction Management (CMT & BMT)

#### Q5.1: What is the difference between CMT and BMT?
- **Short Answer**: In Container-Managed Transactions (CMT), Payara starts, commits, and rolls back transactions automatically based on annotations. In Bean-Managed Transactions (BMT), developer code explicitly controls boundaries using `UserTransaction` (`begin()`, `commit()`, `rollback()`).
- **Better Explanation**: CMT is used for 95% of business logic because it is declarative and less error-prone. BMT is used in specialized batch routines (e.g. `InventoryReconciliationBean`) where partial commits or custom retry logic across multiple items are required.
- **Code Evidence**: CMT in `ShipmentServiceBean.java`; BMT in `InventoryReconciliationBean.java`.

#### Q5.2: Why do we use `REQUIRED`, `MANDATORY`, and `REQUIRES_NEW`?
- **Short Answer**:
  - `REQUIRED`: Joins the caller's transaction or creates a new one if none exists (used on entry points like `processShipmentDispatch`).
  - `MANDATORY`: Demands an existing transaction; throws `EJBTransactionRequiredException` if called without one (used on internal helpers like `adjustStockInternal`).
  - `REQUIRES_NEW`: Suspends any active transaction and executes in an independent new transaction (used on `AuditServiceBean` so audit logs survive rollbacks).
- **Better Explanation**: If a dispatch fails due to an inventory shortage, the outer `REQUIRED` transaction rolls back to keep inventory untouched. However, `AuditServiceBean.logAction` runs in a separate `REQUIRES_NEW` transaction that commits immediately, ensuring a permanent compliance record of the failed attempt.
- **Code Evidence**: `AuditServiceBean.java` (`@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)`).
- **Demo Option**: Execute `POST /api/transactions/dispatch/fail` and verify `rollbackVerified: true` and `independentAuditCommitted: true`.

#### Q5.3: What is `@ApplicationException`?
- **Short Answer**: An annotation on custom exception classes telling the EJB container whether the exception should cause a transaction rollback.
- **Better Explanation**: By default, checked exceptions in Java do *not* cause EJB rollbacks, while unchecked `RuntimeException`s do. `@ApplicationException(rollback = true)` instructs Payara to roll back CMT transactions when business exceptions like `InsufficientInventoryException` occur.
- **Code Evidence**: `InsufficientInventoryException.java` (`@ApplicationException(rollback = true)`).

---

### LEVEL 6: EJB Timer Services

#### Q6.1: What is the difference between Declarative and Programmatic Timers?
- **Short Answer**: Declarative timers use the `@Schedule` annotation for fixed recurring schedules (e.g. every 5 minutes). Programmatic timers use `TimerService.createSingleActionTimer()` for dynamic, event-driven delays (e.g. 5-second expedite alert).
- **Better Explanation**: Declarative timers are initialized automatically by Payara at deployment time. Programmatic timers are created dynamically at runtime in response to business events, can store custom serializable info objects (`AlertTimerInfo`), and can be cancelled programmatically.
- **Code Evidence**: `SupplyChainMonitoringTimerBean.java` (`@Schedule`); `ShipmentAlertTimerBean.java` (`timerService.createSingleActionTimer`).

---

### LEVEL 7: Interceptors

#### Q7.1: What is an EJB Interceptor and what does `context.proceed()` do?
- **Short Answer**: An interceptor is an aspect-oriented class that intercepts EJB method invocations. `context.proceed()` passes execution to the next interceptor in the chain or to the target EJB method.
- **Better Explanation**: Interceptors decouple cross-cutting concerns (validation, metrics, auditing) from core business logic. If an interceptor detects invalid input (e.g. rating $9.99 > 5.00$ in `BusinessValidationInterceptor`), it throws an exception *without* calling `context.proceed()`, preventing the business method and database from ever being touched.
- **Code Evidence**: `BusinessValidationInterceptor.java`, `PerformanceMonitoringInterceptor.java`.

---

### LEVEL 8: Security & JAAS

#### Q8.1: What is the difference between Authentication and Authorization?
- **Short Answer**: Authentication verifies *identity* ("Who are you?"), while Authorization verifies *permissions* ("What are you allowed to do?").
- **Better Explanation**: Authentication verifies the username and password against `app_users` and produces a `Principal`. Authorization checks whether that `Principal` has the necessary `@RolesAllowed` role or data-ownership mapping in `vendor_user_access`.
- **Code Evidence**: `GlobalTradeLoginModule.java` (Authentication); `VendorAuthorizationServiceBean.java` (Authorization).

#### Q8.2: What is JAAS and why did we build a custom Realm and LoginModule?
- **Short Answer**: JAAS (Java Authentication and Authorization Service) is the standard security framework in Java. We built `GlobalTradeCustomRealm` and `GlobalTradeLoginModule` to bridge Payara's HTTP Basic security directly to our MySQL database tables (`app_users`, `user_roles`) with SHA-256 hashing and active account validation.
- **Better Explanation**: Standard file or JDBC realms do not accommodate custom business requirements like checking an `active` account flag or mapping custom password digest algorithms. Our custom provider integrates directly into Payara's security SPI.
- **Code Evidence**: `GlobalTradeCustomRealm.java`, `GlobalTradeLoginModule.java`.

#### Q8.3: How does `gt_vendor` only see Vendor #1 data?
- **Short Answer**: Through programmatic authorization in `VendorAuthorizationServiceBean`, which queries the `vendor_user_access` mapping table.
- **Better Explanation**: `@RolesAllowed(VENDOR_REPRESENTATIVE)` only confirms the user is a vendor; it does not know *which* vendor. In `VendorAuthorizationServiceBean`, if the caller is a `VENDOR_REPRESENTATIVE`, the bean queries `SELECT COUNT(*) FROM vendor_user_access WHERE username = ? AND vendor_id = ?`. If the count is 0, it throws `VendorAccessDeniedException` (HTTP 403).
- **Code Evidence**: `VendorAuthorizationServiceBean.java`.
- **Demo Option**: Call `GET /api/business-security/vendor/1` (200 OK) then `GET /api/business-security/vendor/2` (403 Forbidden) with user `gt_vendor`.

---

### LEVEL 9: Exception Handling

#### Q9.1: What is a JAX-RS `ExceptionMapper`?
- **Short Answer**: A provider component (`@Provider ExceptionMapper<T>`) that catches uncaught Java exceptions thrown by REST resources or EJBs and converts them into standardized HTTP responses.
- **Better Explanation**: `ExceptionMapper` decouples error formatting from business code. It translates domain exceptions to standard HTTP codes (`400`, `403`, `404`, `409`, `500`) and produces clean `ApiErrorResponse` JSON without leaking Java stack traces or database schema names.
- **Code Evidence**: `GenericExceptionMapper.java`, `InsufficientInventoryExceptionMapper.java`, `ApiErrorResponse.java`.

---

### LEVEL 10: Testing & Arquillian

#### Q10.1: Why Arquillian instead of standard unit tests with Mockito?
- **Short Answer**: Unit tests with Mockito only test simulated objects. Arquillian deploys real micro-archives into a running Payara 6 application server to prove real JTA rollbacks, JPA queries, and JAAS security on MySQL.
- **Better Explanation**: Container features like transaction boundaries, JPA entity caches, interceptor chains, and security realms cannot be reliably tested in isolated JVM unit tests. Arquillian bridges JUnit 5 to the live application server.
- **Code Evidence**: `globaltrade-ejb/src/test/java/com/jiat/globaltrade/test/*`.

#### Q10.2: What does the 16/16 test result prove?
- **Short Answer**: It proves that 100% of our automated integration tests passed across all core subsystems: container injection, JPA persistence, CMT rollback, interceptors, and JAAS/RBAC security.
- **Code Evidence**: `mvn -Parquillian-payara -pl globaltrade-ejb verify`.

---

## 3. "Trap Questions" (Proving Genuine Understanding)

### Trap Question 1: "Why is `globaltrade-security-provider.jar` outside the EAR in Payara's `domain/lib`?"
> **Strong Answer**: *"Security realms are part of the application server infrastructure, loaded by the Server-Level ClassLoader when Payara starts up—before any individual application is deployed. If the provider JAR were inside the EAR, Payara's realm catalog could not locate the realm class during server boot, and authentication would fail."*

### Trap Question 2: "If an exception occurs in `ShipmentServiceBean`, why doesn't the `audit_logs` record get rolled back too?"
> **Strong Answer**: *"Because `AuditServiceBean.logAction` is annotated with `@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)`. When invoked, Payara suspends the parent `REQUIRED` transaction, starts a new independent transaction for the audit log, commits it immediately to MySQL, and then resumes the parent transaction. When the parent transaction later rolls back, the committed audit record remains intact."*

### Trap Question 3: "Is HTTP Basic Authentication with SHA-256 secure for production?"
> **Strong Answer**: *"No. For this academic project, HTTP Basic over HTTP and unsalted SHA-256 are used for demonstration and automated testing simplicity. In production, we must mandate HTTPS/TLS 1.3 to encrypt headers in transit, use slow salted hashing algorithms like Argon2id or bcrypt, and integrate an enterprise IdP (like Keycloak) with MFA."*
