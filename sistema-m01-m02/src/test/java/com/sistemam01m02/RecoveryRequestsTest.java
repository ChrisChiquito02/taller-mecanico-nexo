package com.sistemam01m02;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sistemam01m02.identity.dto.Contracts.Forgot;
import com.sistemam01m02.identity.service.*;
import com.sistemam01m02.shared.error.ApiException;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class RecoveryRequestsTest {
  @Test
  void boundedAdmissionDoesNotWaitForDeliveryAndRejectsOverloadUniformly() throws Exception {
    var passwords = mock(PasswordService.class);
    var entered = new CountDownLatch(2);
    var release = new CountDownLatch(1);
    doAnswer(
            call -> {
              entered.countDown();
              if (!release.await(5, TimeUnit.SECONDS))
                throw new IllegalStateException("Test delivery timeout");
              return null;
            })
        .when(passwords)
        .forgot(any());
    var queue = new RecoveryRequests(passwords);
    try {
      queue.submit(new Forgot("known@gmail.com"));
      queue.submit(new Forgot("unknown@gmail.com"));
      assertTrue(entered.await(2, TimeUnit.SECONDS));
      assertEquals(2, queue.pendingTasks());
      for (int i = 0; i < 64; i++) queue.submit(new Forgot("queued@gmail.com"));
      var error =
          assertThrows(ApiException.class, () -> queue.submit(new Forgot("another@gmail.com")));
      assertEquals(503, error.status());
      assertEquals("RECOVERY_BUSY", error.code());
      assertEquals(66, queue.pendingTasks());
    } finally {
      release.countDown();
      queue.stop();
    }
    assertEquals(0, queue.pendingTasks());
  }
}
