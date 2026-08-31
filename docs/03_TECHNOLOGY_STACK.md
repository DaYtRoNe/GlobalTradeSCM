# GlobalTrade SCM — Technology Stack Guide

This document is a comprehensive guide to all technologies, frameworks, specifications, and libraries used in the GlobalTrade Supply Chain Management system. It explains **WHAT** each technology is, **WHY** enterprise systems use it, **HOW** GlobalTrade uses it, and **WHERE** it exists in the codebase.

---

## 1. Core Enterprise Technologies

### 1.1 Java 17 LTS (Standard Edition)
- **WHAT**: Java 17 is a Long-Term Support (LTS) release of the Java programming language.
- **WHY**: Provides modern language capabilities (Records, text blocks, pattern matching, enhanced switch), strong memory management, and high runtime performance.
- **HOW**: All project classes are compiled with Java 17 compiler target (`<source>17</source>`, `<target>17</target>`).
- **WHERE**: Configured in root `pom.xml` and all module `pom.xml` files.

---

### 1.2 Jakarta EE 10 (Enterprise Edition)
- **WHAT**: Jakarta EE 10 is an open-source, industry-standard specification for building modern, cloud-native enterprise Java applications.
- **WHY**: Standardizes core enterprise capabilities (EJB, JPA, JAX-RS, CDI, JTA, Security) so code is vendor-neutral and portable across certified enterprise servers.
- **HOW**: GlobalTrade uses Jakarta EE 10 APIs across all modules without depending on proprietary vendor code for business services.
- **WHERE**: Dependency `jakarta.platform:jakarta.jakartaee-api:10.0.0` with `<scope>provided</scope>` in `globaltrade-ejb/pom.xml` and `globaltrade-web/pom.xml`.

---

### 1.3 Payara Server 6 (Community Edition)
- **WHAT**: Payara Server 6 is a high-performance, Jakarta EE 10-certified enterprise application server based on GlassFish upstream.
- **WHY**: Provides the runtime container that manages thread pooling, database connection pools, transaction coordinators, EJB lifecycles, and security realms.
- **HOW**: Hosts the deployed `globaltrade.ear`, manages the DataSource `jdbc/GlobalTradeDS`, and executes the custom JAAS security realm.
- **WHERE**: Configured in `arquillian.xml` (HTTP port `8080`, Admin port `4848`) and server domain directory `C:/payara6/glassfish/domains/domain1/`.

---

### 1.4 EJB (Enterprise JavaBeans)
- **WHAT**: Server-managed Java components that automatically receive enterprise services (transactions, security, concurrency, pooling).
- **WHY**: Eliminates the need to write boilerplate code for database transaction boundaries, thread synchronization, and security checks.
- **HOW**:
  - Stateless Session Beans (`@Stateless`) implement business services (`ShipmentServiceBean`, `InventoryServiceBean`, `VendorServiceBean`, `AuditServiceBean`).
  - Container manages bean pooling and automatically injects them into web resources via `@EJB`.
- **WHERE**: `globaltrade-ejb/src/main/java/com/jiat/globaltrade/service/`.

---

### 1.5 JPA (Jakarta Persistence API) & EclipseLink
- **WHAT**: The standard Object-Relational Mapping (ORM) framework in Jakarta EE. EclipseLink is the default JPA reference implementation in Payara.
- **WHY**: Allows Java developers to manipulate database tables as standard Java classes (`Entities`) using an object-oriented query language (JPQL) instead of raw SQL strings.
- **HOW**:
  - Maps classes (`Vendor`, `Warehouse`, `InventoryItem`, `Shipment`, `CustomsDocument`, `AuditLog`) to MySQL tables.
  - Interacts with the database through `@PersistenceContext EntityManager em`.
- **WHERE**:
  - Configuration: `globaltrade-ejb/src/main/resources/META-INF/persistence.xml` (`GlobalTradePU`).
  - Entity classes: `globaltrade-ejb/src/main/java/com/jiat/globaltrade/entity/`.

---

### 1.6 JTA (Jakarta Transactions API)
- **WHAT**: The underlying Java transaction management API that coordinates ACID transactions across DataSources and enterprise components.
- **WHY**: Guarantees that multi-step operations (e.g. deduct stock + update shipment) execute atomically, rolling back if an error occurs.
- **HOW**:
  - **Container-Managed Transactions (CMT)**: Uses `@TransactionAttribute(REQUIRED)`, `MANDATORY`, `REQUIRES_NEW`, `SUPPORTS`.
  - **Bean-Managed Transactions (BMT)**: Injects `UserTransaction` in `InventoryReconciliationBean` for explicit programmatic demarcation.
