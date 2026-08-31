package com.jiat.globaltrade.web.mapper;

import com.jiat.globaltrade.exception.InsufficientInventoryException;
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
 * JAX-RS ExceptionMapper for InsufficientInventoryException.
 * Maps stock shortage business exceptions to HTTP 409 Conflict responses.
 */
@Provider
public class InsufficientInventoryExceptionMapper implements ExceptionMapper<InsufficientInventoryException> {

    private static final Logger LOGGER = Logger.getLogger(InsufficientInventoryExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(InsufficientInventoryException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "/api";
        LOGGER.log(Level.WARNING, "[InsufficientInventoryExceptionMapper] Stock conflict at {0}: {1}",
                new Object[]{path, exception.getMessage()});

        ApiErrorResponse error = new ApiErrorResponse(
                "CONFLICT",
                "INSUFFICIENT_INVENTORY",
                exception.getMessage(),
                path
        );

        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
