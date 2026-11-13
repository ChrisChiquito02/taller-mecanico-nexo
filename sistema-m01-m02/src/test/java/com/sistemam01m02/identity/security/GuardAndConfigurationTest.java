package com.sistemam01m02.identity.security;

import static org.junit.jupiter.api.Assertions.*;

import com.sistemam01m02.config.*;
import jakarta.validation.Validation;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class GuardAndConfigurationTest {
  AppSecurityProperties props(String key, Duration ttl, int count, boolean secure) {
    return new AppSecurityProperties(
        key,
        ttl,
        Duration.ofDays(7),
        Duration.ofMinutes(15),
        "refresh_token",
        secure,
        "Strict",
        Set.of("https://example.test"),
        5,
        Duration.ofMinutes(15),
        Set.of("gmail.com"),
        "test",
        count);
  }

  String key = "dGVzdC1vbmx5LXN5bnRoZXRpYy1zZWNyZXQtMzJieXRlcw==";

  @Test
  void rateLimitsByFingerprint() {
    var guard = new AuthRequestGuard(props(key, Duration.ofMinutes(15), 2, true));
    assertTrue(guard.allow("127.0.0.1"));
    assertTrue(guard.allow("127.0.0.1"));
    assertFalse(guard.allow("127.0.0.1"));
    assertTrue(guard.allow("127.0.0.2"));
  }

  @Test
  void rejectsInvalidDurations() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertFalse(factory.getValidator().validate(props(key, Duration.ZERO, 2, true)).isEmpty());
      assertFalse(
          factory.getValidator().validate(props(key, Duration.ofHours(1), 2, true)).isEmpty());
    }
  }

  @Test
  void rejectsWeakKeyAndUnsafeProductionSettings() {
    var config = new SecurityConfig();
    var recovery =
        new RecoveryProperties("smtp", "https://example.test/reset", "noreply@example.test");
    assertThrows(
        IllegalStateException.class,
        () ->
            config.signingKey(
                props("YWJj", Duration.ofMinutes(15), 2, true), recovery, new MockEnvironment()));
    assertThrows(
        IllegalStateException.class,
        () ->
            config.signingKey(
                props(key, Duration.ofMinutes(15), 2, false), recovery, new MockEnvironment()));
    assertThrows(
        IllegalStateException.class,
        () ->
            config.signingKey(
                props(key, Duration.ofMinutes(15), 2, true),
                new RecoveryProperties(
                    "development", "https://example.test/reset", "noreply@example.test"),
                new MockEnvironment()));
    assertNotNull(
        config.signingKey(
            props(key, Duration.ofMinutes(15), 2, true), recovery, new MockEnvironment()));
  }

  @Test
  void filterReturns429AndDoesNotTrustForwardedAddress() throws Exception {
    var guard = new AuthRequestGuard(props(key, Duration.ofMinutes(15), 1, true));
    for (int i = 0; i < 2; i++) {
      var req = new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/auth/login");
      req.setRemoteAddr("127.0.0.1");
      req.addHeader("X-Forwarded-For", "10.0.0." + i);
      var response = new org.springframework.mock.web.MockHttpServletResponse();
      guard.doFilter(req, response, new org.springframework.mock.web.MockFilterChain());
      assertEquals(i == 0 ? 200 : 429, response.getStatus());
      if (i == 1) assertEquals("60", response.getHeader("Retry-After"));
    }
  }
}
