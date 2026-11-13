package com.sistemam01m02.identity.dto;

import jakarta.validation.constraints.*;
import java.util.*;

public final class Contracts {
  private Contracts() {}

  public record Register(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 128) String password,
      @NotBlank @Size(max = 120) String displayName) {}

  public record Login(
      @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 128) String password) {}

  public record Forgot(@NotBlank @Email @Size(max = 254) String email) {}

  public record Reset(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
      @NotBlank @Size(min = 12, max = 128) String password) {}

  public record ChangePassword(
      @NotBlank @Size(max = 128) String currentPassword,
      @NotBlank @Size(min = 12, max = 128) String password) {}

  public record Roles(
      @NotEmpty @Size(max = 20) Set<@Pattern(regexp = "[A-Z_]{1,50}") String> roles) {}

  public record UserView(
      UUID id,
      String email,
      String displayName,
      String status,
      Set<String> roles,
      Set<String> permissions) {}

  public record Access(String accessToken, String tokenType, long expiresIn) {}

  public record Message(String message) {}
}
