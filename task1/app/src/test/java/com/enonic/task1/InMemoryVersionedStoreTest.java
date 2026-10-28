package com.enonic.task1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryVersionedStoreTest {

    private InMemoryVersionedStore<String, String> store;

    @BeforeEach
    void setUp() {
        store = new InMemoryVersionedStore<>();
    }

    @Test
    void newStoreIsAtVersionZero() {
        assertEquals(0, store.currentVersion());
    }

    @Test
    void firstPutProducesVersionOne() {
        assertEquals(1, store.put("a", "x"));
        assertEquals(1, store.currentVersion());
    }

    @Test
    void everyPutIncreasesVersionByExactlyOne() {
        assertEquals(1, store.put("a", "x"));
        assertEquals(2, store.put("b", "y"));
        assertEquals(3, store.put("c", "z"));
        assertEquals(3, store.currentVersion());
    }

    @Test
    void overwritingSameKeyStillCreatesNewVersion() {
        assertEquals(1, store.put("a", "x"));
        assertEquals(2, store.put("a", "y"));
        assertEquals(3, store.put("a", "y"));
        assertEquals(3, store.currentVersion());
    }

    @Test
    void removeIncrementsVersion() {
        assertEquals(0,store.currentVersion());
        assertEquals(1,store.put("a","x"));
        assertEquals(2,store.remove("a"));
    }

    @Test
    void removeOnEmptyStoreDoesNotChangeVersion() {
        assertEquals(0, store.remove("a"));
        assertEquals(0, store.currentVersion());
    }

    @Test
    void removingSameKeyTwiceDoesNotChangeVersion() {
        store.put("a", "x");
        assertEquals(2, store.remove("a"));
        assertEquals(2, store.remove("a"));
        assertEquals(2, store.currentVersion());
    }

    @Test
    void putRejectsNullKey() {
        assertThrows(NullPointerException.class, () -> store.put(null, "x"));
    }

    @Test
    void putRejectsNullValue() {
        assertThrows(NullPointerException.class, () -> store.put("a", null));
    }

    @Test
    void rejectedPutDoesNotConsumeAVersion() {
        assertThrows(NullPointerException.class, () -> store.put(null, "x"));
        assertThrows(NullPointerException.class, () -> store.put("a", null));
        assertEquals(0, store.currentVersion());
        assertEquals(1, store.put("a", "x"));
    }
}
