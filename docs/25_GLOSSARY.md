# GlobalTrade SCM — Enterprise Terminology & Concept Glossary

This glossary provides concise definitions and project-specific contexts for all key enterprise Java, Jakarta EE, database, and security terms used across GlobalTrade SCM.

---

## Alphabetical Concept Index

### ACID
- **Simple Meaning**: A set of four properties (Atomicity, Consistency, Isolation, Durability) that guarantee database transactions are processed reliably.
- **How GlobalTrade Uses It**: Guaranteed during shipment dispatches and stock adjustments via JTA and MySQL InnoDB.

### Annotation
- **Simple Meaning**: Metadata tags starting with `@` in Java code that provide instructions to the compiler or runtime container without modifying business logic directly.
- **How GlobalTrade Uses It**: Configures EJBs (`@Stateless`), persistence (`@Entity`), transactions (`@TransactionAttribute`), and security (`@RolesAllowed`).

### API (Application Programming Interface)
- **Simple Meaning**: A structured set of rules and protocols allowing different software components to communicate.
- **How GlobalTrade Uses It**: 37 JAX-RS REST endpoints exposed under `/globaltrade/api/*`.

### `@ApplicationException`
- **Simple Meaning**: An EJB annotation defining whether a custom business exception should trigger a transaction rollback.
- **How GlobalTrade Uses It**: `InsufficientInventoryException` is configured with `rollback = true` to undo stock adjustments on shortages.

### Arquillian
- **Simple Meaning**: A testing platform for Jakarta EE that executes integration tests directly inside a live application server.
- **How GlobalTrade Uses It**: Deploys micro-test WARs to Payara Server 6 to execute our 16 automated integration tests (`*IT.java`).

### Authentication (AuthN)
- **Simple Meaning**: The security process of verifying the identity of a user or system ("Who are you?").
- **How GlobalTrade Uses It**: HTTP Basic Authentication verified by `GlobalTradeLoginModule` against SHA-256 password hashes in MySQL `app_users`.

### Authorization (AuthZ)
- **Simple Meaning**: The security process of verifying whether an authenticated user has permission to perform an action ("What are you allowed to do?").
- **How GlobalTrade Uses It**: Evaluated via `@RolesAllowed` annotations and programmatic checks in `VendorAuthorizationServiceBean`.

### BASIC Authentication
- **Simple Meaning**: An HTTP authentication protocol where clients send credentials in the `Authorization: Basic <base64>` header.
- **How GlobalTrade Uses It**: Configured in `web.xml` for local demo and test suite authentication.

### BMT (Bean-Managed Transactions)
- **Simple Meaning**: A transaction model where the developer explicitly manages transaction boundaries in code using `UserTransaction`.
- **How GlobalTrade Uses It**: Used in `InventoryReconciliationBean` to programmatically control `utx.begin()`, `utx.commit()`, and `utx.rollback()`.

### CDI (Contexts and Dependency Injection)
- **Simple Meaning**: The Jakarta EE standard for managing component lifecycles and injecting dependencies.
- **How GlobalTrade Uses It**: Enabled in test micro-archives and web modules via `beans.xml`.

### CMT (Container-Managed Transactions)
- **Simple Meaning**: A transaction model where Payara automatically starts, commits, or rolls back transactions based on annotations.
- **How GlobalTrade Uses It**: Default model used across all stateless session beans (`ShipmentServiceBean`, `InventoryServiceBean`).

### Commit
- **Simple Meaning**: The permanent saving of all database modifications made during a transaction.
- **How GlobalTrade Uses It**: Occurs automatically at the end of successful `REQUIRED` or `REQUIRES_NEW` EJB method executions.

### Container
- **Simple Meaning**: The application server runtime environment (Payara Server) providing lifecycle management, pooling, transactions, and security.
- **How GlobalTrade Uses It**: Hosts our deployed EAR, injects dependencies, and intercepts method calls.

### DataSource
- **Simple Meaning**: A configured object in the application server that manages physical connection pools to a database.
- **How GlobalTrade Uses It**: Named `jdbc/GlobalTradeDS`, connected to the MySQL `globaltrade_db` database.

