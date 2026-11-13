package com.sistemam01m02.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.recovery")
public record RecoveryProperties(
    @Pattern(regexp = "smtp|development|disabled") String mode,
    @NotBlank String baseUrl,
    @NotBlank @Email String from) {}
