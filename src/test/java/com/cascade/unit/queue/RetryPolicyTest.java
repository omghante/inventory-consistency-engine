package com.cascade.unit.queue;

import com.cascade.queue.RetryPolicy;
import org.junit.jupiter.api.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RetryPolicy Tests")
class RetryPolicyTest {
    @Test void shouldRetry() { var p=RetryPolicy.defaultPolicy(); assertTrue(p.shouldRetry(1)); assertTrue(p.shouldRetry(2)); assertFalse(p.shouldRetry(3)); }
    @Test void exponentialBackoff() { var p=new RetryPolicy(5,Duration.ofMillis(100));
        assertEquals(100,p.getBackoffForAttempt(1).toMillis()); assertEquals(200,p.getBackoffForAttempt(2).toMillis());
        assertEquals(400,p.getBackoffForAttempt(3).toMillis()); }
}
