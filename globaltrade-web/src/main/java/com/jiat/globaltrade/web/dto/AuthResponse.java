package com.jiat.globaltrade.web.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Data Transfer Object for UI authentication responses.
 */
public class AuthResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status = "SUCCESS";
    private boolean authenticated;
    private String principal;
    private String authMechanism = "BASIC";
    private Map<String, Boolean> roles;

    public AuthResponse() {
    }

    public AuthResponse(boolean authenticated, String principal, Map<String, Boolean> roles) {
        this.authenticated = authenticated;
        this.principal = principal;
        this.roles = roles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getAuthMechanism() {
        return authMechanism;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    public Map<String, Boolean> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, Boolean> roles) {
        this.roles = roles;
    }
}
