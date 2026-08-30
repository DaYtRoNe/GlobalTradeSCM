package com.jiat.globaltrade.security.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Serializable diagnostic DTO representing caller security context and evaluated role memberships.
 */
public class CallerSecuritySummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String principalName;
    private final boolean authenticated;
    private final Map<String, Boolean> evaluatedRoles;
    private final String authMechanism;

    public CallerSecuritySummary(String principalName, boolean authenticated, Map<String, Boolean> evaluatedRoles, String authMechanism) {
        this.principalName = principalName;
        this.authenticated = authenticated;
        this.evaluatedRoles = evaluatedRoles;
        this.authMechanism = authMechanism;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public Map<String, Boolean> getEvaluatedRoles() {
        return evaluatedRoles;
    }

    public String getAuthMechanism() {
        return authMechanism;
    }

    @Override
    public String toString() {
        return "CallerSecuritySummary{" +
                "principalName='" + principalName + '\'' +
                ", authenticated=" + authenticated +
                ", evaluatedRoles=" + evaluatedRoles +
                ", authMechanism='" + authMechanism + '\'' +
                '}';
    }
}
