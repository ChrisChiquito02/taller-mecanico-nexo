package com.sistemam01m02.shared.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {
  @Test
  void fixedClockDoesNotBlockOnSequenceOverflow() {
    var g = new UuidV7Generator(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    Set<UUID> seen = new HashSet<>();
    UUID previous = null;
    for (int i = 0; i < 12000; i++) {
      UUID id = g.generate();
      assertEquals(7, id.version());
      assertEquals(2, id.variant());
      assertTrue(seen.add(id));
      if (previous != null) assertTrue(id.compareTo(previous) > 0);
      previous = id;
    }
  }

  @Test
  void concurrentGenerationIsUnique() throws Exception {
    var g = new UuidV7Generator();
    Set<UUID> seen = ConcurrentHashMap.newKeySet();
    try (var pool = Executors.newFixedThreadPool(8)) {
      List<Future<?>> futures = new ArrayList<>();
      for (int t = 0; t < 8; t++)
        futures.add(
            pool.submit(
                () -> {
                  for (int i = 0; i < 1000; i++) assertTrue(seen.add(g.generate()));
                }));
      for (var f : futures) f.get();
    }
    assertEquals(8000, seen.size());
  }

  @Test
  void backwardClockStaysOrdered() {
    Clock backward =
        new Clock() {
          long value = 2000;

          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          public Clock withZone(ZoneId z) {
            return this;
          }

          public Instant instant() {
            return Instant.ofEpochMilli(value--);
          }
        };
    var g = new UuidV7Generator(backward);
    UUID a = g.generate(), b = g.generate();
    assertTrue(b.compareTo(a) > 0);
  }
}
