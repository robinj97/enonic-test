package com.enonic.task1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryVersionedStoreGetAtTest {

    private InMemoryVersionedStore<String, String> store;

    // Timeline:
    // v1 put a=x, v2 put b=p, v3 put a=y, v4 remove a, v5 put a=z
    @BeforeEach
    void setUp() {
        store = new InMemoryVersionedStore<>();
        store.put("a", "x");
        store.put("b", "p");
        store.put("a", "y");
        store.remove("a");
        store.put("a", "z");
    }

    @Test
    void returnsValueRightAfterPut() {
        assertEquals(Optional.of("x"), store.getAt("a", 1));
    }

    @Test
    void returnsNewValueAfterOverwrite() {
        assertEquals(Optional.of("y"), store.getAt("a", 3));
    }

    @Test
    void stillReturnsOldValueAtOldVersion() {
        assertEquals(Optional.of("x"), store.getAt("a", 1));
    }

    @Test
    void returnsEmptyRightAfterRemove() {
        assertEquals(Optional.empty(), store.getAt("a", 4));
    }

    @Test
    void returnsValueAfterRePutFollowingRemove() {
        assertEquals(Optional.of("z"), store.getAt("a", 5));
    }

    @Test
    void versionBetweenChangesReturnsEarlierValue() {
        // version 2 only changed b, so a is still x
        assertEquals(Optional.of("x"), store.getAt("a", 2));
    }

    @Test
    void versionBeyondCurrentReturnsLatestValue() {
        assertEquals(Optional.of("z"), store.getAt("a", 99));
    }

    @Test
    void versionZeroIsEmptyStore() {
        assertEquals(Optional.empty(), store.getAt("a", 0));
    }

    @Test
    void negativeVersionReturnsEmpty() {
        assertEquals(Optional.empty(), store.getAt("a", -1));
    }

    @Test
    void keyNotWrittenYetReturnsEmpty() {
        // b is first written at version 2
        assertEquals(Optional.empty(), store.getAt("b", 1));
    }

    @Test
    void unknownKeyReturnsEmpty() {
        assertEquals(Optional.empty(), store.getAt("nope", 3));
    }

    @Test
    void writesToOtherKeyDoNotChangeThisKey() {
        assertEquals(Optional.of("p"), store.getAt("b", 5));
    }

    @Test
    void rejectsNullKey() {
        assertThrows(NullPointerException.class, () -> store.getAt(null, 1));
    }
}
