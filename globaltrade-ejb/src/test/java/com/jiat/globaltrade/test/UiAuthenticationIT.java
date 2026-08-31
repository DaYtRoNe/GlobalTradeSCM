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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration Test Suite for the Dedicated UI Authentication Endpoint.
 * Proves that programmatic HttpServletRequest.login() authenticates correctly against the
 * Payara Custom JAAS Realm and returns JSON 401 WITHOUT WWW-Authenticate Basic challenges upon failure.
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class UiAuthenticationIT {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return TestDeployments.createEnterpriseDeployment("ui-auth-test.war");
    }

    @ArquillianResource
    private URL deploymentUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("1. UI Login with valid credentials should return HTTP 200 JSON with principal and roles")
    void testUiLogin_validCredentials_shouldReturn200AndRoles() throws Exception {
        assertNotNull(deploymentUrl, "Deployment URL must be injected by Arquillian container");
        URI targetUri = deploymentUrl.toURI().resolve("security-test/ui-login");

        String jsonPayload = "{\"username\":\"gt_admin\",\"password\":\"Password@123\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(targetUri)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Valid UI login should produce HTTP 200 OK");
        String body = response.body();
        assertTrue(body.contains("\"authenticated\":true"), "Response must indicate authenticated: true");
        assertTrue(body.contains("\"principal\":\"gt_admin\""), "Response must identify principal gt_admin");
        assertTrue(body.contains("\"ADMIN\":true"), "Response must confirm ADMIN role");
    }

    @Test
    @DisplayName("2. UI Login with invalid credentials should return HTTP 401 JSON without WWW-Authenticate challenge")
    void testUiLogin_invalidCredentials_shouldReturn401WithoutBasicChallengeHeader() throws Exception {
        assertNotNull(deploymentUrl, "Deployment URL must be injected by Arquillian container");
        URI targetUri = deploymentUrl.toURI().resolve("security-test/ui-login");

        String jsonPayload = "{\"username\":\"gt_admin\",\"password\":\"WrongPassword!\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(targetUri)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode(), "Invalid UI login must return HTTP 401 Unauthorized");
        String body = response.body();
        assertTrue(body.contains("\"UNAUTHORIZED\""), "Response must contain UNAUTHORIZED status");

        // Crucial validation: Ensure NO WWW-Authenticate header is returned
        Optional<String> wwwAuth = response.headers().firstValue("WWW-Authenticate");
        assertFalse(wwwAuth.isPresent(), "UI Login endpoint must NOT return WWW-Authenticate header to prevent browser popups");
    }
}
