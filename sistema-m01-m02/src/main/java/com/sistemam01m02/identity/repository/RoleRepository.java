package com.sistemam01m02.identity.repository;

import com.sistemam01m02.identity.entity.Role;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByCode(String code);
}
