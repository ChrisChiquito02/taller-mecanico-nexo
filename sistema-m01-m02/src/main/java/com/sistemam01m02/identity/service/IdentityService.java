package com.sistemam01m02.identity.service;

import com.sistemam01m02.identity.dto.Contracts.*;
import com.sistemam01m02.identity.dto.SessionTokens;
import com.sistemam01m02.identity.entity.AppUser;
import java.util.*;
import org.springframework.stereotype.Service;

/** Fachada compatible: delega; las transacciones pertenecen a cada servicio de caso de uso. */
@Service
public class IdentityService {
  public record Session(String access, String refresh) {}

  private final AccountService accounts;
  private final SessionService sessions;
  private final PasswordService passwords;
  private final UserAdministrationService administration;

  public IdentityService(
      AccountService accounts,
      SessionService sessions,
      PasswordService passwords,
      UserAdministrationService administration) {
    this.accounts = accounts;
    this.sessions = sessions;
    this.passwords = passwords;
    this.administration = administration;
  }

  public UserView register(Register input) {
    return accounts.register(input);
  }

  public Session login(Login input) {
    return session(sessions.login(input));
  }

  public Session refresh(String raw) {
    return session(sessions.refresh(raw));
  }

  public void logout(String raw) {
    sessions.logout(raw);
  }

  public void forgot(Forgot input) {
    passwords.forgot(input);
  }

  public void reset(Reset input) {
    passwords.reset(input);
  }

  public void changePassword(UUID id, ChangePassword input) {
    passwords.changePassword(id, input);
  }

  public UserView me(UUID id) {
    return accounts.me(id);
  }

  public List<UserView> list(int page, int size) {
    return administration.list(page, size);
  }

  public UserView assignRoles(UUID id, Set<String> codes) {
    return administration.assignRoles(id, codes);
  }

  public void disable(UUID actor, UUID target) {
    administration.disable(actor, target);
  }

  public static String normalize(String email) {
    return EmailNormalizer.normalize(email);
  }

  public static UserView view(AppUser user) {
    return UserViews.view(user);
  }

  private static Session session(SessionTokens value) {
    return new Session(value.access(), value.refresh());
  }
}
