package com.cascade.unit.recovery;

import com.cascade.recovery.SnapshotManager;
import com.cascade.state.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SnapshotManager Tests")
class SnapshotManagerTest {
    @Test void takeAndRetrieve() {
        SnapshotManager sm = new SnapshotManager();
        ProductState ps = new ProductState("PS5", 100);
        var snap = sm.takeSnapshot(ps, 42);
        assertNotNull(snap); assertEquals(100, snap.quantity()); assertEquals(42, snap.lastSequenceNumber());
        assertTrue(sm.hasSnapshot("PS5")); assertEquals(snap, sm.getLatestSnapshot("PS5")); }

    @Test void noSnapshot() {
        SnapshotManager sm = new SnapshotManager();
        assertFalse(sm.hasSnapshot("UNKNOWN")); assertNull(sm.getLatestSnapshot("UNKNOWN")); }
}
