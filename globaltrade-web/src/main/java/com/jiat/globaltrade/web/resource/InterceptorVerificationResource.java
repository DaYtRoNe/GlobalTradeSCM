package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import com.jiat.globaltrade.entity.enums.CustomsDocumentType;
import com.jiat.globaltrade.interceptor.InterceptorMetricsBean;
import com.jiat.globaltrade.service.CustomsServiceBean;
import com.jiat.globaltrade.service.VendorServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Verification resource for testing Phase 4 EJB Interceptors & Cross-Cutting Concerns:
 * - Performance Monitoring
 * - Business Audit Interception
 * - Input Validation Interception
 * - Trade Compliance Interception
 * - Interceptor Chaining
 *
 * Configured with @TransactionAttribute(NOT_SUPPORTED) so the verification resource itself
 * does not initiate an outer transaction context, allowing business EJBs to manage their own
 * CMT transactions independently and preventing validation/compliance exceptions from aborting
 * the REST layer.
 */
@Stateless
@Path("/interceptors")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InterceptorVerificationResource {

    @EJB
    private InterceptorMetricsBean metricsBean;

    @EJB
    private VendorServiceBean vendorService;

    @EJB
    private CustomsServiceBean customsService;

    /**
     * Retrieves aggregated performance monitoring metrics recorded by PerformanceMonitoringInterceptor.
     * GET /api/interceptors/metrics
     */
    @GET
    @Path("/metrics")
    public Response getPerformanceMetrics() {
        JsonObject response = Json.createObjectBuilder()
                .add("totalInvocations", metricsBean.getTotalInvocations())
                .add("averageExecutionMicros", metricsBean.getAverageExecutionMicros())
                .add("maxExecutionMicros", metricsBean.getMaxExecutionMicros())
                .add("lastExecutionMicros", metricsBean.getLastExecutionMicros())
                .add("lastMethod", metricsBean.getLastMethodName())
                .build();

        return Response.ok(response).build();
    }

    /**
     * Tests valid business invocation through class-level interceptor chain (Validation -> Performance -> Audit).
     * POST /api/interceptors/vendor-valid
     */
    @POST
    @Path("/vendor-valid")
    public Response testValidVendorUpdate() {
        try {
            Vendor updatedVendor = vendorService.updatePerformanceRating(1L, BigDecimal.valueOf(4.75), "AUDIT_OFFICER");

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "SUCCESS")
                    .add("message", "Vendor performance rating updated through interceptor chain.")
                    .add("vendorId", updatedVendor.getId())
                    .add("companyName", updatedVendor.getCompanyName())
                    .add("newPerformanceRating", updatedVendor.getPerformanceRating().toString())
                    .add("interceptorsApplied", "BusinessValidation -> PerformanceMonitoring -> BusinessAudit")
                    .build();

            return Response.ok(response).build();
        } catch (Exception e) {
            JsonObject response = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", e.getMessage())
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    /**
     * Tests BusinessValidationInterceptor rejecting invalid input (rating > 5.00) before business logic runs.
     * POST /api/interceptors/vendor-invalid
     */
    @POST
    @Path("/vendor-invalid")
    public Response testInvalidVendorValidation() {
        try {
            // Rating 9.99 exceeds allowed 0.00 - 5.00 range; BusinessValidationInterceptor must intercept and reject
            vendorService.updatePerformanceRating(1L, BigDecimal.valueOf(9.99), "AUDIT_OFFICER");

            return Response.ok(Json.createObjectBuilder()
                    .add("status", "UNEXPECTED_SUCCESS")
                    .build()).build();
        } catch (Exception e) {
            IllegalArgumentException iae = findIllegalArgumentException(e);
            if (iae != null) {
                JsonObject response = Json.createObjectBuilder()
                        .add("validationRejected", true)
                        .add("interceptor", "BusinessValidationInterceptor")
                        .add("caughtException", iae.getClass().getSimpleName())
                        .add("validationMessage", iae.getMessage())
                        .add("businessMethodExecuted", false)
                        .build();

                return Response.ok(response).build();
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "UNEXPECTED_ERROR")
                    .add("message", e.getMessage() != null ? e.getMessage() : "Unknown error")
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    /**
     * Tests valid statutory customs document creation passing through TradeComplianceInterceptor.
     * POST /api/interceptors/compliance-valid
     */
    @POST
    @Path("/compliance-valid")
    public Response testValidComplianceCheck() {
        try {
            CustomsDocument doc = new CustomsDocument();
            String docNumber = "DOC-EXP-2026-" + (System.currentTimeMillis() % 10000);
            doc.setDocumentNumber(docNumber);
            doc.setDocumentType(CustomsDocumentType.COMMERCIAL_INVOICE);
            doc.setStatus(CustomsDocumentStatus.SUBMITTED);
            doc.setSubmissionDeadline(LocalDate.now().plusDays(10));

            CustomsDocument created = customsService.createCustomsDocument(doc, 1L, "COMPLIANCE_AGENT");

            JsonObject response = Json.createObjectBuilder()
                    .add("complianceCleared", true)
                    .add("documentId", created.getId() != null ? created.getId() : 0L)
                    .add("documentNumber", created.getDocumentNumber())
                    .add("status", created.getStatus().name())
                    .add("message", "Customs document passed trade compliance verification and was persisted.")
                    .build();

            return Response.ok(response).build();
        } catch (Exception e) {
            JsonObject response = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", e.getMessage())
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    /**
     * Tests TradeComplianceInterceptor rejecting non-compliant customs document identifier.
     * POST /api/interceptors/compliance-invalid
     */
    @POST
    @Path("/compliance-invalid")
    public Response testInvalidComplianceCheck() {
        try {
            CustomsDocument invalidDoc = new CustomsDocument();
            invalidDoc.setDocumentNumber("X!"); // Non-compliant: Too short and contains illegal regulatory character '!'
            invalidDoc.setDocumentType(CustomsDocumentType.IMPORT_DECLARATION);

            customsService.createCustomsDocument(invalidDoc, 1L, "COMPLIANCE_AGENT");

            return Response.ok(Json.createObjectBuilder()
                    .add("status", "UNEXPECTED_SUCCESS")
                    .build()).build();
        } catch (Exception e) {
            IllegalArgumentException iae = findIllegalArgumentException(e);
            if (iae != null) {
                JsonObject response = Json.createObjectBuilder()
                        .add("complianceRejected", true)
                        .add("interceptor", "TradeComplianceInterceptor")
                        .add("caughtException", iae.getClass().getSimpleName())
                        .add("complianceViolationMessage", iae.getMessage())
                        .add("businessMethodExecuted", false)
                        .build();

                return Response.ok(response).build();
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "UNEXPECTED_ERROR")
                    .add("message", e.getMessage() != null ? e.getMessage() : "Unknown error")
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    /**
     * Traverses exception cause hierarchy to unwrap container exceptions
     * (e.g. EJBException, TransactionRolledbackLocalException) to find root IllegalArgumentException.
     */
    private IllegalArgumentException findIllegalArgumentException(Throwable t) {
        Throwable current = t;
        while (current != null) {
            if (current instanceof IllegalArgumentException iae) {
                return iae;
            }
            current = current.getCause();
        }
        return null;
    }
}
