package com.sistemam01m02.identity.security;

import com.sistemam01m02.shared.error.ApiErrors;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import org.springframework.web.filter.OncePerRequestFilter;

/** Límite también para JSON con transferencia chunked, antes de deserializar. */
public final class JsonBodyLimitFilter extends OncePerRequestFilter {
  private static final int MAX = 16384;

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    if (!req.getRequestURI().startsWith("/api/")
        || !(req.getMethod().equals("POST") || req.getMethod().equals("PUT"))) {
      chain.doFilter(req, res);
      return;
    }
    if (req.getContentLengthLong() > MAX) {
      ApiErrors.write(res, 413, "PAYLOAD_TOO_LARGE");
      return;
    }
    byte[] body = req.getInputStream().readNBytes(MAX + 1);
    if (body.length > MAX) {
      ApiErrors.write(res, 413, "PAYLOAD_TOO_LARGE");
      return;
    }
    chain.doFilter(
        new HttpServletRequestWrapper(req) {
          @Override
          public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
              public int read() {
                return input.read();
              }

              public int read(byte[] b, int off, int len) {
                return input.read(b, off, len);
              }

              public boolean isFinished() {
                return input.available() == 0;
              }

              public boolean isReady() {
                return true;
              }

              public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException("Solo lectura síncrona");
              }
            };
          }

          @Override
          public BufferedReader getReader() {
            return new BufferedReader(
                new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
          }
        },
        res);
  }
}
