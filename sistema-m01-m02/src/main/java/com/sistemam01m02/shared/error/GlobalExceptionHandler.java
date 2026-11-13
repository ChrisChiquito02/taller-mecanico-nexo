package com.sistemam01m02.shared.error;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  ResponseEntity<?> api(ApiException e) {
    return error(e.status(), e.code(), e.getMessage());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class,
    HandlerMethodValidationException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> validation(Exception e) {
    return error(400, "VALIDATION_ERROR", "Datos de entrada inválidos");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict(Exception e) {
    return error(409, "CONFLICT", "La operación entra en conflicto con los datos existentes");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<?> forbidden(Exception e) {
    return error(403, "FORBIDDEN", "Permiso insuficiente");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<?> missing(Exception e) {
    return error(404, "NOT_FOUND", "Recurso no encontrado");
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<?> method(Exception e) {
    return error(405, "METHOD_NOT_ALLOWED", "Método no permitido");
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  ResponseEntity<?> media(Exception e) {
    return error(415, "UNSUPPORTED_MEDIA_TYPE", "Se requiere JSON");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception e) {
    // No se registran mensajes, argumentos ni cuerpos que puedan contener secretos.
    log.error("Fallo interno: {}", e.getClass().getSimpleName());
    return error(500, "INTERNAL_ERROR", "No fue posible completar la operación");
  }

  private ResponseEntity<?> error(int status, String code, String title) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .cacheControl(CacheControl.noStore())
        .body(Map.of("status", status, "code", code, "title", title));
  }
}
