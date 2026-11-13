package com.sistemam01m02.identity.api;

import com.sistemam01m02.identity.dto.Contracts.*;
import com.sistemam01m02.identity.service.IdentityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {
  private final IdentityService service;

  public UserController(IdentityService s) {
    service = s;
  }

  @GetMapping("/users/me")
  @PreAuthorize("hasAuthority('profile:read')")
  public UserView me(Authentication a) {
    return service.me(UUID.fromString(a.getName()));
  }

  @GetMapping("/admin/users")
  @PreAuthorize("hasAuthority('users:read')")
  public List<UserView> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(page, size);
  }

  @PutMapping(value = "/admin/users/{id}/roles", consumes = "application/json")
  @PreAuthorize("hasAuthority('users:write')")
  public UserView roles(@PathVariable UUID id, @Valid @RequestBody Roles body) {
    return service.assignRoles(id, body.roles());
  }

  @DeleteMapping("/admin/users/{id}")
  @PreAuthorize("hasAuthority('users:write')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disable(@PathVariable UUID id, Authentication a) {
    service.disable(UUID.fromString(a.getName()), id);
  }
}
