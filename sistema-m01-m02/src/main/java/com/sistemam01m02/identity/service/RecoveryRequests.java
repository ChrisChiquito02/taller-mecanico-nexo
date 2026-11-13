package com.sistemam01m02.identity.service;

import com.sistemam01m02.identity.dto.Contracts.Forgot;
import com.sistemam01m02.shared.error.ApiException;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.*;
import org.springframework.stereotype.Service;

/**
 * Admisión acotada antes de consultar identidad: SMTP no determina el tiempo HTTP ni el resultado.
 */
@Service
public class RecoveryRequests {
  private static final Logger log = LoggerFactory.getLogger(RecoveryRequests.class);
  private final PasswordService passwords;
  private final AtomicInteger pending = new AtomicInteger();
  private final ThreadPoolExecutor workers =
      new ThreadPoolExecutor(
          2,
          2,
          0,
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<>(64),
          Thread.ofPlatform().daemon(true).name("recovery-", 0).factory(),
          new ThreadPoolExecutor.AbortPolicy());

  public RecoveryRequests(PasswordService passwords) {
    this.passwords = passwords;
  }

  public void submit(Forgot input) {
    pending.incrementAndGet();
    try {
      workers.execute(
          () -> {
            try {
              passwords.forgot(input);
            } catch (RuntimeException error) {
              log.warn("SMTP_RECOVERY_DELIVERY_FAILED type={}", error.getClass().getSimpleName());
            } finally {
              pending.decrementAndGet();
            }
          });
    } catch (RejectedExecutionException error) {
      pending.decrementAndGet();
      throw new ApiException(
          503, "RECOVERY_BUSY", "Recuperación temporalmente ocupada; inténtalo más tarde");
    }
  }

  public int pendingTasks() {
    return pending.get();
  }

  @PreDestroy
  public void stop() throws InterruptedException {
    workers.shutdown();
    if (!workers.awaitTermination(10, TimeUnit.SECONDS)) workers.shutdownNow();
  }
}
