package com.sistemam01m02.identity.repository;

import com.sistemam01m02.identity.entity.AccountToken;
import com.sistemam01m02.identity.entity.AccountToken.Kind;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TokenRepository extends JpaRepository<AccountToken, UUID> {
  Optional<AccountToken> findByTokenHashAndKind(String hash, Kind kind);

  @Query("select t.user.id from AccountToken t where t.tokenHash=:hash and t.kind=:kind")
  Optional<UUID> findOwner(String hash, Kind kind);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from AccountToken t where t.tokenHash=:hash and t.kind=:kind")
  Optional<AccountToken> lockByHash(String hash, Kind kind);

  @Modifying
  @Query(
      "update AccountToken t set t.consumedAt=:now where t.user.id=:userId and t.consumedAt is"
          + " null")
  int revokeAll(UUID userId, Instant now);

  @Modifying
  @Query(
      "update AccountToken t set t.consumedAt=:now where t.user.id=:userId and t.kind=:kind and"
          + " t.consumedAt is null")
  int revokeKind(UUID userId, Kind kind, Instant now);
}
