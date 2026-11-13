package com.sistemam01m02.nexo.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;

@Validated
@ConfigurationProperties("nexo")
public record NexoSettings(
 @DefaultValue("ollama") @Pattern(regexp="ollama|disabled") String provider,
 @DefaultValue("http://127.0.0.1:11434") String baseUrl,
 @DefaultValue("qwen2.5-coder:7b") @Pattern(regexp="[A-Za-z0-9._:/-]{1,100}") String model,
 @DefaultValue("180") @Min(10) @Max(300) int timeoutSeconds,
 @DefaultValue("700") @Min(64) @Max(2048) int maxTokens,
 @DefaultValue("7") @Min(1) @Max(30) int retentionDays,
 @DefaultValue("8") @Min(1) @Max(60) int requestsPerMinute,
 @DefaultValue("100") @Min(1) @Max(1000) int turnsPerDay) {}