### Dependency Injection (DI)
- **Simple Meaning**: Passing dependencies into an object automatically rather than constructing them manually with `new`.
- **How GlobalTrade Uses It**: `@EJB` injects session beans; `@PersistenceContext` injects EntityManager; `@Resource` injects SessionContext.

### EAR (Enterprise Archive)
- **Simple Meaning**: A standard `.ear` archive packaging multiple EJB JARs and Web WARs into a single deployable unit.
- **How GlobalTrade Uses It**: `globaltrade-ear/target/globaltrade.ear` packages `globaltrade-ejb.jar` and `globaltrade-web.war`.

### EJB (Enterprise JavaBean)
- **Simple Meaning**: Server-side Java components designed for business logic, transactional integrity, and multi-user concurrency.
- **How GlobalTrade Uses It**: Encapsulates logistics, inventory, customs, and audit operations.

### Entity
- **Simple Meaning**: A lightweight Java class mapped to a relational database table using JPA annotations (`@Entity`, `@Table`).
- **How GlobalTrade Uses It**: `Vendor`, `Warehouse`, `InventoryItem`, `Shipment`, `CustomsDocument`, `AuditLog`.

### `EntityManager`
- **Simple Meaning**: The primary JPA interface used to persist, find, merge, and remove database entities.
- **How GlobalTrade Uses It**: Injected via `@PersistenceContext` in stateless session beans to query MySQL.

### `ExceptionMapper`
- **Simple Meaning**: A JAX-RS provider interface that intercepts Java exceptions and converts them into structured HTTP responses.
- **How GlobalTrade Uses It**: Maps exceptions to status codes (`400`, `403`, `404`, `409`, `500`) with clean `ApiErrorResponse` JSON.

### Failsafe Plugin (`maven-failsafe-plugin`)
- **Simple Meaning**: A Maven plugin designed for running integration tests (`*IT.java`) during the `integration-test` and `verify` phases.
- **How GlobalTrade Uses It**: Executes our 16 Arquillian integration tests under the `arquillian-payara` profile.

### Foreign Key
- **Simple Meaning**: A database column linking a child table row to a primary key in a parent table to preserve referential integrity.
- **How GlobalTrade Uses It**: Links `shipments.vendor_id` to `vendors.id` and `inventory_items.warehouse_id` to `warehouses.id`.

### HTTP (Hypertext Transfer Protocol)
- **Simple Meaning**: The foundational application protocol used for distributed, collaborative, hypermedia information systems.
- **How GlobalTrade Uses It**: REST clients interact with the server over HTTP port 8080.

### Interceptor
- **Simple Meaning**: An aspect-oriented class that intercepts method invocations to execute cross-cutting logic before/after the business method.
- **How GlobalTrade Uses It**: `BusinessValidationInterceptor`, `TradeComplianceInterceptor`, `PerformanceMonitoringInterceptor`, `BusinessAuditInterceptor`.

### `InvocationContext`
- **Simple Meaning**: The context object passed into interceptor `@AroundInvoke` methods providing metadata and target parameters.
- **How GlobalTrade Uses It**: Inspects method parameters and controls execution via `context.proceed()`.

### JAAS (Java Authentication and Authorization Service)
- **Simple Meaning**: The standard Java security framework managing pluggable authentication realms and login modules.
- **How GlobalTrade Uses It**: Implemented via `GlobalTradeCustomRealm` and `GlobalTradeLoginModule`.

### JAR (Java Archive)
- **Simple Meaning**: A package file format aggregating multiple Java class files and metadata into a single file.
- **How GlobalTrade Uses It**: `globaltrade-ejb.jar` and `globaltrade-security-provider.jar`.

### Jakarta EE
- **Simple Meaning**: An open-source, enterprise-level Java computing platform managed by the Eclipse Foundation.
- **How GlobalTrade Uses It**: We use Jakarta EE 10 Full Platform specifications.

### JAX-RS (Jakarta RESTful Web Services)
- **Simple Meaning**: The Java specification for creating RESTful web services using annotations (`@Path`, `@GET`, `@POST`).
- **How GlobalTrade Uses It**: Powers all 37 endpoints across our 7 web resource controllers.

### JDBC (Java Database Connectivity)
- **Simple Meaning**: The standard Java API for connecting and executing queries against relational databases.
- **How GlobalTrade Uses It**: Managed by MySQL Connector/J in Payara Server.

