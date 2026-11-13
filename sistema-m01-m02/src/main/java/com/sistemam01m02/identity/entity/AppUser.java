package com.sistemam01m02.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "app_user")
public class AppUser {
  @Id
  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(name = "session_version", nullable = false)
  private long sessionVersion;

  @Column(name = "email_normalized", nullable = false, length = 254, unique = true)
  private String emailNormalized;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 120)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserStatus status;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "failed_login_count", nullable = false)
  private int failedLoginCount;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "password_changed_at", nullable = false)
  private Instant passwordChangedAt;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deactivated_at")
  private Instant deactivatedAt;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new LinkedHashSet<>();

  protected AppUser() {}

  public AppUser(UUID id, String emailNormalized, String passwordHash, String displayName) {
    Instant now = Instant.now();
    this.id = id;
    this.emailNormalized = emailNormalized;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.status = UserStatus.ACTIVE;
    this.passwordChangedAt = now;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getEmailNormalized() {
    return emailNormalized;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public UserStatus getStatus() {
    return status;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public int getFailedLoginCount() {
    return failedLoginCount;
  }

  public Instant getLockedUntil() {
    return lockedUntil;
  }

  public Instant getPasswordChangedAt() {
    return passwordChangedAt;
  }

  public boolean hasExpiredLock(Instant now) {
    return status == UserStatus.LOCKED && lockedUntil != null && !lockedUntil.isAfter(now);
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public Set<Role> getRoles() {
    return roles;
  }

  public long getSessionVersion() {
    return sessionVersion;
  }

  public void revokeSessions() {
    sessionVersion++;
  }

  public void replaceRoles(Set<Role> replacement) {
    roles.clear();
    roles.addAll(replacement);
  }

  public void disable(Instant now) {
    status = UserStatus.DISABLED;
    deactivatedAt = now;
    updatedAt = now;
    revokeSessions();
  }

  public void addRole(Role role) {
    roles.add(role);
  }

  public void recordSuccessfulLogin(Instant now) {
    failedLoginCount = 0;
    lockedUntil = null;
    if (status == UserStatus.LOCKED) status = UserStatus.ACTIVE;
    lastLoginAt = now;
    updatedAt = now;
  }

  public void recordFailedLogin(Instant now, int maximum, java.time.Duration duration) {
    if (status != UserStatus.ACTIVE && status != UserStatus.LOCKED) return;
    failedLoginCount++;
    if (failedLoginCount >= maximum) {
      status = UserStatus.LOCKED;
      lockedUntil = now.plus(duration);
    }
    updatedAt = now;
  }

  public boolean isTemporarilyLocked(Instant now) {
    return status == UserStatus.LOCKED && lockedUntil != null && lockedUntil.isAfter(now);
  }

  public void unlockIfExpired(Instant now) {
    if (status == UserStatus.LOCKED && lockedUntil != null && !lockedUntil.isAfter(now)) {
      status = UserStatus.ACTIVE;
      failedLoginCount = 0;
      lockedUntil = null;
      updatedAt = now;
    }
  }

  public void changePassword(String newHash, Instant now) {
    revokeSessions();
    passwordHash = newHash;
    passwordChangedAt = now;
    updatedAt = now;
    failedLoginCount = 0;
    lockedUntil = null;
    if (status == UserStatus.LOCKED) status = UserStatus.ACTIVE;
  }
}
