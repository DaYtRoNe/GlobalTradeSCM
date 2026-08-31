package com.jiat.globaltrade.web.mapper;

import com.jiat.globaltrade.exception.BusinessRuleViolationException;
import com.jiat.globaltrade.exception.InsufficientInventoryException;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.exception.VendorAccessDeniedException;
import com.jiat.globaltrade.web.dto.ApiErrorResponse;
import jakarta.ejb.AccessLocalException;
import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Universal fallback ExceptionMapper for all unhandled Throwables and container EJBException wrappers.
 * Traverses the cause chain to recognize embedded application/business exceptions,
 * and securely masks genuine system exceptions behind generic HTTP 500 responses without leaking internals.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "/api";

        // Unpack nested causes to inspect for known business/application exceptions
        Throwable root = unwrapException(exception);

        if (root instanceof ResourceNotFoundException rnf) {
            LOGGER.log(Level.INFO, "[GenericExceptionMapper] Unwrapped ResourceNotFoundException at {0}: {1}",
                    new Object[]{path, rnf.getMessage()});
            ApiErrorResponse error = new ApiErrorResponse("NOT_FOUND", "RESOURCE_NOT_FOUND", rnf.getMessage(), path);
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(error).build();
        }

        if (root instanceof InsufficientInventoryException iie) {
            LOGGER.log(Level.WARNING, "[GenericExceptionMapper] Unwrapped InsufficientInventoryException at {0}: {1}",
                    new Object[]{path, iie.getMessage()});
            ApiErrorResponse error = new ApiErrorResponse("CONFLICT", "INSUFFICIENT_INVENTORY", iie.getMessage(), path);
            return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON).entity(error).build();
        }

        if (root instanceof BusinessRuleViolationException brv) {
            LOGGER.log(Level.WARNING, "[GenericExceptionMapper] Unwrapped BusinessRuleViolationException at {0}: {1}",
                    new Object[]{path, brv.getMessage()});
            ApiErrorResponse error = new ApiErrorResponse("BAD_REQUEST", "BUSINESS_RULE_VIOLATION", brv.getMessage(), path);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(error).build();
        }

        if (root instanceof VendorAccessDeniedException vad) {
            LOGGER.log(Level.WARNING, "[GenericExceptionMapper] Unwrapped VendorAccessDeniedException at {0}: {1}",
                    new Object[]{path, vad.getMessage()});
            ApiErrorResponse error = new ApiErrorResponse("FORBIDDEN", "VENDOR_ACCESS_DENIED", "Access denied to the requested vendor.", path);
            return Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(error).build();
        }

        if (root instanceof EJBAccessException || root instanceof AccessLocalException || root instanceof SecurityException) {
            LOGGER.log(Level.WARNING, "[GenericExceptionMapper] Unwrapped Security/RBAC failure at {0}: {1}",
                    new Object[]{path, root.getMessage()});
            ApiErrorResponse error = new ApiErrorResponse("FORBIDDEN", "ACCESS_DENIED", "Access Denied: Caller does not possess the required security role.", path);
            return Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(error).build();
        }

        if (root instanceof IllegalArgumentException iae) {
            LOGGER.log(Level.INFO, "[GenericExceptionMapper] Unwrapped IllegalArgumentException at {0}: {1}",
                    new Object[]{path, iae.getMessage()});
            ApiErrorResponse error = new ApiErrorResponse("BAD_REQUEST", "VALIDATION_ERROR",
                    iae.getMessage() != null ? iae.getMessage() : "Invalid input parameters.", path);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(error).build();
        }

        if (root instanceof WebApplicationException wae) {
            int statusCode = wae.getResponse() != null ? wae.getResponse().getStatus() : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            Response.Status responseStatus = Response.Status.fromStatusCode(statusCode);
            String statusName = responseStatus != null ? responseStatus.name() : "HTTP_" + statusCode;
            String message = wae.getMessage() != null && !wae.getMessage().isEmpty()
                    ? wae.getMessage()
                    : (responseStatus != null ? responseStatus.getReasonPhrase() : "HTTP error " + statusCode);

            LOGGER.log(Level.INFO, "[GenericExceptionMapper] Unwrapped WebApplicationException (HTTP {0}) at {1}: {2}",
                    new Object[]{statusCode, path, message});
            ApiErrorResponse error = new ApiErrorResponse(statusName, "HTTP_" + statusCode, message, path);
            return Response.status(statusCode).type(MediaType.APPLICATION_JSON).entity(error).build();
        }

        // Genuine unhandled server/system exception: Log detailed technical trace on server, return safe generic message to client
        LOGGER.log(Level.SEVERE, "[GenericExceptionMapper] Unexpected system failure at " + path + ": " + exception.getMessage(), exception);

        ApiErrorResponse error = new ApiErrorResponse(
                "ERROR",
                "INTERNAL_SERVER_ERROR",
                "An unexpected system error occurred while processing the request.",
                path
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }

    private Throwable unwrapException(Throwable t) {
        Throwable current = t;
        while (current != null) {
            if (current instanceof ResourceNotFoundException
                    || current instanceof InsufficientInventoryException
                    || current instanceof BusinessRuleViolationException
                    || current instanceof VendorAccessDeniedException
                    || current instanceof EJBAccessException
                    || current instanceof AccessLocalException
                    || current instanceof SecurityException
                    || current instanceof IllegalArgumentException
                    || current instanceof WebApplicationException) {
                return current;
            }
            if (current.getCause() == null || current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return t;
    }
}
