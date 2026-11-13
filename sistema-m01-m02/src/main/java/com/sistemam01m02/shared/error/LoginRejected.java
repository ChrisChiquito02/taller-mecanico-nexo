package com.sistemam01m02.shared.error;

/** El rechazo se confirma en la transacción para conservar contadores y revocaciones. */
public final class LoginRejected extends ApiException {
  public LoginRejected() {
    super(401, "INVALID_CREDENTIALS", "Credenciales o sesión inválidas");
  }
}
