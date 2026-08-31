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
 * Phase 7B-2 Integration Test Suite: Real Payara Container Security, Custom JAAS Realm,
 * HTTP Basic Authentication, Declarative RBAC, and Fine-Grained Authorization.
 *
 * Runs client-side against the live Payara Server exercising the full security flow:
 * HTTP Client -> Basic Auth Header -> Payara Security Pipeline -> GlobalTradeCustomRealm
 * -> GlobalTradeLoginModule -> Database SHA-256 Auth -> Role Mapping -> Secured Components.
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class SecurityAuthenticationIT {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return TestDeployments.createEnterpriseDeployment("security-authentication-test.war");
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
    @DisplayName("A. Unauthenticated request to protected endpoint should return HTTP 401 Unauthorized")
    void testNoCredentials_shouldReturn401() throws Exception {
        HttpResponse<String> response = sendGet("security-test/whoami", null, null);
        assertEquals(401, response.statusCode(), "Unauthenticated request must be challenged with HTTP 401");
    }

    @Test
    @DisplayName("B. Correct ADMIN credentials should authenticate via GlobalTradeCustomRealm and return HTTP 200")
    void testValidAdminCredentials_shouldReturn200AndCallerSummary() throws Exception {
        HttpResponse<String> response = sendGet("security-test/whoami", "gt_admin", "Password@123");
        assertEquals(200, response.statusCode(), "Valid admin credentials must return HTTP 200");
        String body = response.body();
        assertTrue(body.contains("\"principal\":\"gt_admin\""), "Principal should be 'gt_admin'");
        assertTrue(body.contains("\"ADMIN\":true"), "Caller must possess ADMIN role");
    }

    @Test
    @DisplayName("C. Wrong password for existing user should be rejected with HTTP 401 Unauthorized")
    void testWrongPassword_shouldReturn401() throws Exception {
        HttpResponse<String> response = sendGet("security-test/whoami", "gt_admin", "WrongPassword@999");
        assertEquals(401, response.statusCode(), "Invalid password must be rejected with HTTP 401");
    }

    @Test
    @DisplayName("D. CUSTOMS_AGENT attempting to access ADMIN-only endpoint should be forbidden (HTTP 403)")
    void testCustomsUserOnAdminEndpoint_shouldReturn403() throws Exception {
        HttpResponse<String> response = sendGet("security-test/admin", "gt_customs", "Password@123");
        assertEquals(403, response.statusCode(), "Non-admin role must be denied access to admin endpoint with HTTP 403");
    }

    @Test
    @DisplayName("E. ADMIN user accessing ADMIN-only endpoint should succeed (HTTP 200)")
    void testAdminOnAdminEndpoint_shouldReturn200() throws Exception {
        HttpResponse<String> response = sendGet("security-test/admin", "gt_admin", "Password@123");
        assertEquals(200, response.statusCode(), "Admin must have access to admin endpoint");
        assertTrue(response.body().contains("Admin clearance confirmed"));
    }

    @Test
    @DisplayName("F. CUSTOMS_AGENT accessing customs-protected endpoint should succeed (HTTP 200)")
    void testCustomsUserOnCustomsEndpoint_shouldReturn200() throws Exception {
        HttpResponse<String> response = sendGet("security-test/customs", "gt_customs", "Password@123");
        assertEquals(200, response.statusCode(), "Customs agent must have access to customs endpoint");
        assertTrue(response.body().contains("Customs clearance confirmed"));
    }

    @Test
    @DisplayName("G. WAREHOUSE_MANAGER accessing customs-protected endpoint should be forbidden (HTTP 403)")
    void testWarehouseUserOnCustomsEndpoint_shouldReturn403() throws Exception {
        HttpResponse<String> response = sendGet("security-test/customs", "gt_warehouse", "Password@123");
        assertEquals(403, response.statusCode(), "Warehouse manager must be denied access to customs endpoint with HTTP 403");
    }

    @Test
    @DisplayName("H. VENDOR_REPRESENTATIVE accessing mapped Vendor #1 should succeed (HTTP 200)")
    void testVendorAccessingMappedVendor_shouldReturn200() throws Exception {
        HttpResponse<String> response = sendGet("security-test/vendor/1", "gt_vendor", "Password@123");
        assertEquals(200, response.statusCode(), "Vendor representative must be authorized to access mapped Vendor #1");
        String body = response.body();
        assertTrue(body.contains("\"vendorId\":1"), "Response should contain Vendor #1 data");
        assertTrue(body.contains("\"caller\":\"gt_vendor\""), "Caller principal must propagate as 'gt_vendor'");
    }

    @Test
    @DisplayName("I. VENDOR_REPRESENTATIVE accessing unmapped Vendor #2 should be denied (HTTP 403)")
    void testVendorAccessingUnmappedVendor_shouldReturn403() throws Exception {
        HttpResponse<String> response = sendGet("security-test/vendor/2", "gt_vendor", "Password@123");
        assertEquals(403, response.statusCode(), "Vendor representative must be denied cross-vendor access to Vendor #2");
    }

    @Test
    @DisplayName("J. ADMIN accessing Vendor #2 should bypass vendor restrictions (HTTP 200)")
    void testAdminAccessingVendor2_shouldReturn200() throws Exception {
        HttpResponse<String> response = sendGet("security-test/vendor/2", "gt_admin", "Password@123");
        assertEquals(200, response.statusCode(), "Admin must have enterprise-wide access to Vendor #2");
        assertTrue(response.body().contains("\"vendorId\":2"), "Response should contain Vendor #2 data");
    }
}
