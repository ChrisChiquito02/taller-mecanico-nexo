package com.sistemam01m02.identity.api;

import com.sistemam01m02.config.AppSecurityProperties;
import com.sistemam01m02.identity.dto.Contracts.*;
import com.sistemam01m02.identity.service.IdentityService;
import com.sistemam01m02.identity.service.RecoveryRequests;
import com.sistemam01m02.shared.error.RegistrationConflict;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final IdentityService service;
  private final AppSecurityProperties p;
  private final RecoveryRequests recovery;

  public AuthController(IdentityService s, AppSecurityProperties p, RecoveryRequests recovery) {
    service = s;
    this.p = p;
    this.recovery = recovery;
  }

  @PostMapping(value = "/register", consumes = "application/json")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Message register(@Valid @RequestBody Register input) {
    try {
      service.register(input);
    } catch (RegistrationConflict ignored) {
    }
    return new Message(
        "Solicitud de registro procesada. Puedes intentar iniciar sesión o recuperar el acceso");
  }

  @PostMapping(value = "/login", consumes = "application/json")
  public Access login(@Valid @RequestBody Login input, HttpServletResponse response) {
    return issue(service.login(input), response);
  }

  @PostMapping(value = "/refresh", consumes = "application/json")
  public Access refresh(
      @RequestBody Map<String, Object> body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return issue(service.refresh(cookie(request)), response);
  }

  @PostMapping(value = "/logout", consumes = "application/json")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @RequestBody Map<String, Object> body,
      HttpServletRequest request,
      HttpServletResponse response) {
    service.logout(cookie(request));
    setCookie(response, "", 0);
  }

  @PostMapping(value = "/forgot-password", consumes = "application/json")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Message forgot(@Valid @RequestBody Forgot input) {
    recovery.submit(input);
    return new Message("Si la cuenta es elegible, recibirás instrucciones de recuperación");
  }

  @PostMapping(value = "/reset-password", consumes = "application/json")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reset(@Valid @RequestBody Reset input) {
    service.reset(input);
  }

  @PostMapping(value = "/change-password", consumes = "application/json")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('profile:read')")
  public void change(
      @Valid @RequestBody ChangePassword input, Authentication auth, HttpServletResponse response) {
    service.changePassword(UUID.fromString(auth.getName()), input);
    setCookie(response, "", 0);
  }

  private Access issue(IdentityService.Session s, HttpServletResponse res) {
    setCookie(res, s.refresh(), p.refreshTokenTtl().toSeconds());
    return new Access(s.access(), "Bearer", p.accessTokenTtl().toSeconds());
  }

  private void setCookie(HttpServletResponse res, String value, long seconds) {
    res.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from(p.refreshCookieName(), value)
            .httpOnly(true)
            .secure(p.refreshCookieSecure())
            .sameSite(p.refreshCookieSameSite())
            .path("/api/auth")
            .maxAge(seconds)
            .build()
            .toString());
  }

  private String cookie(HttpServletRequest req) {
    if (req.getCookies() != null)
      for (Cookie c : req.getCookies())
        if (p.refreshCookieName().equals(c.getName())) return c.getValue();
    return null;
  }
}