### JNDI (Java Naming and Directory Interface)
- **Simple Meaning**: A directory service enabling Java components to look up resources by name.
- **How GlobalTrade Uses It**: Used to look up DataSource `jdbc/GlobalTradeDS`.

### JPA (Jakarta Persistence API)
- **Simple Meaning**: The standard framework for Object-Relational Mapping and database persistence in enterprise Java.
- **How GlobalTrade Uses It**: Managed by EclipseLink under persistence unit `GlobalTradePU`.

### JPQL (Java Persistence Query Language)
- **Simple Meaning**: An object-oriented query language used to define database queries against JPA entity objects rather than tables.
- **How GlobalTrade Uses It**: `SELECT v FROM Vendor v WHERE v.status = :status`.

### JTA (Jakarta Transactions API)
- **Simple Meaning**: The Java specification managing distributed transactions across multiple resources.
- **How GlobalTrade Uses It**: Coordinates Container-Managed Transactions across Payara and MySQL.

### JUnit 5
- **Simple Meaning**: The modern Java testing framework providing annotations and test runner engines.
- **How GlobalTrade Uses It**: Executes unit assertions alongside Arquillian.

### `LoginModule`
- **Simple Meaning**: A JAAS SPI component responsible for verifying user credentials and populating roles.
- **How GlobalTrade Uses It**: `GlobalTradeLoginModule` computes SHA-256 hashes and verifies credentials against MySQL `app_users`.

### `MANDATORY`
- **Simple Meaning**: A transaction attribute requiring an existing active transaction; throws an exception if none exists.
- **How GlobalTrade Uses It**: Applied to `adjustStockInternal` in `InventoryServiceBean`.

### Maven
- **Simple Meaning**: A build automation and dependency management tool based on the Project Object Model (`pom.xml`).
- **How GlobalTrade Uses It**: Manages our 4-module reactor build.

### MySQL
- **Simple Meaning**: An open-source relational database management system (RDBMS) implementing ACID transactions.
- **How GlobalTrade Uses It**: Permanent storage for all 10 domain and security tables (`globaltrade_db`).

### ORM (Object-Relational Mapping)
- **Simple Meaning**: A technique that maps object-oriented Java classes to relational database tables.
- **How GlobalTrade Uses It**: Handled automatically by EclipseLink JPA.

### Payara Server
- **Simple Meaning**: An enterprise-grade, cloud-native application server derived from GlassFish.
- **How GlobalTrade Uses It**: We use Payara Server 6.2025.11 (Community Edition).

### Persistence Context
- **Simple Meaning**: The first-level in-memory cache managed by `EntityManager` where entity instances are managed.
- **How GlobalTrade Uses It**: Tracks entity state changes during transactions before flushing to MySQL.

### Persistence Unit
- **Simple Meaning**: A named group of entity classes defined in `persistence.xml` pointing to a DataSource.
- **How GlobalTrade Uses It**: Named `GlobalTradePU`.

### `PreparedStatement`
- **Simple Meaning**: A precompiled SQL statement that safely binds parameters to prevent SQL injection.
- **How GlobalTrade Uses It**: Used in `GlobalTradeLoginModule` and `GlobalTradeCustomRealm`.

### Primary Key
- **Simple Meaning**: A unique identifier column for each record in a database table.
- **How GlobalTrade Uses It**: `id BIGINT AUTO_INCREMENT PRIMARY KEY` across all domain tables.

### `Principal`
- **Simple Meaning**: An object representing the authenticated identity of a user in the security context.
- **How GlobalTrade Uses It**: Retrieved via `sessionContext.getCallerPrincipal().getName()` (e.g. `"gt_admin"`).

### RBAC (Role-Based Access Control)
- **Simple Meaning**: An access-control mechanism restricting system access to authorized users based on roles.
- **How GlobalTrade Uses It**: 7 enterprise roles (`ADMIN`, `LOGISTICS_COORDINATOR`, `CUSTOMS_AGENT`, `WAREHOUSE_MANAGER`, `VENDOR_REPRESENTATIVE`, `CUSTOMER`, `SYSTEM`).

