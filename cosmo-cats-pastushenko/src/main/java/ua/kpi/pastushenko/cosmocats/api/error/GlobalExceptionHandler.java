package ua.kpi.pastushenko.cosmocats.api.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ua.kpi.pastushenko.cosmocats.application.exception.ResourceNotFoundException;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException exception, WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleInvalidArgument(IllegalArgumentException exception, WebRequest request) {
        String detail = exception.getMessage() != null ? exception.getMessage() : "Invalid request data";
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
        return handleExceptionInternal(
                exception, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), validationMessage(error.getDefaultMessage())))
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (exception.isForReturnValue()) {
            return handleExceptionInternal(exception, null, headers, status, request);
        }
        var problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        var errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ValidationError(
                                error instanceof FieldError fieldError
                                        ? fieldError.getField()
                                        : parameterName(result.getMethodParameter()),
                                validationMessage(error.getDefaultMessage()))))
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(
                status, "Request body must be valid JSON matching the product schema"
        );
        if (exception.getCause() instanceof UnrecognizedPropertyException unknownField) {
            problem.setProperty("errors", List.of(new ValidationError(unknownField.getPropertyName(), "Unknown field")));
        } else if (exception.getCause() instanceof JsonMappingException mapping) {
            String field = mapping.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            if (!field.isEmpty()) {
                problem.setProperty("errors", List.of(new ValidationError(field, "Invalid JSON value or type")));
            }
        }
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem;
        if (status.is5xxServerError()) {
            logger.error("Unexpected error while processing a request", exception);
            problem = ProblemDetail.forStatusAndDetail(status, "An unexpected error occurred");
        } else if (body instanceof ProblemDetail detail) {
            problem = detail;
        } else if (exception instanceof ErrorResponse errorResponse) {
            problem = errorResponse.getBody();
        } else {
            problem = ProblemDetail.forStatusAndDetail(status, "Request could not be processed");
        }
        problem.setStatus(status.value());
        if (problem.getDetail() == null) {
            problem.setDetail("Request could not be processed");
        }
        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        var responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return super.handleExceptionInternal(exception, problem, responseHeaders, status, request);
    }

    private String validationMessage(String message) {
        return message != null ? message : "Invalid value";
    }

    private String parameterName(MethodParameter parameter) {
        var query = parameter.getParameterAnnotation(RequestParam.class);
        if (query != null) {
            if (!query.name().isEmpty()) {
                return query.name();
            }
            if (!query.value().isEmpty()) {
                return query.value();
            }
        }
        var path = parameter.getParameterAnnotation(PathVariable.class);
        if (path != null) {
            if (!path.name().isEmpty()) {
                return path.name();
            }
            if (!path.value().isEmpty()) {
                return path.value();
            }
        }
        return parameter.getParameterName() != null ? parameter.getParameterName() : "arg" + parameter.getParameterIndex();
    }
}
