package com.jiat.globaltrade.security;

/**
 * Compile-time security role constants for the GlobalTrade Supply Chain Management System.
 * Standardized for use in @DeclareRoles, @RolesAllowed, @RunAs, and web.xml descriptors.
 */
public final class SecurityRoles {

    /** Full administrative control across all enterprise supply chain functions. */
    public static final String ADMIN = "ADMIN";

    /** Coordinates shipment dispatches, transport carrier schedules, and route logistics. */
    public static final String LOGISTICS_COORDINATOR = "LOGISTICS_COORDINATOR";

    /** Manages statutory import/export declarations and regulatory customs clearances. */
    public static final String CUSTOMS_AGENT = "CUSTOMS_AGENT";

    /** Manages warehouse inventory, stock adjustments, and physical reconciliations. */
    public static final String WAREHOUSE_MANAGER = "WAREHOUSE_MANAGER";

    /** External vendor/supplier representative managing supplier catalog and shipment handovers. */
    public static final String VENDOR_REPRESENTATIVE = "VENDOR_REPRESENTATIVE";

    /** End-customer tracking orders, consignment deliveries, and proof of receipt. */
    public static final String CUSTOMER = "CUSTOMER";

    /**
     * Internal trusted system identity reserved for automated background tasks,
     * timer callbacks, and @RunAs internal operations. Not assigned to external users.
     */
    public static final String SYSTEM = "SYSTEM";

    private SecurityRoles() {
        // Prevent direct instantiation
    }
}
