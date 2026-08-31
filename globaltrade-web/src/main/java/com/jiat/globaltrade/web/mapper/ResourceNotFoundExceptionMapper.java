package com.jiat.globaltrade.web.mapper;

import com.jiat.globaltrade.exception.ResourceNotFoundException;
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
 * JAX-RS ExceptionMapper for ResourceNotFoundException.
 * Maps missing resource lookup exceptions to clean HTTP 404 Not Found responses.
 */
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

    private static final Logger LOGGER = Logger.getLogger(ResourceNotFoundExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ResourceNotFoundException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "/api";
        LOGGER.log(Level.INFO, "[ResourceNotFoundExceptionMapper] Resource not found at {0}: {1}",
                new Object[]{path, exception.getMessage()});

        ApiErrorResponse error = new ApiErrorResponse(
                "NOT_FOUND",
                "RESOURCE_NOT_FOUND",
                exception.getMessage(),
                path
        );

        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
