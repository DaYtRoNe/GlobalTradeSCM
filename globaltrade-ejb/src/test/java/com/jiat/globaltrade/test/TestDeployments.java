package com.jiat.globaltrade.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Reusable ShrinkWrap deployment factory for Phase 7 integration test suites.
 * Assembles a focused Jakarta EE WebArchive containing the necessary entity,
 * service, interceptor, security, and persistence resources.
 */
public final class TestDeployments {

    private static final String TEST_WEB_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                                         https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
                     version="6.0">

                <display-name>GlobalTrade Test Deployment</display-name>

                <!-- Constraint 1: Protected WhoAmI Probe - requires any valid authenticated role -->
                <security-constraint>
                    <display-name>WhoAmI Probe Protection</display-name>
                    <web-resource-collection>
                        <web-resource-name>WhoAmI</web-resource-name>
                        <url-pattern>/security-test/whoami</url-pattern>
                    </web-resource-collection>
                    <auth-constraint>
                        <role-name>ADMIN</role-name>
                        <role-name>LOGISTICS_COORDINATOR</role-name>
                        <role-name>CUSTOMS_AGENT</role-name>
                        <role-name>WAREHOUSE_MANAGER</role-name>
                        <role-name>VENDOR_REPRESENTATIVE</role-name>
                        <role-name>CUSTOMER</role-name>
                        <role-name>SYSTEM</role-name>
                    </auth-constraint>
                </security-constraint>

                <!-- Constraint 2: Admin-Only Probe -->
                <security-constraint>
                    <display-name>Admin Probe Protection</display-name>
                    <web-resource-collection>
                        <web-resource-name>Admin Endpoint</web-resource-name>
                        <url-pattern>/security-test/admin</url-pattern>
                    </web-resource-collection>
                    <auth-constraint>
                        <role-name>ADMIN</role-name>
                    </auth-constraint>
                </security-constraint>

                <!-- Constraint 3: Customs Agent Probe -->
                <security-constraint>
                    <display-name>Customs Probe Protection</display-name>
                    <web-resource-collection>
                        <web-resource-name>Customs Endpoint</web-resource-name>
                        <url-pattern>/security-test/customs</url-pattern>
                    </web-resource-collection>
                    <auth-constraint>
                        <role-name>CUSTOMS_AGENT</role-name>
                        <role-name>ADMIN</role-name>
                    </auth-constraint>
                </security-constraint>

