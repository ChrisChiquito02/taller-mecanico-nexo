package com.sistemam01m02.identity.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppUserTest {
  @Test
  void locksExpiresAndPreservesDisabledState() {
    AppUser u = new AppUser(UUID.randomUUID(), "test@gmail.com", "hash", "Test");
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    u.recordFailedLogin(now, 2, Duration.ofMinutes(5));
    assertFalse(u.isTemporarilyLocked(now));
    u.recordFailedLogin(now, 2, Duration.ofMinutes(5));
    assertTrue(u.isTemporarilyLocked(now));
    u.unlockIfExpired(now.plusSeconds(300));
    assertEquals(UserStatus.ACTIVE, u.getStatus());
    assertEquals(0, u.getFailedLoginCount());
    u.disable(now);
    u.recordFailedLogin(now, 1, Duration.ofMinutes(5));
    u.changePassword("other", now);
    assertEquals(UserStatus.DISABLED, u.getStatus());
    assertEquals(2, u.getSessionVersion());
  }

  @Test
  void successfulLoginClearsCounters() {
    AppUser u = new AppUser(UUID.randomUUID(), "x@gmail.com", "h", "X");
    Instant now = Instant.now();
    u.recordFailedLogin(now, 3, Duration.ofMinutes(5));
    u.recordSuccessfulLogin(now);
    assertEquals(0, u.getFailedLoginCount());
    assertEquals(now, u.getLastLoginAt());
  }
}
