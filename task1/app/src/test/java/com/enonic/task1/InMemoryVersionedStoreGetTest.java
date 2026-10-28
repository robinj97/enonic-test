package com.enonic.task1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryVersionedStoreGetTest {

    private InMemoryVersionedStore<String, String> store;

    @BeforeEach
    void setUp() {
        store = new InMemoryVersionedStore<>();
    }

    @Test
    void emptyStoreReturnsEmpty() {
        assertEquals(Optional.empty(), store.get("a"));
    }

    @Test
    void returnsValueAfterPut() {
        store.put("a", "x");
        assertEquals(Optional.of("x"), store.get("a"));
    }

    @Test
    void returnsNewestValueAfterOverwrite() {
        store.put("a", "x");
        store.put("a", "y");
        assertEquals(Optional.of("y"), store.get("a"));
    }

    @Test
    void returnsEmptyAfterRemove() {
        store.put("a", "x");
        store.remove("a");
        assertEquals(Optional.empty(), store.get("a"));
    }

    @Test
    void returnsValueAfterRePutFollowingRemove() {
        store.put("a", "x");
        store.remove("a");
        store.put("a", "z");
        assertEquals(Optional.of("z"), store.get("a"));
    }

    @Test
    void unknownKeyReturnsEmpty() {
        store.put("a", "x");
        assertEquals(Optional.empty(), store.get("nope"));
    }

    @Test
    void otherKeysAreNotAffected() {
        store.put("a", "x");
        store.put("b", "p");
        store.remove("a");
        assertEquals(Optional.of("p"), store.get("b"));
    }

    @Test
    void rejectsNullKey() {
        assertThrows(NullPointerException.class, () -> store.get(null));
    }
}
