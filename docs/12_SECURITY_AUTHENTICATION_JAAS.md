# GlobalTrade SCM — Security Authentication & Custom JAAS Guide

This document explains the security architecture of GlobalTrade SCM, focusing on HTTP Basic Authentication, the Java Authentication and Authorization Service (JAAS), and our custom Payara Security Realm and Login Module.

---

## 1. Security Foundations (Beginner Concepts)

### 1.1 Authentication vs. Authorization
Students often confuse authentication and authorization. In enterprise security, they have distinct meanings:

| Concept | Question Answered | Everyday Analogy | Role in GlobalTrade SCM |
| :--- | :--- | :--- | :--- |
| **Authentication (AuthN)** | *"Who are you?"* | Showing your passport at airport customs. | Verifying username (`gt_admin`) and password hash against MySQL `app_users`. |
| **Authorization (AuthZ)** | *"What are you allowed to do?"* | Boarding pass checking if you can enter the cockpit or passenger cabin. | Checking if the authenticated user has the `ADMIN` role to update vendor ratings or `CUSTOMS_AGENT` to clear shipments. |

---

## 2. HTTP Basic Authentication Mechanism

GlobalTrade SCM configures HTTP Basic Authentication in `globaltrade-web/src/main/webapp/WEB-INF/web.xml`:

```xml
<login-config>
    <auth-method>BASIC</auth-method>
    <realm-name>GlobalTradeCustomRealm</realm-name>
</login-config>
```

### 2.1 How HTTP Basic Works
1. Client sends an HTTP request (e.g. `GET /api/security/whoami`).
2. If no `Authorization` header is present, Payara returns `HTTP 401 Unauthorized` with header `WWW-Authenticate: Basic realm="GlobalTradeCustomRealm"`.
3. Client provides credentials by encoding `username:password` in Base64:
   ```text
   Authorization: Basic Z3RfYWRtaW46UGFzc3dvcmRAMTIz
   ```
4. Payara intercepts the request, decodes the username/password, and passes them to the custom JAAS security realm for verification.

> [!IMPORTANT]
> **Educational & Local Demo Notice**:
> In this academic project, HTTP Basic Authentication is used over standard HTTP for local demonstration and automated Arquillian testing. Because Base64 encoding is easily reversible, **production enterprise deployments strictly require HTTPS (TLS encryption)** to prevent credentials from being intercepted over the network.

---

## 3. Custom JAAS Architecture (Realm & LoginModule)

Instead of hard-coding authentication inside web filters or REST endpoints, GlobalTrade SCM integrates with Payara's container security pipeline using standard **JAAS (Java Authentication and Authorization Service)**.

```mermaid
graph TD
    subgraph Client["Client Tier"]
        Postman["HTTP Client / Postman / Test Probe"]
    end

    subgraph PayaraContainer["Payara Server 6 Security Subsystem"]
        PayaraAuth["Payara Web Container / HTTP BASIC Authentication"]
        Realm["GlobalTradeCustomRealm<br/>(extends AppservRealm)"]
        LoginModule["GlobalTradeLoginModule<br/>(extends AppservPasswordLoginModule)"]
    end

    subgraph ServerLib["Payara Domain lib/"]
        SecJar["globaltrade-security-provider.jar"]
    end

    subgraph Database["MySQL Data Tier"]
        DS["JNDI: jdbc/GlobalTradeDS"]
        Users["app_users (username, password_hash, active)"]
        Roles["user_roles (username, role_name)"]
    end

    Postman -->|HTTP Authorization: Basic| PayaraAuth
    PayaraAuth --> Realm
    Realm --> LoginModule
    SecJar -.->|Provides Class Definitions| Realm
    SecJar -.->|Provides Class Definitions| LoginModule
    LoginModule -->|JNDI Lookup| DS
    DS --> Users
    DS --> Roles
    LoginModule -->|commitUserAuthentication(roles)| PayaraAuth
```

### 3.1 Why `globaltrade-security-provider.jar` is in Payara's `domain/lib`
In Jakarta EE application servers, security realms are loaded by the **Server-Level ClassLoader** at server startup time—*before* any individual `.ear` or `.war` application is deployed.

If the security provider were bundled inside `globaltrade.ear`, Payara's server-level realm manager would not be able to find the `GlobalTradeCustomRealm` class during boot, causing authentication initialization to fail. Installing the JAR in `C:/payara6/glassfish/domains/domain1/lib/` ensures it is globally available to the server runtime.

---

## 4. Step-by-Step Authentication Sequence

The sequence diagram below traces what happens inside the server during authentication:

```mermaid
sequenceDiagram
    autonumber
    actor Client as HTTP Client (Postman / Test)
    participant Payara as Payara Server (HTTP Port 8080)
    participant Realm as GlobalTradeCustomRealm
    participant LM as GlobalTradeLoginModule
    participant JNDI as JNDI InitialContext
    participant DB as MySQL (app_users & user_roles)

    Client->>Payara: GET /api/security/whoami (Header: Authorization: Basic gt_admin:Password@123)
    Payara->>Realm: Delegate authentication to realm
    Realm->>LM: Initialize LoginModule under JAAS context "GlobalTradeCustomJaas"
    
    LM->>LM: Validate username & password non-empty
    LM->>JNDI: ctx.lookup("jdbc/GlobalTradeDS")
    JNDI-->>LM: DataSource reference
    
    LM->>DB: SELECT password_hash, active FROM app_users WHERE username = 'gt_admin'
    DB-->>LM: Hash: 5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8, Active: true
    
    LM->>LM: Check if active == true (Passed)
    LM->>LM: Compute SHA-256(password) == stored hash (Passed)
    
    LM->>DB: SELECT role_name FROM user_roles WHERE username = 'gt_admin'
    DB-->>LM: Roles: ["ADMIN"]
    
    LM->>Payara: commitUserAuthentication(groups = ["ADMIN"])
    Note over Payara: Principal "gt_admin" and role "ADMIN" attached to SecurityContext
    
    Payara->>Client: HTTP 200 OK (User Identity JSON)
```

---

## 5. Implementation Details in Code

### 5.1 `GlobalTradeCustomRealm.java`
Located in `globaltrade-security-provider/src/main/java/com/jiat/globaltrade/security/jaas/GlobalTradeCustomRealm.java`:
- Extends `com.sun.appserv.security.AppservRealm`.
- Declares the default JAAS context:
  ```java
  public static final String DEFAULT_JAAS_CONTEXT = "GlobalTradeCustomJaas";
  ```
- Reads realm configuration properties (JNDI DataSource name `jdbc/GlobalTradeDS`, table names `app_users` and `user_roles`, column names).
- Implements `getGroupNames(username)` to query assigned roles using `PreparedStatement`.

### 5.2 `GlobalTradeLoginModule.java`
Located in `globaltrade-security-provider/src/main/java/com/jiat/globaltrade/security/jaas/GlobalTradeLoginModule.java`:
- Extends `com.sun.appserv.security.AppservPasswordLoginModule`.
- Implements `authenticateUser()`:
  1. Retrieves raw credentials from `_username` and `_password`.
  2. Queries `app_users` for `password_hash` and `active` status.
  3. **Enforces active user check**: If `active == false`, immediately throws `LoginException("Invalid credentials.")`.
  4. Computes SHA-256 lowercase hex digest:
     ```java
     MessageDigest md = MessageDigest.getInstance("SHA-256");
     byte[] digest = md.digest(_password.getBytes(StandardCharsets.UTF_8));
     // Formats byte array to lowercase hex string
     ```
  5. Compares computed hash with stored hash.
  6. Queries `user_roles` for assigned roles.
  7. Commits roles to container:
     ```java
     commitUserAuthentication(userGroups.toArray(new String[0]));
     ```

---

## 6. Password Hashing: Prototype vs. Production Best Practices

| Aspect | Academic Prototype (Current) | Production Recommendation |
| :--- | :--- | :--- |
| **Algorithm** | SHA-256 Digest | **Argon2id, bcrypt, or PBKDF2** |
| **Salting** | Unsalted (Demo simplicity) | **Cryptographically secure per-user unique salt** |
| **Transport** | HTTP / Localhost | **HTTPS / TLS 1.3** |
| **Brute-force defense** | Inactive user flag | **Rate limiting, account lockout, and WAF** |
| **Multi-Factor** | Single factor (Password) | **MFA (TOTP / WebAuthn / SSO IdP via OAuth2/OIDC)** |

> [!NOTE]
> For academic coursework and demonstrations, SHA-256 hex hashes are used so demo accounts can be easily seeded in `database/schema.sql`. In a real production deployment, salted Argon2id or bcrypt hashing with TLS transport is mandatory.

---

## 7. Common Viva Questions & Model Answers

### Q1: What is the difference between authentication and authorization?
> **Answer**: Authentication verifies *who* the caller is (e.g. verifying `gt_admin`'s password hash in `app_users`). Authorization determines *what* that authenticated caller is permitted to do (e.g. checking if `gt_admin` has the `ADMIN` role before allowing a vendor rating update).

### Q2: Why did we build a custom JAAS Realm and LoginModule?
> **Answer**: Standard application servers only provide default file-based or generic JDBC realms that do not support custom schemas (like our `active` user status flag) or custom password hashing logic. Our custom `GlobalTradeLoginModule` seamlessly bridges Payara's HTTP Basic authentication directly to our MySQL database schema.

### Q3: Why is `globaltrade-security-provider.jar` deployed to Payara's `domain/lib` rather than inside the EAR?
> **Answer**: In Jakarta EE servers, security realms are loaded by the server-level classloader at startup time before any application is deployed. Placing the provider in `domain/lib` ensures the realm class is globally accessible to the application server runtime.