                <!-- Constraint 4: Vendor Data Probe -->
                <security-constraint>
                    <display-name>Vendor Data Probe Protection</display-name>
                    <web-resource-collection>
                        <web-resource-name>Vendor Endpoint</web-resource-name>
                        <url-pattern>/security-test/vendor/*</url-pattern>
                    </web-resource-collection>
                    <auth-constraint>
                        <role-name>ADMIN</role-name>
                        <role-name>LOGISTICS_COORDINATOR</role-name>
                        <role-name>VENDOR_REPRESENTATIVE</role-name>
                    </auth-constraint>
                </security-constraint>

                <!-- Constraint 5: Shipment Data Probe -->
                <security-constraint>
                    <display-name>Shipment Data Probe Protection</display-name>
                    <web-resource-collection>
                        <web-resource-name>Shipment Endpoint</web-resource-name>
                        <url-pattern>/security-test/shipment/*</url-pattern>
                    </web-resource-collection>
                    <auth-constraint>
                        <role-name>ADMIN</role-name>
                        <role-name>LOGISTICS_COORDINATOR</role-name>
                        <role-name>CUSTOMS_AGENT</role-name>
                        <role-name>WAREHOUSE_MANAGER</role-name>
                        <role-name>CUSTOMER</role-name>
                    </auth-constraint>
                </security-constraint>

                <!-- Constraint 6: Customs Docs Probe -->
                <security-constraint>
                    <display-name>Customs Docs Probe Protection</display-name>
                    <web-resource-collection>
                        <web-resource-name>Customs Docs Endpoint</web-resource-name>
                        <url-pattern>/security-test/customs-docs/*</url-pattern>
                    </web-resource-collection>
                    <auth-constraint>
                        <role-name>ADMIN</role-name>
                        <role-name>CUSTOMS_AGENT</role-name>
                        <role-name>LOGISTICS_COORDINATOR</role-name>
                        <role-name>CUSTOMER</role-name>
                    </auth-constraint>
                </security-constraint>

                <!-- Constraint 7: Staff Data Probe -->
                <security-constraint>
                    <display-name>Staff Data Probe Protection</display-name>
                    <web-resource-collection>
                        <web-resource-name>Staff Data Endpoint</web-resource-name>
                        <url-pattern>/security-test/staff-data/*</url-pattern>
                    </web-resource-collection>
                    <auth-constraint>
                        <role-name>ADMIN</role-name>
                        <role-name>LOGISTICS_COORDINATOR</role-name>
                        <role-name>CUSTOMS_AGENT</role-name>
                        <role-name>WAREHOUSE_MANAGER</role-name>
                    </auth-constraint>
                </security-constraint>

                <!-- Authentication Mechanism: HTTP Basic with GlobalTradeCustomRealm -->
                <login-config>
                    <auth-method>BASIC</auth-method>
                    <realm-name>GlobalTradeCustomRealm</realm-name>
                </login-config>

                <!-- Application Security Roles -->
                <security-role>
                    <role-name>ADMIN</role-name>
                </security-role>
                <security-role>
                    <role-name>LOGISTICS_COORDINATOR</role-name>
                </security-role>
                <security-role>
                    <role-name>CUSTOMS_AGENT</role-name>
                </security-role>
                <security-role>
                    <role-name>WAREHOUSE_MANAGER</role-name>
                </security-role>
                <security-role>
                    <role-name>VENDOR_REPRESENTATIVE</role-name>
                </security-role>
                <security-role>
                    <role-name>CUSTOMER</role-name>
                </security-role>
                <security-role>
                    <role-name>SYSTEM</role-name>
                </security-role>

                <deny-uncovered-http-methods/>

            </web-app>
            """;

    private static final String TEST_GLASSFISH_WEB_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE glassfish-web-app PUBLIC "-//GlassFish.org//DTD GlassFish Application Server 3.1 Servlet 3.0//EN" "http://glassfish.org/dtds/glassfish-web-app_3_0-1.dtd">
            <glassfish-web-app>
                <security-role-mapping>
                    <role-name>ADMIN</role-name>
                    <principal-name>gt_admin</principal-name>
                    <group-name>ADMIN</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>LOGISTICS_COORDINATOR</role-name>
                    <principal-name>gt_coordinator</principal-name>
                    <group-name>LOGISTICS_COORDINATOR</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>CUSTOMS_AGENT</role-name>
                    <principal-name>gt_customs</principal-name>
                    <group-name>CUSTOMS_AGENT</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>WAREHOUSE_MANAGER</role-name>
                    <principal-name>gt_warehouse</principal-name>
                    <group-name>WAREHOUSE_MANAGER</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>VENDOR_REPRESENTATIVE</role-name>
                    <principal-name>gt_vendor</principal-name>
                    <group-name>VENDOR_REPRESENTATIVE</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>CUSTOMER</role-name>
                    <principal-name>gt_customer</principal-name>
                    <group-name>CUSTOMER</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>SYSTEM</role-name>
                    <principal-name>gt_system</principal-name>
                    <group-name>SYSTEM</group-name>
                </security-role-mapping>
            </glassfish-web-app>
            """;

    private static final String TEST_GLASSFISH_EJB_JAR_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE glassfish-ejb-jar PUBLIC "-//GlassFish.org//DTD GlassFish Application Server 3.1 EJB 3.1//EN" "http://glassfish.org/dtds/glassfish-ejb-jar_3_1-1.dtd">
            <glassfish-ejb-jar>
                <security-role-mapping>
                    <role-name>ADMIN</role-name>
                    <principal-name>gt_admin</principal-name>
                    <group-name>ADMIN</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>LOGISTICS_COORDINATOR</role-name>
                    <principal-name>gt_coordinator</principal-name>
                    <group-name>LOGISTICS_COORDINATOR</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>CUSTOMS_AGENT</role-name>
                    <principal-name>gt_customs</principal-name>
                    <group-name>CUSTOMS_AGENT</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>WAREHOUSE_MANAGER</role-name>
                    <principal-name>gt_warehouse</principal-name>
                    <group-name>WAREHOUSE_MANAGER</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>VENDOR_REPRESENTATIVE</role-name>
                    <principal-name>gt_vendor</principal-name>
                    <group-name>VENDOR_REPRESENTATIVE</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>CUSTOMER</role-name>
                    <principal-name>gt_customer</principal-name>
                    <group-name>CUSTOMER</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>SYSTEM</role-name>
                    <principal-name>gt_system</principal-name>
                    <group-name>SYSTEM</group-name>
                </security-role-mapping>
            </glassfish-ejb-jar>
            """;

    private TestDeployments() {
    }

    /**
     * Creates an enterprise test archive with the full domain model, services,
     * interceptors, persistence context, security descriptors, and test helpers.
     */
    public static WebArchive createEnterpriseDeployment(String archiveName) {
        return ShrinkWrap.create(WebArchive.class, archiveName)
                // Entities and Enums
                .addPackage("com.jiat.globaltrade.entity")
                .addPackage("com.jiat.globaltrade.entity.enums")
                // Exceptions
                .addPackage("com.jiat.globaltrade.exception")
                // Interceptors
                .addPackage("com.jiat.globaltrade.interceptor")
                // Core Services
                .addPackage("com.jiat.globaltrade.service")
                // Security definitions and DTOs
                .addPackage("com.jiat.globaltrade.security")
                .addPackage("com.jiat.globaltrade.security.dto")
                // Test helper EJB, probe servlet, and test classes
                .addClass(AdminTestInvoker.class)
                .addClass(SecurityTestProbeServlet.class)
                .addClass(TestDeployments.class)
                // Real JPA persistence.xml
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                // CDI configuration
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                // Security descriptors targeting GlobalTradeCustomRealm
                .addAsWebInfResource(new StringAsset(TEST_WEB_XML), "web.xml")
                .addAsWebInfResource(new StringAsset(TEST_GLASSFISH_WEB_XML), "glassfish-web.xml")
                .addAsWebInfResource(new StringAsset(TEST_GLASSFISH_EJB_JAR_XML), "glassfish-ejb-jar.xml");
    }

    /**
     * Traverses the exception cause hierarchy to find an instance of the target exception type.
     */
    public static <T extends Throwable> T findException(Throwable t, Class<T> targetClass) {
        Throwable current = t;
        while (current != null) {
            if (targetClass.isInstance(current)) {
                return targetClass.cast(current);
            }
            if (current.getCause() == null || current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return null;
    }
}
