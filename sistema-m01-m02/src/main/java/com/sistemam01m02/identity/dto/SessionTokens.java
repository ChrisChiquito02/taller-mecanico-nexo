package com.sistemam01m02.identity.dto;

/** Contrato interno; solo Access se serializa al cliente, el refresh viaja en cookie. */
public record SessionTokens(String access, String refresh) {}
