package com.cascade.config;

import java.time.Duration;

/**
 * MetricsConfig — Configuration for metrics collection.
 */
public record MetricsConfig(Duration reportInterval, boolean enableThroughputMetrics, boolean enableConflictMetrics) {
    public static MetricsConfig defaults() {
        return new MetricsConfig(Duration.ofSeconds(10), true, true);
    }
}
