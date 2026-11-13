package com.sistemam01m02.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security")
public record AppSecurityProperties(
    @NotBlank String jwtSigningKey,
    @NotNull Duration accessTokenTtl,
    @NotNull Duration refreshTokenTtl,
    @NotNull Duration passwordResetTtl,
    @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String refreshCookieName,
    boolean refreshCookieSecure,
    @Pattern(regexp = "Strict|Lax|None") String refreshCookieSameSite,
    @NotEmpty Set<@NotBlank String> allowedOrigins,
    @Min(1) @Max(100) int maxFailedAttempts,
    @NotNull Duration lockDuration,
    @NotEmpty Set<@NotBlank String> allowedEmailDomains,
    @NotBlank String issuer,
    @Min(1) @Max(10000) int requestsPerMinute) {
  @AssertTrue(
      message = "Los tiempos deben ser positivos; access-token-ttl no puede exceder 15 minutos")
  public boolean isDurationsValid() {
    return positive(accessTokenTtl)
        && accessTokenTtl.compareTo(Duration.ofMinutes(15)) <= 0
        && positive(refreshTokenTtl)
        && positive(passwordResetTtl)
        && positive(lockDuration);
  }

  private static boolean positive(Duration d) {
    return d != null && !d.isNegative() && !d.isZero();
  }
}