### Realm
- **Simple Meaning**: An application server security domain managing user credential repositories and group mappings.
- **How GlobalTrade Uses It**: `GlobalTradeCustomRealm` configured in Payara Server.

### `REQUIRED`
- **Simple Meaning**: The default transaction attribute; joins an existing transaction or creates a new one if none exists.
- **How GlobalTrade Uses It**: Applied to business entry methods like `processShipmentDispatch`.

### `REQUIRES_NEW`
- **Simple Meaning**: A transaction attribute that suspends any active transaction and runs in an independent new transaction.
- **How GlobalTrade Uses It**: Applied to `AuditServiceBean.logAction` so audit records survive parent rollbacks.

### REST (Representational State Transfer)
- **Simple Meaning**: An architectural style for stateless, client-server distributed web services using HTTP verbs.
- **How GlobalTrade Uses It**: Exposes JSON endpoints using `@GET`, `@POST`, and `@DELETE`.

### Rollback
- **Simple Meaning**: Reverting all database changes made during a failed transaction to maintain consistency.
- **How GlobalTrade Uses It**: Triggered on `InsufficientInventoryException` during dispatches.

### Role
- **Simple Meaning**: A named permission group assigned to users that defines what actions and resources they can access.
- **How GlobalTrade Uses It**: 7 standard enterprise roles (`ADMIN`, `LOGISTICS_COORDINATOR`, `CUSTOMS_AGENT`, `WAREHOUSE_MANAGER`, `VENDOR_REPRESENTATIVE`, `CUSTOMER`, `SYSTEM`) declared in `SecurityRoles.java` and assigned in MySQL `user_roles`.

### `SessionContext`
- **Simple Meaning**: An interface providing access to container-managed runtime context inside an EJB.
- **How GlobalTrade Uses It**: Injected via `@Resource` to check `isCallerInRole()` and `getCallerPrincipal()`.

### ShrinkWrap
- **Simple Meaning**: A Java library used by Arquillian to construct micro-archives (`.war`) in memory for testing.
- **How GlobalTrade Uses It**: `TestDeployments.java` constructs isolated test archives.

### Stateless Session Bean
- **Simple Meaning**: An EJB with no client-specific conversational state, pooled by the container.
- **How GlobalTrade Uses It**: Annotated with `@Stateless` for high throughput and thread safety.

### Surefire Plugin (`maven-surefire-plugin`)
- **Simple Meaning**: A Maven plugin that executes unit tests (`*Test.java`) during the `test` phase.
- **How GlobalTrade Uses It**: Used for standard build lifecycle phases.

### Timer Service
- **Simple Meaning**: A container-managed service that schedules background jobs using declarative `@Schedule` or programmatic timers.
- **How GlobalTrade Uses It**: `SupplyChainMonitoringTimerBean` (5-min check) and `ShipmentAlertTimerBean` (single-action alert).

### Transaction
- **Simple Meaning**: A single logical unit of work consisting of one or more database operations that must all succeed together (commit) or all be undone together (rollback).
- **How GlobalTrade Uses It**: Managed automatically via JTA Container-Managed Transactions (CMT) to ensure inventory deductions, shipment dispatches, and audit entries maintain ACID integrity.

### `UserTransaction`
- **Simple Meaning**: The JTA interface used in Bean-Managed Transactions to programmatically control commit and rollback.
- **How GlobalTrade Uses It**: Injected into `InventoryReconciliationBean`.

### WAR (Web Archive)
- **Simple Meaning**: A package file format containing web resources, servlets, and JAX-RS endpoints.
- **How GlobalTrade Uses It**: `globaltrade-web.war`.

---

## Project-Specific Constants & Identifiers

- **`GlobalTradePU`**: The JPA Persistence Unit defined in `persistence.xml`.
- **`jdbc/GlobalTradeDS`**: The JNDI resource name pointing to the MySQL connection pool.
- **`GlobalTradeCustomRealm`**: The custom Payara security realm name extending `AppservRealm`.
- **`GlobalTradeCustomJaas`**: The JAAS context identifier configured in `login.conf`.
- **`GlobalTradeLoginModule`**: The custom JAAS login module class extending `AppservPasswordLoginModule`.
- **`vendor_user_access`**: The MySQL mapping table used for fine-grained vendor data isolation (`username` $\rightarrow$ `vendor_id`).
