package com.sistemam01m02.shared.persistence;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class UuidV7Generator {
  private static final long TIMESTAMP_MASK = 0x0000FFFFFFFFFFFFL;
  private static final long VARIANT_MASK = 0x3FFFFFFFFFFFFFFFL;
  private static final long RFC_4122_VARIANT = 0x8000000000000000L;
  private final SecureRandom random = new SecureRandom();
  private final Clock clock;
  private long lastMillis = -1;
  private int sequence;

  public UuidV7Generator() {
    this(Clock.systemUTC());
  }

  UuidV7Generator(Clock clock) {
    this.clock = clock;
  }

  public synchronized UUID generate() {
    long now = Math.max(clock.millis(), lastMillis);
    if (now == lastMillis) {
      if (sequence == 0x0fff) {
        now = lastMillis + 1;
        sequence = 0;
      } else sequence++;
    } else {
      sequence = random.nextInt(0x1000);
    }
    lastMillis = now;
    long mostSignificantBits = ((now & TIMESTAMP_MASK) << 16) | 0x7000L | sequence;
    long leastSignificantBits = (random.nextLong() & VARIANT_MASK) | RFC_4122_VARIANT;
    return new UUID(mostSignificantBits, leastSignificantBits);
  }
}
