package com.jiat.globaltrade.web.mapper;

import com.jiat.globaltrade.exception.ShipmentAccessDeniedException;
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
 * JAX-RS ExceptionMapper for ShipmentAccessDeniedException.
 * Maps fine-grained customer consignment ownership violations to safe HTTP 403 Forbidden responses.
 */
@Provider
public class ShipmentAccessDeniedExceptionMapper implements ExceptionMapper<ShipmentAccessDeniedException> {

    private static final Logger LOGGER = Logger.getLogger(ShipmentAccessDeniedExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ShipmentAccessDeniedException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "/api";
        LOGGER.log(Level.WARNING, "[ShipmentAccessDeniedExceptionMapper] Shipment ownership violation at {0}: {1}",
                new Object[]{path, exception.getMessage()});

        ApiErrorResponse error = new ApiErrorResponse(
                "FORBIDDEN",
                "SHIPMENT_ACCESS_DENIED",
                "Access denied to the requested shipment.",
                path
        );

        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
