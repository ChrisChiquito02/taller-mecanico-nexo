package com.sistemam01m02.identity.repository;

import com.sistemam01m02.identity.entity.AppUser;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
  @Override
  @EntityGraph(attributePaths = {"roles", "roles.permissions"})
  Optional<AppUser> findById(UUID id);

  @Query(
      "select distinct u from AppUser u left join fetch u.roles r left join fetch r.permissions"
          + " where u.id in :ids")
  List<AppUser> loadAuthorities(Collection<UUID> ids);

  Optional<AppUser> findByEmailNormalized(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from AppUser u where u.emailNormalized=:email")
  Optional<AppUser> lockByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from AppUser u where u.id=:id")
  Optional<AppUser> lockById(UUID id);
}
