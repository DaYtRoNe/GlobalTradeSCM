package com.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

/**
 * Application exception thrown when a domain business rule, state precondition,
 * or operational constraint is violated during service execution.
 *
 * Configured with @ApplicationException(rollback = true) to instruct the EJB container
 * to automatically mark the enclosing container-managed transaction for rollback.
 */
@ApplicationException(rollback = true)
public class BusinessRuleViolationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String ruleName;
    private final Object resourceId;

    public BusinessRuleViolationException(String ruleName, Object resourceId, String message) {
        super(message);
        this.ruleName = ruleName;
        this.resourceId = resourceId;
    }

    public BusinessRuleViolationException(String message) {
        super(message);
        this.ruleName = "BUSINESS_RULE";
        this.resourceId = null;
    }

    public String getRuleName() {
        return ruleName;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
