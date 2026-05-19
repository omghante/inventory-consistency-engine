package com.cascade.utils;

import java.time.Duration;
import java.time.Instant;

/**
 * ClockUtils — Utility methods for time operations in CASCADE.
 */
public final class ClockUtils {
    private ClockUtils() {}
    public static Duration age(Instant eventTimestamp) { return Duration.between(eventTimestamp, Instant.now()); }
    public static boolean isExpired(Instant timestamp, Duration maxAge) { return age(timestamp).compareTo(maxAge) > 0; }
    public static boolean isRecent(Instant timestamp, Duration threshold) { return age(timestamp).compareTo(threshold) <= 0; }
    public static Instant max(Instant a, Instant b) { return a.isAfter(b) ? a : b; }
}
