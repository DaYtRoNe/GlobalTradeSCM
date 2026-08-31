package com.jiat.globaltrade.web.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Standardized API Error Response DTO for REST endpoints.
 * Provides safe, structured, and informative error payloads without leaking
 * system internals, database topology, passwords, or Java stack traces.
 */
public class ApiErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private String status;
    private String errorCode;
    private String message;
    private String timestamp;
    private String path;

    public ApiErrorResponse() {
        this.timestamp = LocalDateTime.now().format(ISO_FORMATTER);
    }

    public ApiErrorResponse(String status, String errorCode, String message, String path) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now().format(ISO_FORMATTER);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
