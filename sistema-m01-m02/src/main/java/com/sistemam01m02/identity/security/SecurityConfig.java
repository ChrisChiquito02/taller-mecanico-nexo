package com.sistemam01m02.identity.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sistemam01m02.config.*;
import com.sistemam01m02.shared.error.ApiErrors;
import java.net.URI;
import java.time.Clock;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
  }

  @Bean
  SecretKeySpec signingKey(AppSecurityProperties p, RecoveryProperties recovery, Environment env) {
    byte[] key;
    try {
      key = Base64.getDecoder().decode(p.jwtSigningKey());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("JWT_SIGNING_KEY debe estar en Base64");
    }
    if (key.length < 32)
      throw new IllegalStateException("JWT_SIGNING_KEY requiere al menos 256 bits");
    boolean local = env.matchesProfiles("local", "test");
    if ("None".equals(p.refreshCookieSameSite()) && !p.refreshCookieSecure())
      throw new IllegalStateException("SameSite=None requiere cookie Secure");
    URI url = URI.create(recovery.baseUrl());
    if (url.getHost() == null
        || url.getRawQuery() != null
        || url.getRawFragment() != null
        || url.getUserInfo() != null
        || !("https".equals(url.getScheme()) || (local && "http".equals(url.getScheme()))))
      throw new IllegalStateException(
          "APP_RECOVERY_BASE_URL debe ser una URL válida, HTTPS fuera de local/test, sin query ni"
              + " fragmento");
    for (String origin : p.allowedOrigins()) {
      URI o = URI.create(origin);
      if (o.getHost() == null
          || o.getUserInfo() != null
          || o.getRawQuery() != null
          || o.getRawFragment() != null
          || (o.getPath() != null && !o.getPath().isEmpty())
          || !("https".equals(o.getScheme()) || (local && "http".equals(o.getScheme()))))
        throw new IllegalStateException("Origen CORS inválido");
    }
    if (!local
        && (!p.refreshCookieSecure()
            || !("smtp".equals(recovery.mode()) || "disabled".equals(recovery.mode()))
            || !env.getProperty(
                "spring.mail.properties.mail.smtp.starttls.required", Boolean.class, true)))
      throw new IllegalStateException(
          "Producción requiere cookie Secure y recuperación SMTP segura o desactivada");
    return new SecretKeySpec(key, "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKeySpec key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKeySpec key, AppSecurityProperties p) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(p.issuer()));
    return decoder;
  }

  @Bean
  CorsConfigurationSource cors(AppSecurityProperties p) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(new ArrayList<>(p.allowedOrigins()));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    c.setAllowCredentials(true);
    c.setMaxAge(600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }

  @Bean
  FilterRegistrationBean<AuthRequestGuard> disableServletGuard(AuthRequestGuard guard) {
    FilterRegistrationBean<AuthRequestGuard> bean = new FilterRegistrationBean<>(guard);
    bean.setEnabled(false);
    return bean;
  }

  @Bean
  SecurityFilterChain apiSecurity(
      HttpSecurity http,
      DatabaseJwtAuthentication converter,
      AuthRequestGuard guard,
      CorsConfigurationSource cors)
      throws Exception {
    return http.cors(c -> c.configurationSource(cors))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'none'; script-src 'self'; style-src 'self'; connect-src"
                                + " 'self'; form-action 'self'; base-uri 'none'; frame-ancestors"
                                + " 'none'")))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        HttpMethod.GET,
                        "/api/health",
                        "/",
                        "/index.html",
                        "/reset-password.html",
                        "/app.js",
                        "/styles.css")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> ApiErrors.write(res, 401, "UNAUTHORIZED"))
                    .accessDeniedHandler((req, res, ex) -> ApiErrors.write(res, 403, "FORBIDDEN")))
        .oauth2ResourceServer(
            o ->
                o.jwt(j -> j.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint(
                        (req, res, ex) -> ApiErrors.write(res, 401, "UNAUTHORIZED"))
                    .accessDeniedHandler((req, res, ex) -> ApiErrors.write(res, 403, "FORBIDDEN")))
        .addFilterBefore(guard, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(new JsonBodyLimitFilter(), AuthRequestGuard.class)
        .build();
  }
}
