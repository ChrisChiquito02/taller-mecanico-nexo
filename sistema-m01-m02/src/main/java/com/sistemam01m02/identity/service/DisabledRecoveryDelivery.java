package com.sistemam01m02.identity.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.recovery", name = "mode", havingValue = "disabled")
public class DisabledRecoveryDelivery implements RecoveryDelivery {
  @Override
  public void send(String recipient, String recoveryUrl) {
    throw new IllegalStateException("La recuperación por correo está desactivada");
  }
}
