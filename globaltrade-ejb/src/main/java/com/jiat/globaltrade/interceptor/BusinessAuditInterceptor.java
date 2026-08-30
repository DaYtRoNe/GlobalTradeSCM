package com.jiat.globaltrade.interceptor;

import com.jiat.globaltrade.service.AuditServiceBean;
import jakarta.ejb.EJB;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cross-cutting generic business audit interceptor.
 * Intercepts business method invocations and records SUCCESS or FAILURE audit entries
 * into the database via AuditServiceBean (REQUIRES_NEW), without altering business logic.
 */
public class BusinessAuditInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(BusinessAuditInterceptor.class.getName());

    @EJB
    private AuditServiceBean auditService;

    @AroundInvoke
    public Object auditInvocation(InvocationContext context) throws Exception {
        String targetClass = context.getTarget() != null ? context.getTarget().getClass().getSimpleName() : "UnknownClass";
        String methodName = context.getMethod() != null ? context.getMethod().getName() : "unknownMethod";
        String operationName = targetClass + "." + methodName;
        Object[] params = context.getParameters();

        // Extract safe summary of parameters (no passwords or sensitive data)
        String paramSummary = summarizeParameters(params);

        LOGGER.log(Level.INFO, "[BusinessAuditInterceptor] [BEFORE] Intercepting business operation: {0} ({1})",
                new Object[]{operationName, paramSummary});

        try {
            Object result = context.proceed();

            LOGGER.log(Level.INFO, "[BusinessAuditInterceptor] [AFTER_SUCCESS] Business operation {0} succeeded.", operationName);

            if (auditService != null) {
                auditService.logAction(
                        "INTERCEPTOR_BUSINESS_SUCCESS",
                        targetClass,
                        extractTargetId(params),
                        "INTERCEPTOR",
                        String.format("Method %s executed successfully. Params: [%s]", methodName, paramSummary)
                );
            }

            return result;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[BusinessAuditInterceptor] [AFTER_FAILURE] Business operation {0} failed: {1}",
                    new Object[]{operationName, e.getMessage()});

            if (auditService != null) {
                auditService.logAction(
                        "INTERCEPTOR_BUSINESS_FAILURE",
                        targetClass,
                        extractTargetId(params),
                        "INTERCEPTOR",
                        String.format("Method %s failed with %s: %s", methodName, e.getClass().getSimpleName(), e.getMessage())
                );
            }

            // Rethrow original exception unchanged to preserve CMT rollback and caller handling
            throw e;
        }
    }

    private Long extractTargetId(Object[] params) {
        if (params != null && params.length > 0 && params[0] instanceof Long id) {
            return id;
        }
        return null;
    }

    private String summarizeParameters(Object[] params) {
        if (params == null || params.length == 0) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append(", ");
            Object p = params[i];
            if (p == null) {
                sb.append("null");
            } else if (p instanceof Long || p instanceof Integer || p instanceof String || p instanceof Enum) {
                sb.append(p);
            } else {
                sb.append(p.getClass().getSimpleName());
            }
        }
        return sb.toString();
    }
}
