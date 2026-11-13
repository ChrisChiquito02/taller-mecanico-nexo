package com.sistemam01m02.identity.security;

import com.github.benmanes.caffeine.cache.*;
import com.sistemam01m02.config.AppSecurityProperties;
import com.sistemam01m02.shared.error.ApiErrors;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRequestGuard extends OncePerRequestFilter {
  private final AppSecurityProperties p;
  private final Cache<String, AtomicInteger> attempts =
      Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(Duration.ofMinutes(1)).build();

  public AuthRequestGuard(AppSecurityProperties p) {
    this.p = p;
  }

  public boolean allow(String address) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(p.jwtSigningKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String fingerprint =
          HexFormat.of().formatHex(mac.doFinal(address.getBytes(StandardCharsets.UTF_8)));
      return attempts.get(fingerprint, k -> new AtomicInteger()).incrementAndGet()
          <= p.requestsPerMinute();
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    res.setHeader("Cache-Control", "no-store");
    if (req.getRequestURI().startsWith("/api/auth/") && !"OPTIONS".equals(req.getMethod())) {
      if (!allow(req.getRemoteAddr())) {
        res.setHeader("Retry-After", "60");
        ApiErrors.write(res, 429, "RATE_LIMITED");
        return;
      }
      if (Set.of("/api/auth/refresh", "/api/auth/logout").contains(req.getRequestURI())) {
        // La autenticación por cookie exige origen exacto y JSON; no se confía en Referer ni
        // X-Forwarded-For.
        if (!p.allowedOrigins().contains(req.getHeader("Origin"))) {
          ApiErrors.write(res, 403, "ORIGIN_REJECTED");
          return;
        }
      }
    }
    chain.doFilter(req, res);
  }
}
