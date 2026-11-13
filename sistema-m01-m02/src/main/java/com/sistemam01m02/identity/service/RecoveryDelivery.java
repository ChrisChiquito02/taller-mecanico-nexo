package com.sistemam01m02.identity.service;

public interface RecoveryDelivery {
  void send(String recipient, String recoveryUrl);
}
