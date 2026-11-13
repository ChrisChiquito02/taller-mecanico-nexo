package com.sistemam01m02.shared.error;

import jakarta.servlet.http.*;
import java.io.IOException;

public final class ApiErrors {
  private ApiErrors() {}

  public static void write(HttpServletResponse response, int status, String code)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response.setHeader("Cache-Control", "no-store");
    response
        .getWriter()
        .write(
            "{\"status\":"
                + status
                + ",\"code\":\""
                + code
                + "\",\"title\":\"Solicitud rechazada\"}");
  }
}
