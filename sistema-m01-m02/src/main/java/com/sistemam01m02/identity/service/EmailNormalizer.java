package com.sistemam01m02.identity.service;

import java.util.Locale;

public final class EmailNormalizer {
  private EmailNormalizer() {}

  public static String normalize(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }
}
