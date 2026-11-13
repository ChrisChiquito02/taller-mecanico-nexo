package com.sistemam01m02.identity.service;

import static com.sistemam01m02.identity.service.UserViews.view;

import com.sistemam01m02.config.*;
import com.sistemam01m02.identity.dto.Contracts.*;
import com.sistemam01m02.identity.entity.*;
import com.sistemam01m02.identity.repository.*;
import com.sistemam01m02.shared.error.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdministrationService {

  private final UserRepository users;
  private final RoleRepository roles;
  private final TokenRepository tokens;
  private final Clock clock;

  public UserAdministrationService(
      UserRepository users, RoleRepository roles, TokenRepository tokens, Clock clock) {
    this.users = users;
    this.roles = roles;
    this.tokens = tokens;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<UserView> list(int page, int size) {

    var pageOfUsers =
        users.findAll(PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id")));
    if (pageOfUsers.isEmpty()) return List.of();
    var loaded = users.loadAuthorities(pageOfUsers.stream().map(AppUser::getId).toList());
    var byId = loaded.stream().collect(Collectors.toMap(AppUser::getId, u -> u));
    return pageOfUsers.stream().map(u -> view(byId.get(u.getId()))).toList();
  }

  @Transactional
  public UserView assignRoles(UUID id, Set<String> codes) {
    AppUser u =
        users
            .lockById(id)
            .orElseThrow(() -> new ApiException(404, "NOT_FOUND", "Usuario no encontrado"));
    Set<Role> selected = new HashSet<>();
    for (String code : codes)
      selected.add(
          roles
              .findByCode(code)
              .filter(Role::isActive)
              .orElseThrow(
                  () -> new ApiException(400, "INVALID_ROLE", "Rol inexistente o inactivo")));
    u.replaceRoles(selected);
    u.revokeSessions();
    tokens.revokeAll(id, clock.instant());
    return view(u);
  }

  @Transactional
  public void disable(UUID actor, UUID target) {
    if (actor.equals(target))
      throw new ApiException(409, "SELF_DISABLE", "No se permite desactivar la propia cuenta");
    AppUser u =
        users
            .lockById(target)
            .orElseThrow(() -> new ApiException(404, "NOT_FOUND", "Usuario no encontrado"));
    u.disable(clock.instant());
    tokens.revokeAll(target, clock.instant());
  }
}
