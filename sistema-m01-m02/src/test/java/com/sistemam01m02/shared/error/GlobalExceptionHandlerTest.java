package com.sistemam01m02.shared.error;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {
  @Test
  void unexpectedFailureReturns500WithoutExceptionDetails() {
    var response =
        new GlobalExceptionHandler()
            .unexpected(new IllegalStateException("sensitive-sql-password"));
    assertEquals(500, response.getStatusCode().value());
    assertTrue(response.getBody().toString().contains("INTERNAL_ERROR"));
    assertFalse(response.getBody().toString().contains("sensitive"));
    assertFalse(response.getBody().toString().contains("IllegalStateException"));
  }
}
