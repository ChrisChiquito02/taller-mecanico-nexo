package com.sistemam01m02.identity.service;

import com.sistemam01m02.config.RecoveryProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpRecoveryDelivery implements RecoveryDelivery {
  private final JavaMailSender mail;
  private final RecoveryProperties p;

  public SmtpRecoveryDelivery(JavaMailSender mail, RecoveryProperties p) {
    this.mail = mail;
    this.p = p;
  }

  public void send(String recipient, String url) {
    SimpleMailMessage m = new SimpleMailMessage();
    m.setFrom(p.from());
    m.setTo(recipient);
    m.setSubject("Recuperación de cuenta");
    m.setText(
        "Para cambiar tu contraseña abre este enlace de un solo uso:\n"
            + url
            + "\nSi no lo solicitaste, ignora este mensaje.");
    mail.send(m);
  }
}
