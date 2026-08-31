package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.exception.VendorAccessDeniedException;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.security.VendorAuthorizationServiceBean;
import com.jiat.globaltrade.service.VendorServiceBean;
import com.jiat.globaltrade.web.dto.VendorResponse;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production REST Resource for Vendor queries in Admin and Staff Portals.
 * Base Path: /api/vendors
 */
@Stateless
@Path("/vendors")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.VENDOR_REPRESENTATIVE
})
public class VendorResource {

    @EJB
    private VendorServiceBean vendorService;

    @EJB
    private VendorAuthorizationServiceBean vendorAuthService;

    /**
     * Lists all registered vendors across the enterprise.
     * Restricted to administrative and logistics management roles.
     * GET /api/vendors
     */
    @GET
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    public Response getAllVendors() {
        List<Vendor> vendors = vendorService.findAllVendors();
        List<VendorResponse> response = vendors.stream()
                .map(VendorResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }

    /**
     * Returns the vendor profile for the authenticated VENDOR_REPRESENTATIVE.
     * Derives vendor identity from caller principal and vendor_user_access mapping.
     * GET /api/vendors/me
     */
    @GET
    @Path("/me")
    @RolesAllowed(SecurityRoles.VENDOR_REPRESENTATIVE)
    public Response getMyVendorProfile() throws VendorAccessDeniedException, ResourceNotFoundException {
        Vendor vendor = vendorAuthService.findMappedVendorForCaller();
        return Response.ok(VendorResponse.fromEntity(vendor)).build();
    }
}
