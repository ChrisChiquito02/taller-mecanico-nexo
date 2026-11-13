package com.sistemam01m02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SistemaApplication {
  public static void main(String[] args) {
    SpringApplication.run(SistemaApplication.class, args);
  }
}
