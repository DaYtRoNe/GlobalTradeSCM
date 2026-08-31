package com.jiat.globaltrade.web.mapper;

import com.jiat.globaltrade.exception.BusinessRuleViolationException;
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
 * JAX-RS ExceptionMapper for BusinessRuleViolationException.
 * Maps operational constraint violations to HTTP 400 Bad Request responses.
 */
@Provider
public class BusinessRuleViolationExceptionMapper implements ExceptionMapper<BusinessRuleViolationException> {

    private static final Logger LOGGER = Logger.getLogger(BusinessRuleViolationExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(BusinessRuleViolationException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "/api";
        LOGGER.log(Level.WARNING, "[BusinessRuleViolationExceptionMapper] Business rule violated at {0}: {1}",
                new Object[]{path, exception.getMessage()});

        ApiErrorResponse error = new ApiErrorResponse(
                "BAD_REQUEST",
                "BUSINESS_RULE_VIOLATION",
                exception.getMessage(),
                path
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
