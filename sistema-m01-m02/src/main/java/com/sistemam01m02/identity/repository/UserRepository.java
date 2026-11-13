package com.sistemam01m02.identity.repository;

import com.sistemam01m02.identity.entity.AppUser;
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

  @Query(value = "select * from app_user where email_normalized=:email for update", nativeQuery = true)
  Optional<AppUser> lockByEmail(String email);

  @Query(value = "select * from app_user where id=:id for update", nativeQuery = true)
  Optional<AppUser> lockById(UUID id);
}
