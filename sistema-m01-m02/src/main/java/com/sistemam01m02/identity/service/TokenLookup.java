package com.sistemam01m02.identity.service;

import com.sistemam01m02.config.*;
import com.sistemam01m02.identity.dto.Contracts.*;
import com.sistemam01m02.identity.entity.*;
import com.sistemam01m02.identity.repository.*;
import com.sistemam01m02.identity.security.TokenService;
import com.sistemam01m02.shared.error.*;
import java.time.*;
import java.util.*;

@org.springframework.stereotype.Component
public class TokenLookup {
  private final UserRepository users;
  private final TokenRepository tokens;

  public TokenLookup(UserRepository users, TokenRepository tokens) {
    this.users = users;
    this.tokens = tokens;
  }

  public AccountToken lockedToken(String raw, AccountToken.Kind kind) {
    if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) throw new LoginRejected();
    String hash = TokenService.hash(raw);
    UUID owner = tokens.findOwner(hash, kind).orElseThrow(LoginRejected::new);
    // Orden único: usuario antes del token, compartido por refresh/reset/logout.
    users.lockById(owner).orElseThrow(LoginRejected::new);
    return tokens.lockByHash(hash, kind).orElseThrow(LoginRejected::new);
  }
}
