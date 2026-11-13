package com.sistemam01m02.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "account_token")
public class AccountToken {
  public enum Kind {
    REFRESH,
    RESET
  }

  @Id
  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private AppUser user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Kind kind;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  protected AccountToken() {}

  public AccountToken(UUID id, AppUser user, String hash, Kind kind, Instant expiry) {
    this.id = id;
    this.user = user;
    this.tokenHash = hash;
    this.kind = kind;
    this.expiresAt = expiry;
  }

  public AppUser getUser() {
    return user;
  }

  public boolean valid(Instant now) {
    return consumedAt == null && expiresAt.isAfter(now);
  }

  public boolean expired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  public boolean consumed() {
    return consumedAt != null;
  }

  public void consume(Instant now) {
    consumedAt = now;
  }
}
