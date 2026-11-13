package com.sistemam01m02.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "permission")
public class Permission {
  @Id
  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String code;

  @Column(name = "module_code", nullable = false, length = 30)
  private String moduleCode;

  @Column(nullable = false, length = 255)
  private String description;

  protected Permission() {}

  public String getCode() {
    return code;
  }
}
