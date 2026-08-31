package com.jiat.globaltrade.web.mapper;

import com.jiat.globaltrade.web.dto.ApiErrorResponse;
import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JAX-RS ExceptionMapper for EJBAccessException.
 * Maps container-level EJB authorization and RBAC role mismatch rejections to HTTP 403 Forbidden.
 */
@Provider
public class EJBAccessExceptionMapper implements ExceptionMapper<EJBAccessException> {

    private static final Logger LOGGER = Logger.getLogger(EJBAccessExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(EJBAccessException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "/api";
        LOGGER.log(Level.WARNING, "[EJBAccessExceptionMapper] Container RBAC authorization failure at {0}: {1}",
                new Object[]{path, exception.getMessage()});

        ApiErrorResponse error = new ApiErrorResponse(
                "FORBIDDEN",
                "ACCESS_DENIED",
                "Access Denied: Caller does not possess the required security role.",
                path
        );

        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
