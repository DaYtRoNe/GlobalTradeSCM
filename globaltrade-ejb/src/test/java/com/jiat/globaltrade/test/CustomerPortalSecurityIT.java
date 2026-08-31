package com.jiat.globaltrade.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 10B Integration Test Suite: Customer Portal Security, Customer-Scoped Consignment
 * Queries, Fine-Grained Shipment & Customs Ownership Authorization, and Staff Data Operations.
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class CustomerPortalSecurityIT {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return TestDeployments.createEnterpriseDeployment("customer-portal-security-test.war");
    }

    @ArquillianResource
    private URL deploymentUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private HttpResponse<String> sendGet(String path, String username, String password) throws Exception {
        assertNotNull(deploymentUrl, "Deployment URL must be injected by Arquillian container");
        URI targetUri = deploymentUrl.toURI().resolve(path);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(targetUri)
                .GET();

        if (username != null && password != null) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encodedAuth);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("1. Customer-scoped query returns only shipments assigned to authenticated caller (gt_customer)")
    void testMyShipments_returnsOnlyAssignedShipments() throws Exception {
        HttpResponse<String> response = sendGet("security-test/shipment/my-shipments", "gt_customer", "Password@123");
        assertEquals(200, response.statusCode(), "Authenticated customer should receive HTTP 200 for my-shipments");
        String body = response.body();
        assertTrue(body.contains("TRK-2026-001"), "Customer's own shipment TRK-2026-001 must be present in response");
        assertFalse(body.contains("TRK-2026-002"), "Unowned shipment TRK-2026-002 must NOT be present in customer response");
    }

    @Test
    @DisplayName("2. CUSTOMER accessing own assigned shipment (#1) should succeed with HTTP 200")
    void testCustomer_canAccessOwnShipment_shouldReturn200() throws Exception {
        HttpResponse<String> response = sendGet("security-test/shipment/1", "gt_customer", "Password@123");
        assertEquals(200, response.statusCode(), "Customer should be authorized to access own shipment");
        String body = response.body();
        assertTrue(body.contains("TRK-2026-001"), "Response must contain TRK-2026-001");
        assertTrue(body.contains("gt_customer"), "Customer username must match");
    }

    @Test
    @DisplayName("3. CUSTOMER accessing unowned shipment (#2) should be denied with HTTP 403 Forbidden")
    void testCustomer_cannotAccessUnownedShipment_shouldReturn403() throws Exception {
        HttpResponse<String> response = sendGet("security-test/shipment/2", "gt_customer", "Password@123");
        assertEquals(403, response.statusCode(), "Customer must be rejected with HTTP 403 when accessing unowned shipment");
    }

    @Test
    @DisplayName("4. ADMIN accessing unowned shipment (#2) should succeed with HTTP 200")
    void testAdmin_canAccessAnyShipment_shouldReturn200() throws Exception {
        HttpResponse<String> response = sendGet("security-test/shipment/2", "gt_admin", "Password@123");
        assertEquals(200, response.statusCode(), "Admin must have enterprise-wide access to all shipments");
        assertTrue(response.body().contains("TRK-2026-002"), "Response must contain TRK-2026-002");
    }

    @Test
    @DisplayName("5. CUSTOMER accessing customs data for own shipment (#1) should succeed with HTTP 200")
    void testCustomer_canAccessCustomsForOwnShipment_shouldReturn200() throws Exception {
        HttpResponse<String> response = sendGet("security-test/customs-docs/1", "gt_customer", "Password@123");
        assertEquals(200, response.statusCode(), "Customer should be authorized to view customs for own shipment");
        assertTrue(response.body().contains("\"status\":\"SUCCESS\""));
    }

    @Test
    @DisplayName("6. CUSTOMER accessing customs data for unowned shipment (#2) should be denied with HTTP 403 Forbidden")
    void testCustomer_cannotAccessCustomsForUnownedShipment_shouldReturn403() throws Exception {
        HttpResponse<String> response = sendGet("security-test/customs-docs/2", "gt_customer", "Password@123");
        assertEquals(403, response.statusCode(), "Customer must be rejected with HTTP 403 for unowned shipment customs data");
    }

    @Test
    @DisplayName("7. Authorized staff (ADMIN) can query global listing endpoints (vendors, inventory, customs, audit-logs)")
    void testStaffListing_forAuthorizedAdmin() throws Exception {
        HttpResponse<String> vResp = sendGet("security-test/staff-data/vendors", "gt_admin", "Password@123");
        assertEquals(200, vResp.statusCode(), "Admin should have access to vendors list");

        HttpResponse<String> iResp = sendGet("security-test/staff-data/inventory", "gt_admin", "Password@123");
        assertEquals(200, iResp.statusCode(), "Admin should have access to inventory list");

        HttpResponse<String> cResp = sendGet("security-test/staff-data/customs", "gt_admin", "Password@123");
        assertEquals(200, cResp.statusCode(), "Admin should have access to customs list");

        HttpResponse<String> aResp = sendGet("security-test/staff-data/audit-logs", "gt_admin", "Password@123");
        assertEquals(200, aResp.statusCode(), "Admin should have access to audit logs list");
    }
}
