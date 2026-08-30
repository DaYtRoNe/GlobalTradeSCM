package com.jiat.globaltrade.interceptor;

import com.jiat.globaltrade.entity.CustomsDocument;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cross-cutting regulatory and trade compliance interceptor.
 * Enforces statutory export/import trade compliance rules on international customs documents
 * and cross-border consignment dispatches.
 */
public class TradeComplianceInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(TradeComplianceInterceptor.class.getName());

    @AroundInvoke
    public Object checkCompliance(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        String methodName = method != null ? method.getName() : "unknown";
        Object[] params = context.getParameters();

        LOGGER.log(Level.INFO, "[TradeComplianceInterceptor] [COMPLIANCE_START] Checking trade compliance for: {0}", methodName);

        if ("createCustomsDocument".equals(methodName)) {
            verifyCustomsDocumentCompliance(params);
        } else if ("updateDocumentStatus".equals(methodName)) {
            verifyStatusTransitionCompliance(params);
        } else if ("processShipmentDispatch".equals(methodName)) {
            verifyDispatchCompliance(params);
        }

        LOGGER.log(Level.INFO, "[TradeComplianceInterceptor] [COMPLIANCE_PASSED] Compliance cleared for: {0}", methodName);
        return context.proceed();
    }

    private void verifyCustomsDocumentCompliance(Object[] params) {
        if (params != null && params.length > 0 && params[0] instanceof CustomsDocument doc) {
            String docNum = doc.getDocumentNumber();
            if (docNum == null || docNum.trim().length() < 4) {
                LOGGER.log(Level.WARNING, "[TradeComplianceInterceptor] [COMPLIANCE_FAILED] Document number: {0} is too short for statutory customs filing.",
                        new Object[]{docNum});
                throw new IllegalArgumentException("Trade Compliance Violation: Customs document reference number must be at least 4 alphanumeric characters.");
            }

            // Compliance check: Regulatory prefix or alphanumeric convention
            if (!docNum.matches("^[A-Za-z0-9_\\-]+$")) {
                LOGGER.log(Level.WARNING, "[TradeComplianceInterceptor] [COMPLIANCE_FAILED] Document number: {0} contains illegal regulatory characters.",
                        new Object[]{docNum});
                throw new IllegalArgumentException("Trade Compliance Violation: Customs document identifier contains invalid regulatory characters.");
            }
        }
    }

    private void verifyStatusTransitionCompliance(Object[] params) {
        if (params != null && params.length > 0 && params[0] instanceof Long docId) {
            if (docId == null || docId <= 0) {
                throw new IllegalArgumentException("Trade Compliance Violation: Invalid regulatory document ID.");
            }
        }
    }

    private void verifyDispatchCompliance(Object[] params) {
        if (params != null && params.length >= 4) {
            String operator = params[3] != null ? params[3].toString() : null;
            if (operator == null || operator.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "[TradeComplianceInterceptor] [COMPLIANCE_FAILED] Dispatch attempt lacks authenticated operator credentials.");
                throw new IllegalArgumentException("Trade Compliance Violation: Consignment dispatch requires certified operator identity.");
            }
        }
    }
}
