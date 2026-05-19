package com.cascade.config;

import java.time.Duration;

/**
 * EngineConfig — Configuration for the CASCADE engine.
 */
public record EngineConfig(
        double trustAcceptanceThreshold,
        Duration maxAcceptableEventAge,
        boolean enableIdempotency,
        boolean enablePreconditions,
        boolean enableCausality
) {
    public static EngineConfig defaults() {
        return new EngineConfig(0.5, Duration.ofMinutes(5), true, true, true);
    }
}
