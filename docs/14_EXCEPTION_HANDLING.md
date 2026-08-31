# GlobalTrade SCM — Exception Handling Guide

This document explains the enterprise exception handling architecture in GlobalTrade SCM, including exception classification, rollback semantics, centralized JAX-RS Exception Mappers, and secure payload formatting.

---

## 1. Exception Handling Foundations (Beginner Concepts)

### 1.1 What is an Exception?
An **exception** is an abnormal condition or unexpected event that disrupts the normal execution flow of a program.

In enterprise Jakarta EE applications, exceptions fall into two broad categories:

| Exception Category | What It Means | Examples in GlobalTrade SCM | How the System Responds |
| :--- | :--- | :--- | :--- |
| **Application / Business Exceptions** | Expected business rule violations, stock shortages, or access rejections. | `InsufficientInventoryException`, `VendorAccessDeniedException`, `ResourceNotFoundException` | Handled gracefully, mapped to clear HTTP status codes (`400`, `403`, `404`, `409`), and logs business event. |
| **System / Runtime Exceptions** | Unexpected technical failures (database connection drop, out-of-memory, null pointer). | `SQLException`, `NullPointerException`, `EJBException` | Transaction rolled back immediately, technical stack trace logged to server logs, and generic **HTTP 500** returned to client. |

---

## 2. Standardized Error Payload (`ApiErrorResponse`)

To ensure clients receive consistent, structured JSON responses, the project defines `globaltrade-web/src/main/java/com/jiat/globaltrade/web/dto/ApiErrorResponse.java`:

```json
{
  "status": "CONFLICT",
  "errorCode": "INSUFFICIENT_INVENTORY",
  "message": "Insufficient inventory for item ID 1. Requested: 99999, Available: 100",
  "timestamp": "2026-08-31T12:00:00.123456",
  "path": "/api/business-security/shipment/1/dispatch"
}
```

### Critical Security Rule: Zero Technical Leakage
In enterprise applications, API error payloads must **never expose**:
- Java stack traces.
- Raw SQL queries or database table names.
- File system paths or server configuration files.
- Passwords, hashes, or security tokens.

All technical stack traces are logged strictly to Payara's internal server log (`server.log`), while the client receives only sanitized error messages.

---

## 3. GlobalTrade Exception Taxonomy & HTTP Mappings

The table below details all domain exceptions, their rollback configurations, and their corresponding HTTP status codes:

| Java Exception Class | Jakarta EE Annotation | CMT Rollback? | HTTP Status Code | JAX-RS Exception Mapper |
| :--- | :--- | :---: | :---: | :--- |
| **`IllegalArgumentException`** | Standard Unchecked Exception | Yes | **`400 Bad Request`** | `IllegalArgumentExceptionMapper.java` |
| **`BusinessRuleViolationException`** | `@ApplicationException(rollback = true)` | **Yes** | **`400 Bad Request`** | `BusinessRuleViolationExceptionMapper.java` |
| **`EJBAccessException` / `SecurityException`** | Standard Container Security Exception | Yes | **`403 Forbidden`** | `EJBAccessExceptionMapper.java` |
| **`VendorAccessDeniedException`** | `@ApplicationException(rollback = false)` | **No** | **`403 Forbidden`** | `VendorAccessDeniedExceptionMapper.java` |
| **`ResourceNotFoundException`** | `@ApplicationException(rollback = false)` | **No** | **`404 Not Found`** | `ResourceNotFoundExceptionMapper.java` |
| **`InsufficientInventoryException`** | `@ApplicationException(rollback = true)` | **Yes** | **`409 Conflict`** | `InsufficientInventoryExceptionMapper.java` |
| **`WebApplicationException`** *(e.g. unknown URL)* | JAX-RS Web Framework Exception | N/A | **`404 / 405 / etc.`** | `WebApplicationExceptionMapper.java` |
| **`Throwable` / `RuntimeException`** | Unhandled System Exceptions | Yes | **`500 Internal Error`** | `GenericExceptionMapper.java` |

