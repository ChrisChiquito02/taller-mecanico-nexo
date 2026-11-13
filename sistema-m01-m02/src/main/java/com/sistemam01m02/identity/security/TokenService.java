package com.sistemam01m02.identity.security;

import com.sistemam01m02.config.AppSecurityProperties;
import com.sistemam01m02.identity.entity.*;
import com.sistemam01m02.identity.repository.TokenRepository;
import com.sistemam01m02.shared.persistence.UuidV7Generator;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private final TokenRepository tokens;
  private final UuidV7Generator ids;
  private final AppSecurityProperties p;
  private final JwtEncoder encoder;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public TokenService(
      TokenRepository t, UuidV7Generator i, AppSecurityProperties p, JwtEncoder e, Clock c) {
    tokens = t;
    ids = i;
    this.p = p;
    encoder = e;
    clock = c;
  }

  public String opaque(AppUser user, AccountToken.Kind kind) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Duration ttl = kind == AccountToken.Kind.REFRESH ? p.refreshTokenTtl() : p.passwordResetTtl();
    tokens.save(new AccountToken(ids.generate(), user, hash(raw), kind, clock.instant().plus(ttl)));
    return raw;
  }

  public String access(AppUser user) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(p.issuer())
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(p.accessTokenTtl()))
            .id(ids.generate().toString())
            .claim("sv", user.getSessionVersion())
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  public static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