- **WHERE**: Annotated on all methods in `globaltrade-ejb/src/main/java/com/jiat/globaltrade/service/`.

---

### 1.7 JAX-RS (Jakarta RESTful Web Services)
- **WHAT**: Standard specification for building RESTful HTTP web APIs in Java.
- **WHY**: Provides clean annotations (`@Path`, `@GET`, `@POST`, `@Produces`, `@Consumes`) to expose business logic as JSON endpoints over HTTP.
- **HOW**:
  - Root application configuration: `RestApplication.java` (`@ApplicationPath("/api")`).
  - Verification & operational resources: `SecurityVerificationResource.java`, `BusinessSecurityVerificationResource.java`, `TransactionVerificationResource.java`, `InterceptorVerificationResource.java`, `TimerVerificationResource.java`, `ExceptionVerificationResource.java`, `DatabaseHealthResource.java`.
- **WHERE**: `globaltrade-web/src/main/java/com/jiat/globaltrade/web/resource/`.

---

### 1.8 Jakarta Security & Declarative RBAC
- **WHAT**: Declarative security specification enabling role-based authorization via annotations and web descriptors.
- **WHY**: Decouples security policy from business logic, ensuring authorization is enforced by the container before methods execute.
- **HOW**:
  - Roles declared via `@DeclareRoles` and secured via `@RolesAllowed({ADMIN, LOGISTICS_COORDINATOR, ...})`.
  - Web paths protected via `<security-constraint>` in `globaltrade-web/src/main/webapp/WEB-INF/web.xml`.
- **WHERE**: `SecurityRoles.java`, EJB services, and `web.xml`.

---

### 1.9 JAAS (Java Authentication & Authorization Service) & Custom Realm
- **WHAT**: Pluggable security framework allowing custom authentication logic to integrate directly with the application server.
- **WHY**: Connects Payara's HTTP authentication directly to custom database schemas without hard-coding security logic in the application.
- **HOW**:
  - `GlobalTradeCustomRealm.java` extends Payara's `AppservRealm`.
  - `GlobalTradeLoginModule.java` extends `AppservPasswordLoginModule` to verify plain-text passwords against SHA-256 password hashes stored in `app_users` table and extract user groups from `user_roles`.
- **WHERE**:
  - Source: `globaltrade-security-provider/src/main/java/com/jiat/globaltrade/security/jaas/`.
  - Deployment: Installed in `C:/payara6/glassfish/domains/domain1/lib/globaltrade-security-provider.jar`.

---

### 1.10 MySQL Database & JDBC DataSource
- **WHAT**: MySQL is an open-source relational database management system. JDBC is the standard database connectivity driver.
- **WHY**: Stores all business state, transactions, users, and audit logs permanently with relational integrity and foreign keys.
- **HOW**:
  - Payara manages a connection pool bound to JNDI name `jdbc/GlobalTradeDS`.
  - Schema defined in `database/schema.sql` (10 tables, sample seed data, and SHA-256 demo password hashes).
- **WHERE**: `database/schema.sql` and `persistence.xml`.

---

### 1.11 Apache Maven & Multi-Module Architecture
- **WHAT**: Industry-standard build automation and dependency management tool.
- **WHY**: Manages compilation order, dependency versions via BOMs, test execution, and multi-module packaging.
- **HOW**:
  - Root `pom.xml` acts as reactor parent with `<packaging>pom</packaging>` and `<dependencyManagement>`.
  - Coordinates child modules: `globaltrade-security-provider`, `globaltrade-ejb`, `globaltrade-web`, `globaltrade-ear`.
- **WHERE**: Root `pom.xml` and sub-module `pom.xml` files.

---

### 1.12 Packaging Types: EAR, WAR, JAR
- **WHAT**:
  - **JAR (Java Archive)**: Library or EJB component module (`globaltrade-ejb.jar`, `globaltrade-security-provider.jar`).
  - **WAR (Web Archive)**: Web application module containing JAX-RS REST resources, HTML pages, and `web.xml` (`globaltrade-web.war`).
  - **EAR (Enterprise Archive)**: Top-level enterprise bundle containing both the EJB JAR and Web WAR (`globaltrade.ear`).
