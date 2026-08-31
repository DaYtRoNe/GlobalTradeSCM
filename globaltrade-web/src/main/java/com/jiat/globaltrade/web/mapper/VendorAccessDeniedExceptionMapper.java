package com.jiat.globaltrade.web.mapper;

import com.jiat.globaltrade.exception.VendorAccessDeniedException;
import com.jiat.globaltrade.web.dto.ApiErrorResponse;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JAX-RS ExceptionMapper for VendorAccessDeniedException.
 * Maps fine-grained vendor data ownership denials to HTTP 403 Forbidden responses.
 */
@Provider
public class VendorAccessDeniedExceptionMapper implements ExceptionMapper<VendorAccessDeniedException> {

    private static final Logger LOGGER = Logger.getLogger(VendorAccessDeniedExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(VendorAccessDeniedException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "/api";
        LOGGER.log(Level.WARNING, "[VendorAccessDeniedExceptionMapper] Fine-grained vendor access denied at {0}: {1}",
                new Object[]{path, exception.getMessage()});

        ApiErrorResponse error = new ApiErrorResponse(
                "FORBIDDEN",
                "VENDOR_ACCESS_DENIED",
                "Access denied to the requested vendor.",
                path
        );

        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
