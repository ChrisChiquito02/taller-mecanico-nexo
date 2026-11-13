package com.sistemam01m02.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthControllerTest {
  @Test
  void databaseFailureReturns503WithoutConnectionDetails() {
    var jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject("SELECT 1", Integer.class))
        .thenThrow(new DataAccessResourceFailureException("sensitive-jdbc-details"));
    var response = new HealthController(jdbc).health();
    assertEquals(503, response.getStatusCode().value());
    assertEquals(java.util.Map.of("status", "DOWN"), response.getBody());
  }
}