- **WHY**: Separates business logic (EJB) from web presentation (WAR) while packaging them together for single-click deployment to Payara.
- **WHERE**: `globaltrade-ear/pom.xml` and `globaltrade-ear/target/globaltrade.ear`.

---

### 1.13 JUnit 5 & Arquillian Integration Testing Framework
- **WHAT**:
  - **JUnit 5 (Jupiter)**: The testing engine for writing assertions and test cases.
  - **Arquillian**: An integration testing framework that deploys test packages directly into the live Payara server.
  - **ShrinkWrap**: A Java API used by Arquillian to dynamically assemble test `.war` archives in memory.
- **WHY**: Unit tests with mock frameworks (Mockito) cannot test real JPA queries, container transactions, interceptor chains, or JAAS realms. Arquillian tests them inside the real server.
- **HOW**:
  - Test suites (`ArquillianContainerSmokeIT`, `PersistenceIntegrationIT`, `TransactionRollbackIntegrationIT`, `BusinessValidationInterceptorIT`, `SecurityAuthenticationIT`) run via `mvn -Parquillian-payara verify`.
- **WHERE**: `globaltrade-ejb/src/test/java/com/jiat/globaltrade/test/`.

---

## 2. Essential Enterprise Comparisons (Viva Essentials)

| Comparison | Key Difference |
| :--- | :--- |
| **Java vs. Jakarta EE** | Java SE provides core language APIs (Collections, Math, I/O, Streams). Jakarta EE adds enterprise specifications (EJB, JPA, JAX-RS, JTA, Security) for multi-tier, distributed systems. |
| **Jakarta EE vs. Payara** | Jakarta EE is the **specification (standard rules/interfaces)**. Payara Server is the **implementation (engine/runtime)** that executes the specification. |
| **JPA vs. MySQL** | MySQL is the **relational database engine** storing tables on disk. JPA is the **Java framework** mapping Java Entity classes to MySQL tables. |
| **JPA vs. JDBC** | JDBC requires writing manual SQL strings (`PreparedStatement`, `ResultSet`). JPA automates SQL generation and maps rows directly to Java objects (`EntityManager.find(...)`). |
| **EJB vs. Normal Java Class (POJO)** | A POJO (`new MyClass()`) has no transaction management or security. An EJB (`@Stateless`) is managed by Payara, automatically receiving CMT transactions, `@RolesAllowed` security, thread safety, and lifecycle pooling. |
| **JUnit vs. Arquillian** | Standard JUnit tests run isolated in a local JVM with mock data. Arquillian boots or connects to the real Payara server and runs tests against real EJBs, real JPA, and real DataSources. |
| **WAR vs. EAR** | A **WAR** packages web-tier resources (REST, Servlets, HTML). An **EAR** packages the entire enterprise system (EJB JARs + Web WARs + application descriptors) into a single deployable unit. |

---

## 3. Technology Summary Table

| Technology | Role in GlobalTrade SCM | Key Module / File |
| :--- | :--- | :--- |
| **Java 17 LTS** | Base programming language | All modules (`pom.xml`) |
| **Jakarta EE 10** | Enterprise API platform | `globaltrade-ejb`, `globaltrade-web` |
| **Payara Server 6** | Application runtime server | Domain 1 runtime (`localhost:8080`, `4848`) |
| **EJB 3.2 / 4.0** | Business services, timers, interceptors | `globaltrade-ejb/.../service/` |
| **JPA 3.1 / EclipseLink**| Entity persistence & JPQL queries | `globaltrade-ejb/.../entity/`, `persistence.xml` |
| **JTA 2.0** | CMT & BMT transaction boundaries | `ShipmentServiceBean`, `InventoryServiceBean` |
| **JAX-RS 3.1** | REST API endpoints & Exception Mappers | `globaltrade-web/.../resource/`, `mapper/` |
| **Custom JAAS Realm** | Database password authentication | `globaltrade-security-provider/.../jaas/` |
| **MySQL 8.x** | Persistent relational storage | `database/schema.sql` (`jdbc/GlobalTradeDS`) |
| **Apache Maven** | Multi-module compilation & build | `pom.xml` (root and submodules) |
| **EAR Bundle** | Enterprise distribution archive | `globaltrade-ear/target/globaltrade.ear` |
| **JUnit 5 + Arquillian** | In-container live regression testing | `globaltrade-ejb/src/test/java/.../*IT.java` |
| **ShrinkWrap** | Dynamic micro-deployment generation | `TestDeployments.java` |
