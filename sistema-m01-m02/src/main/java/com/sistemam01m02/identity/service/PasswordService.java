package com.sistemam01m02.identity.service;

import static com.sistemam01m02.identity.service.EmailNormalizer.normalize;

import com.sistemam01m02.config.*;
import com.sistemam01m02.identity.dto.Contracts.*;
import com.sistemam01m02.identity.entity.*;
import com.sistemam01m02.identity.repository.*;
import com.sistemam01m02.identity.security.TokenService;
import com.sistemam01m02.shared.error.*;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordService {

  private final UserRepository users;
  private final TokenRepository tokens;
  private final PasswordEncoder passwords;
  private final TokenService issuer;
  private final RecoveryProperties recovery;
  private final RecoveryDelivery delivery;
  private final Clock clock;
  private final TokenLookup lookup;

  public PasswordService(
      UserRepository users,
      TokenRepository tokens,
      PasswordEncoder passwords,
      TokenService issuer,
      RecoveryProperties recovery,
      RecoveryDelivery delivery,
      Clock clock,
      TokenLookup lookup) {
    this.users = users;
    this.tokens = tokens;
    this.passwords = passwords;
    this.issuer = issuer;
    this.recovery = recovery;
    this.delivery = delivery;
    this.clock = clock;
    this.lookup = lookup;
  }

  @Transactional
  public void forgot(Forgot input) {
    if ("disabled".equals(recovery.mode())) return;
    AppUser u = users.lockByEmail(normalize(input.email())).orElse(null);
    if (u == null || (u.getStatus() != UserStatus.ACTIVE && u.getStatus() != UserStatus.LOCKED))
      return;
    tokens.revokeKind(u.getId(), AccountToken.Kind.RESET, clock.instant());
    String raw = issuer.opaque(u, AccountToken.Kind.RESET);
    // Fragmento evita que el token aparezca en registros HTTP o Referer del sitio destino.
    delivery.send(u.getEmailNormalized(), recovery.baseUrl() + "#token=" + raw);
  }

  @Transactional
  public void reset(Reset input) {
    AccountToken t = lookup.lockedToken(input.token(), AccountToken.Kind.RESET);
    AppUser u = t.getUser();
    if (!t.valid(clock.instant())
        || (u.getStatus() != UserStatus.ACTIVE && u.getStatus() != UserStatus.LOCKED))
      throw ApiException.unauthorized();
    u.changePassword(passwords.encode(input.password()), clock.instant());
    t.consume(clock.instant());
    tokens.revokeAll(u.getId(), clock.instant());
  }

  @Transactional(noRollbackFor = LoginRejected.class)
  public void changePassword(UUID id, ChangePassword input) {
    AppUser u = users.lockById(id).orElseThrow(ApiException::unauthorized);
    if (!passwords.matches(input.currentPassword(), u.getPasswordHash())) throw new LoginRejected();
    u.changePassword(passwords.encode(input.password()), clock.instant());
    tokens.revokeAll(id, clock.instant());
  }
}
