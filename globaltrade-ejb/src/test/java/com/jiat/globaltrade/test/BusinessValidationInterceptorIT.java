package com.jiat.globaltrade.test;

import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.service.VendorServiceBean;
import jakarta.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Phase 7B-1 Integration Test: Verifies that EJB interceptor chaining
 * executes and that BusinessValidationInterceptor rejects invalid input parameters
 * before business method execution, preserving entity state.
 */
@ExtendWith(ArquillianExtension.class)
public class BusinessValidationInterceptorIT {

    @Deployment
    public static WebArchive createDeployment() {
        return TestDeployments.createEnterpriseDeployment("interceptor-test.war");
    }

    @EJB
    private AdminTestInvoker adminTestInvoker;

    @EJB
    private VendorServiceBean vendorService;

    @Test
    @DisplayName("Should intercept and reject invalid vendor rating (9.99 > 5.00) without mutating vendor state")
    void shouldInterceptAndRejectInvalidRating() {
        assertNotNull(adminTestInvoker, "AdminTestInvoker must be injected by container");
        assertNotNull(vendorService, "VendorServiceBean must be injected by container");

        Long targetVendorId = 1L;
        Vendor initialVendor = vendorService.findVendorById(targetVendorId);
        assertNotNull(initialVendor, "Vendor #1 must exist in seeded database");
        BigDecimal initialRating = initialVendor.getPerformanceRating();

        // Attempt update with an invalid rating (9.99) outside the allowed [0.00, 5.00] range
        BigDecimal invalidRating = BigDecimal.valueOf(9.99);
        boolean exceptionCaught = false;

        try {
            adminTestInvoker.updatePerformanceRating(targetVendorId, invalidRating, "ARQUILLIAN_IT_RUNNER");
            fail("Expected IllegalArgumentException from BusinessValidationInterceptor was not thrown");
        } catch (Exception e) {
            IllegalArgumentException iae = TestDeployments.findException(e, IllegalArgumentException.class);
            assertNotNull(iae, "Exception cause chain must contain IllegalArgumentException thrown by interceptor. Received: " + e.getClass().getName() + " -> " + e.getMessage());
            assertTrue(iae.getMessage().contains("Performance rating") || iae.getMessage().contains("between 0.00 and 5.00"),
                    "Exception message should indicate rating range validation failure. Message: " + iae.getMessage());
            exceptionCaught = true;
        }

        assertTrue(exceptionCaught, "IllegalArgumentException must have been thrown and intercepted");

        // Verify that entity state was not modified
        Vendor vendorAfter = vendorService.findVendorById(targetVendorId);
        assertNotNull(vendorAfter);
        if (initialRating == null) {
            assertEquals(null, vendorAfter.getPerformanceRating(),
                    "Vendor rating must remain null because invocation was blocked by interceptor");
        } else {
            assertEquals(0, initialRating.compareTo(vendorAfter.getPerformanceRating()),
                    "Vendor rating must remain unchanged because invocation was blocked by interceptor");
        }
    }
}
