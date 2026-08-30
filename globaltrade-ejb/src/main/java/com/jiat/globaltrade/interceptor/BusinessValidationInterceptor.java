package com.jiat.globaltrade.interceptor;

import com.jiat.globaltrade.entity.CustomsDocument;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cross-cutting business input validation interceptor.
 * Intercepts invocations before business execution and verifies parameter constraints.
 * If validation fails, throws IllegalArgumentException before context.proceed() is reached.
 */
public class BusinessValidationInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(BusinessValidationInterceptor.class.getName());

    @AroundInvoke
    public Object validateArguments(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        String methodName = method != null ? method.getName() : "unknown";
        Object[] params = context.getParameters();

        LOGGER.log(Level.INFO, "[BusinessValidationInterceptor] [VALIDATION_CHECK] Validating inputs for: {0}", methodName);

        if (methodName.startsWith("find") || methodName.startsWith("get")) {
            validateLookupParameters(methodName, params);
        } else if ("updatePerformanceRating".equals(methodName)) {
            validateVendorRatingParameters(params);
        } else if ("updateVendorStatus".equals(methodName)) {
            validateVendorStatusParameters(params);
        } else if ("increaseStock".equals(methodName) || "decreaseStock".equals(methodName)) {
            validateStockAdjustmentParameters(methodName, params);
        } else if ("processShipmentDispatch".equals(methodName)) {
            validateDispatchParameters(params);
        } else if ("createCustomsDocument".equals(methodName)) {
            validateCustomsDocumentCreation(params);
        } else if ("updateDocumentStatus".equals(methodName)) {
            validateCustomsStatusUpdate(params);
        }

        LOGGER.log(Level.INFO, "[BusinessValidationInterceptor] [VALIDATION_PASSED] Inputs valid for: {0}", methodName);
        return context.proceed();
    }

    private void validateLookupParameters(String methodName, Object[] params) {
        if (params != null && params.length > 0 && params[0] instanceof Long id) {
            if (id <= 0) {
                throw new IllegalArgumentException(String.format("Validation failed for %s: Target entity ID (%d) must be a positive number.", methodName, id));
            }
        }
    }

    private void validateVendorRatingParameters(Object[] params) {
        if (params == null || params.length < 2) {
            throw new IllegalArgumentException("Validation failed: Insufficient arguments for vendor rating update.");
        }
        if (!(params[0] instanceof Long vendorId) || vendorId <= 0) {
            throw new IllegalArgumentException("Validation failed: Vendor ID must be a positive number.");
        }
        if (!(params[1] instanceof BigDecimal rating)) {
            throw new IllegalArgumentException("Validation failed: Rating must not be null.");
        }
        if (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(BigDecimal.valueOf(5.0)) > 0) {
            throw new IllegalArgumentException(String.format("Validation failed: Performance rating (%s) must be between 0.00 and 5.00.", rating));
        }
    }

    private void validateVendorStatusParameters(Object[] params) {
        if (params == null || params.length < 2) {
            throw new IllegalArgumentException("Validation failed: Insufficient arguments for vendor status update.");
        }
        if (!(params[0] instanceof Long vendorId) || vendorId <= 0) {
            throw new IllegalArgumentException("Validation failed: Vendor ID must be a positive number.");
        }
        if (params[1] == null) {
            throw new IllegalArgumentException("Validation failed: Vendor status must not be null.");
        }
    }

    private void validateStockAdjustmentParameters(String methodName, Object[] params) {
        if (params == null || params.length < 2) {
            throw new IllegalArgumentException("Validation failed: Insufficient arguments for stock adjustment.");
        }
        if (!(params[0] instanceof Long itemId) || itemId <= 0) {
            throw new IllegalArgumentException("Validation failed: Item ID must be a positive number.");
        }
        if (!(params[1] instanceof Integer qty) || qty <= 0) {
            throw new IllegalArgumentException(String.format("Validation failed for %s: Quantity must be greater than zero.", methodName));
        }
    }

    private void validateDispatchParameters(Object[] params) {
        if (params == null || params.length < 3) {
            throw new IllegalArgumentException("Validation failed: Insufficient arguments for shipment dispatch.");
        }
        if (!(params[0] instanceof Long shipmentId) || shipmentId <= 0) {
            throw new IllegalArgumentException("Validation failed: Shipment ID must be a positive number.");
        }
        if (!(params[1] instanceof Long itemId) || itemId <= 0) {
            throw new IllegalArgumentException("Validation failed: Inventory item ID must be a positive number.");
        }
        if (!(params[2] instanceof Integer qty) || qty <= 0) {
            throw new IllegalArgumentException("Validation failed: Dispatch quantity must be greater than zero.");
        }
    }

    private void validateCustomsDocumentCreation(Object[] params) {
        if (params == null || params.length < 2) {
            throw new IllegalArgumentException("Validation failed: Insufficient arguments for customs document creation.");
        }
        if (!(params[0] instanceof CustomsDocument doc)) {
            throw new IllegalArgumentException("Validation failed: Customs document object must not be null.");
        }
        if (doc.getDocumentNumber() == null || doc.getDocumentNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Validation failed: Customs document number is required and cannot be empty.");
        }
        if (doc.getDocumentType() == null) {
            throw new IllegalArgumentException("Validation failed: Customs document type must be specified.");
        }
        if (!(params[1] instanceof Long shipmentId) || shipmentId <= 0) {
            throw new IllegalArgumentException("Validation failed: Associated shipment ID must be a positive number.");
        }
    }

    private void validateCustomsStatusUpdate(Object[] params) {
        if (params == null || params.length < 2) {
            throw new IllegalArgumentException("Validation failed: Insufficient arguments for customs status update.");
        }
        if (!(params[0] instanceof Long docId) || docId <= 0) {
            throw new IllegalArgumentException("Validation failed: Customs document ID must be a positive number.");
        }
        if (params[1] == null) {
            throw new IllegalArgumentException("Validation failed: New customs status must not be null.");
        }
    }
}
