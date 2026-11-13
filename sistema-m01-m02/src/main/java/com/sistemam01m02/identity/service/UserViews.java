package com.sistemam01m02.identity.service;

import com.sistemam01m02.config.*;
import com.sistemam01m02.identity.dto.Contracts.*;
import com.sistemam01m02.identity.entity.*;
import com.sistemam01m02.identity.repository.*;
import com.sistemam01m02.shared.error.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public final class UserViews {
  private UserViews() {}

  public static UserView view(AppUser u) {
    Set<Role> active = u.getRoles().stream().filter(Role::isActive).collect(Collectors.toSet());
    return new UserView(
        u.getId(),
        u.getEmailNormalized(),
        u.getDisplayName(),
        u.getStatus().name(),
        active.stream().map(Role::getCode).collect(Collectors.toSet()),
        active.stream()
            .flatMap(r -> r.getPermissions().stream())
            .map(Permission::getCode)
            .collect(Collectors.toSet()));
  }
}
