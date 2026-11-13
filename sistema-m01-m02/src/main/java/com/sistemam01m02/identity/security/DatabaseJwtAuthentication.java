package com.sistemam01m02.identity.security;

import com.sistemam01m02.identity.entity.*;
import com.sistemam01m02.identity.repository.UserRepository;
import java.time.Clock;
import java.util.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseJwtAuthentication implements Converter<Jwt, AbstractAuthenticationToken> {
  private final UserRepository users;
  private final Clock clock;

  public DatabaseJwtAuthentication(UserRepository users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public AbstractAuthenticationToken convert(Jwt jwt) {
    try {
      AppUser u =
          users
              .findById(UUID.fromString(jwt.getSubject()))
              .orElseThrow(() -> new InvalidBearerTokenException("Sesión inválida"));
      Number version = jwt.getClaim("sv");
      if (version == null
          || version.longValue() != u.getSessionVersion()
          || (u.getStatus() != UserStatus.ACTIVE && !(u.hasExpiredLock(clock.instant()))))
        throw new InvalidBearerTokenException("Sesión inválida");
      Set<SimpleGrantedAuthority> authorities = new HashSet<>();
      for (Role role : u.getRoles())
        if (role.isActive()) {
          authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
          for (Permission p : role.getPermissions())
            authorities.add(new SimpleGrantedAuthority(p.getCode()));
        }
      return new JwtAuthenticationToken(jwt, authorities, u.getId().toString());
    } catch (IllegalArgumentException e) {
      throw new InvalidBearerTokenException("Sesión inválida");
    }
  }
}
