package com.jiat.globaltrade.test;

import com.jiat.globaltrade.service.SystemHealthBean;
import jakarta.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 7A Smoke Integration Test verifying JUnit 5 + Arquillian integration
 * against a running remote Payara 6 Server.
 *
 * Verifies:
 * - Arquillian connects to Payara Server Remote adapter (admin port 4848, HTTP port 8080)
 * - Test deployment archive is created via ShrinkWrap and deployed to Payara
 * - Jakarta EE container injects the @EJB SystemHealthBean into the test instance
 * - JUnit 5 assertions execute and validate the live container state
 */
@ExtendWith(ArquillianExtension.class)
public class ArquillianContainerSmokeIT {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "globaltrade-smoke-test.war")
                .addClass(SystemHealthBean.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @EJB
    private SystemHealthBean systemHealthBean;

    @Test
    @DisplayName("Should successfully deploy to remote Payara, inject SystemHealthBean, and verify status")
    void shouldInjectSystemHealthBeanAndVerifyStatus() {
        assertNotNull(systemHealthBean, "SystemHealthBean must be injected by Payara EJB container");
        String status = systemHealthBean.getStatus();
        assertNotNull(status, "SystemHealthBean.getStatus() must not return null");
        assertEquals("GlobalTrade EJB Module is running", status,
                "SystemHealthBean status must match expected production string");
    }
}
