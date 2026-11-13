package com.sistemam01m02.identity.service;

import static com.sistemam01m02.identity.service.EmailNormalizer.normalize;

import com.sistemam01m02.config.*;
import com.sistemam01m02.identity.dto.Contracts.*;
import com.sistemam01m02.identity.dto.SessionTokens;
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
public class SessionService {

  private final UserRepository users;
  private final TokenRepository tokens;
  private final PasswordEncoder passwords;
  private final TokenService issuer;
  private final AppSecurityProperties security;
  private final Clock clock;
  private final TokenLookup lookup;
  private final String dummyHash;

  public SessionService(
      UserRepository users,
      TokenRepository tokens,
      PasswordEncoder passwords,
      TokenService issuer,
      AppSecurityProperties security,
      Clock clock,
      TokenLookup lookup) {
    this.users = users;
    this.tokens = tokens;
    this.passwords = passwords;
    this.issuer = issuer;
    this.security = security;
    this.clock = clock;
    this.lookup = lookup;
    dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  @Transactional(noRollbackFor = LoginRejected.class)
  public SessionTokens login(Login input) {
    AppUser u = users.lockByEmail(normalize(input.email())).orElse(null);
    if (u == null) {
      passwords.matches(input.password(), dummyHash);
      throw new LoginRejected();
    }
    Instant now = clock.instant();
    u.unlockIfExpired(now);
    boolean matches = passwords.matches(input.password(), u.getPasswordHash());
    if (u.getStatus() != UserStatus.ACTIVE) throw new LoginRejected();
    if (!matches) {
      u.recordFailedLogin(now, security.maxFailedAttempts(), security.lockDuration());
      throw new LoginRejected();
    }
    u.recordSuccessfulLogin(now);
    return session(u);
  }

  @Transactional(noRollbackFor = LoginRejected.class)
  public SessionTokens refresh(String raw) {
    AccountToken t = lookup.lockedToken(raw, AccountToken.Kind.REFRESH);
    AppUser u = t.getUser();
    if (t.expired(clock.instant())) throw new LoginRejected();
    if (t.consumed()) {
      u.revokeSessions();
      tokens.revokeAll(u.getId(), clock.instant());
      throw new LoginRejected();
    }
    u.unlockIfExpired(clock.instant());
    if (!t.valid(clock.instant()) || u.getStatus() != UserStatus.ACTIVE) throw new LoginRejected();
    t.consume(clock.instant());
    return session(u);
  }

  @Transactional
  public void logout(String raw) {
    if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) return;
    Optional<UUID> found = tokens.findOwner(TokenService.hash(raw), AccountToken.Kind.REFRESH);
    if (found.isEmpty()) return;
    AppUser u = users.lockById(found.get()).orElseThrow();
    AccountToken token =
        tokens.lockByHash(TokenService.hash(raw), AccountToken.Kind.REFRESH).orElse(null);
    if (token == null || token.expired(clock.instant())) return;
    u.revokeSessions();
    tokens.revokeAll(u.getId(), clock.instant());
  }

  private SessionTokens session(AppUser u) {
    return new SessionTokens(issuer.access(u), issuer.opaque(u, AccountToken.Kind.REFRESH));
  }
}
