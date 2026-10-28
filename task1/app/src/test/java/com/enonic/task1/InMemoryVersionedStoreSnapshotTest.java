package com.enonic.task1;

import com.enonic.assignment.store.Snapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryVersionedStoreSnapshotTest {

    private InMemoryVersionedStore<String, String> store;

    @BeforeEach
    void setUp() {
        store = new InMemoryVersionedStore<>();
    }

    @Test
    void snapshotOfEmptyStoreIsVersionZeroAndEmpty() {
        Snapshot<String, String> snapshot = store.snapshot();
        assertEquals(0, snapshot.version());
        assertEquals(Map.of(), snapshot.asMap());
    }

    @Test
    void snapshotHasVersionAtTimeOfTaking() {
        store.put("a", "x");
        store.put("b", "y");
        assertEquals(2, store.snapshot().version());
    }

    @Test
    void snapshotDoesNotSeeLaterPut() {
        store.put("a", "x");
        Snapshot<String, String> snapshot = store.snapshot();

        store.put("a", "y");
        store.put("b", "new");

        assertEquals(Optional.of("x"), snapshot.get("a"));
        assertEquals(Optional.empty(), snapshot.get("b"));
        assertEquals(Map.of("a", "x"), snapshot.asMap());
    }

    @Test
    void snapshotDoesNotSeeLaterRemove() {
        store.put("a", "x");
        Snapshot<String, String> snapshot = store.snapshot();

        store.remove("a");

        assertEquals(Optional.of("x"), snapshot.get("a"));
        assertEquals(Map.of("a", "x"), snapshot.asMap());
    }

    @Test
    void asMapLeavesOutRemovedKeys() {
        store.put("a", "x");
        store.put("b", "y");
        store.remove("a");

        assertEquals(Map.of("b", "y"), store.snapshot().asMap());
    }

    @Test
    void asMapIsUnmodifiable() {
        store.put("a", "x");
        Map<String, String> map = store.snapshot().asMap();

        assertThrows(UnsupportedOperationException.class, () -> map.put("b", "y"));
        assertEquals(Optional.empty(), store.get("b"));
    }

    @Test
    void snapshotGetRejectsNullKey() {
        assertThrows(NullPointerException.class, () -> store.snapshot().get(null));
    }
}
