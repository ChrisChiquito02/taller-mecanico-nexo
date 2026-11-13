package com.sistemam01m02.shared.error;

/** Conflicto interno; la API pública no revela si el correo ya existe. */
public final class RegistrationConflict extends ApiException {
  public RegistrationConflict() {
    super(409, "REGISTRATION_CONFLICT", "Registro no disponible");
  }
}
