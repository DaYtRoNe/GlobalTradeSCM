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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration Test Suite for Shipment List RBAC Alignment.
 * Verifies that WAREHOUSE_MANAGER and CUSTOMS_AGENT have read access to the global shipment list,
 * while CUSTOMER and VENDOR_REPRESENTATIVE are strictly denied (HTTP 403 Forbidden).
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class ShipmentSecurityRbacIT {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return TestDeployments.createEnterpriseDeployment("shipment-security-rbac-test.war");
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
    @DisplayName("1. WAREHOUSE_MANAGER should have READ access to global shipment list (HTTP 200 OK)")
    void testWarehouseManager_canAccessShipmentsList_shouldReturn200() throws Exception {
        HttpResponse<String> response = sendGet("security-test/staff-data/shipments", "gt_warehouse", "Password@123");
        assertEquals(200, response.statusCode(), "Warehouse Manager must have read access to shipments list");
        assertTrue(response.body().contains("\"status\":\"SUCCESS\""), "Response should indicate success");
    }

    @Test
    @DisplayName("2. CUSTOMS_AGENT should have READ access to global shipment list (HTTP 200 OK)")
    void testCustomsAgent_canAccessShipmentsList_shouldReturn200() throws Exception {
        HttpResponse<String> response = sendGet("security-test/staff-data/shipments", "gt_customs", "Password@123");
        assertEquals(200, response.statusCode(), "Customs Agent must have read access to shipments list");
        assertTrue(response.body().contains("\"status\":\"SUCCESS\""), "Response should indicate success");
    }

    @Test
    @DisplayName("3. CUSTOMER must NOT have access to global shipment list (HTTP 403 Forbidden)")
    void testCustomer_cannotAccessGlobalShipmentsList_shouldReturn403() throws Exception {
        HttpResponse<String> response = sendGet("security-test/staff-data/shipments", "gt_customer", "Password@123");
        assertEquals(403, response.statusCode(), "Customer must receive HTTP 403 Forbidden when requesting global shipment list");
    }

    @Test
    @DisplayName("4. VENDOR_REPRESENTATIVE must NOT have access to global shipment list (HTTP 403 Forbidden)")
    void testVendorRepresentative_cannotAccessGlobalShipmentsList_shouldReturn403() throws Exception {
        HttpResponse<String> response = sendGet("security-test/staff-data/shipments", "gt_vendor", "Password@123");
        assertEquals(403, response.statusCode(), "Vendor Representative must receive HTTP 403 Forbidden when requesting global shipment list");
    }
}
