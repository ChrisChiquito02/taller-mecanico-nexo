package com.sistemam01m02.identity.service;

import static com.sistemam01m02.identity.service.EmailNormalizer.normalize;
import static com.sistemam01m02.identity.service.UserViews.view;

import com.sistemam01m02.config.*;
import com.sistemam01m02.identity.dto.Contracts.*;
import com.sistemam01m02.identity.entity.*;
import com.sistemam01m02.identity.repository.*;
import com.sistemam01m02.shared.error.*;
import com.sistemam01m02.shared.persistence.UuidV7Generator;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  private final UserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder passwords;
  private final UuidV7Generator ids;
  private final AppSecurityProperties security;

  public AccountService(
      UserRepository users,
      RoleRepository roles,
      PasswordEncoder passwords,
      UuidV7Generator ids,
      AppSecurityProperties security) {
    this.users = users;
    this.roles = roles;
    this.passwords = passwords;
    this.ids = ids;
    this.security = security;
  }

  @Transactional
  public UserView register(Register input) {
    String email = normalize(input.email());
    String domain = email.substring(email.lastIndexOf('@') + 1);
    if (!security.allowedEmailDomains().contains(domain))
      throw new ApiException(400, "EMAIL_DOMAIN", "Dominio de correo no permitido");
    String hash = passwords.encode(input.password());
    if (users.findByEmailNormalized(email).isPresent()) throw new RegistrationConflict();
    AppUser user = new AppUser(ids.generate(), email, hash, input.displayName().strip());
    user.addRole(
        roles
            .findByCode("USER")
            .filter(Role::isActive)
            .orElseThrow(() -> new IllegalStateException("Falta rol USER")));
    try {
      users.saveAndFlush(user);
    } catch (org.springframework.dao.DataIntegrityViolationException error) {
      // Solo la unicidad del correo se traduce a registro genérico; otros errores conservan su
      // causa.
      Throwable cause = error;
      while (cause != null) {
        if (cause instanceof java.sql.SQLException sql
            && sql.getErrorCode() == 1062
            && sql.getMessage() != null
            && sql.getMessage().contains("uq_app_user_email")) throw new RegistrationConflict();
        cause = cause.getCause();
      }
      throw error;
    }
    return view(user);
  }

  @Transactional(readOnly = true)
  public UserView me(UUID id) {
    return view(users.findById(id).orElseThrow(ApiException::unauthorized));
  }
}
