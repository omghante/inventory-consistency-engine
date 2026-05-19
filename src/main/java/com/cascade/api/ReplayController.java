package com.cascade.api;

import com.cascade.engine.CASCADEEngine;
import com.cascade.recovery.ReplayEngine;

/**
 * ReplayController — REST API facade for event replay operations.
 */
public class ReplayController {
    private final ReplayEngine replayEngine;
    public ReplayController(ReplayEngine replayEngine) { this.replayEngine = replayEngine; }
    public ReplayEngine.ReplayResult replay(CASCADEEngine target, String productId, int initialStock) {
        return replayEngine.replayAll(target, productId, initialStock);
    }
}