---

## 4. Centralized `ExceptionMapper` Architecture

Instead of writing repetitive `try-catch` blocks inside every REST controller, Jakarta EE uses the **Provider Pattern** (`@Provider ExceptionMapper<T>`):

```mermaid
sequenceDiagram
    autonumber
    actor Client as HTTP Client
    participant Resource as JAX-RS REST Resource
    participant EJB as EJB Business Service
    participant Mapper as Centralized ExceptionMapper
    participant Log as Server Log (server.log)

    Client->>Resource: POST /api/business-security/shipment/1/dispatch (Qty: 99999)
    Resource->>EJB: processShipmentDispatch(...)
    Note over EJB: Shortage detected! Throws InsufficientInventoryException
    EJB-->>Resource: Exception propagates out of EJB layer
    Note over Resource: Uncaught in resource method; intercepted by JAX-RS runtime
    Resource->>Mapper: toResponse(InsufficientInventoryException)
    Mapper->>Log: Log business warning
    Mapper->>Client: HTTP 409 Conflict (Structured ApiErrorResponse JSON)
```

### 4.1 Exception Unwrapping in `GenericExceptionMapper`
When an EJB throws an application exception, the application server sometimes wraps it in a container exception (e.g. `jakarta.ejb.EJBException`). 

`GenericExceptionMapper.java` traverses the `getCause()` hierarchy to extract the original business exception (such as `InsufficientInventoryException` or `ResourceNotFoundException`) before mapping it to its correct HTTP status code.

### 4.2 Preserving Standard 404 Routes (`WebApplicationExceptionMapper`)
When a client requests a non-existent URL (e.g. `GET /api/nonexistent`), JAX-RS throws `NotFoundException` (a subclass of `WebApplicationException`). `WebApplicationExceptionMapper.java` intercepts this and returns a clean `HTTP 404 Not Found` JSON payload rather than allowing it to be caught as a generic `HTTP 500` server error.

---

## 5. Diagnostic Verification Exception Endpoints

`ExceptionVerificationResource.java` (`/api/exceptions/*`) provides dedicated endpoints to test and verify every exception mapping:

| Endpoint URL | Method | Triggered Exception | Expected HTTP Status |
| :--- | :---: | :--- | :---: |
| `/api/exceptions/validation` | `POST` | `IllegalArgumentException` | `400 Bad Request` |
| `/api/exceptions/business-rule` | `POST` | `BusinessRuleViolationException` | `400 Bad Request` |
| `/api/exceptions/not-found` | `GET` | `ResourceNotFoundException` | `404 Not Found` |
| `/api/exceptions/inventory-conflict` | `POST` | `InsufficientInventoryException` | `409 Conflict` |
| `/api/exceptions/system-error` | `GET` | `RuntimeException` (Simulated failure) | `500 Internal Error` |

---

## 6. Common Viva Questions & Model Answers

### Q1: What is the purpose of JAX-RS `ExceptionMapper`?
> **Answer**: `ExceptionMapper` decouples error formatting from business logic. Instead of writing try-catch blocks in every controller, uncaught exceptions are intercepted centrally, translated into standard HTTP status codes (`400`, `403`, `404`, `409`, `500`), and formatted into uniform JSON error payloads.

### Q2: Why does `InsufficientInventoryException` have `rollback = true` while `ResourceNotFoundException` has `rollback = false`?
> **Answer**: `InsufficientInventoryException` represents a business failure during a write operation (e.g. shipment dispatch) that invalidates the transaction, requiring all prior database modifications to be undone. `ResourceNotFoundException` is an informational query result during a read operation where no transaction rollback is necessary.

### Q3: Why do we never return raw stack traces in REST API error responses?
> **Answer**: Returning stack traces leaks internal implementation details (class names, database schemas, library versions, file paths) that malicious actors can exploit to find vulnerabilities. In enterprise systems, stack traces are written to secure server logs, and clients receive only sanitized error codes and messages.
