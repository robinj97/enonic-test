package com.enonic.task1;

import com.enonic.assignment.store.Change;
import com.enonic.assignment.store.Snapshot;
import com.enonic.assignment.store.VersionedStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

public class InMemoryVersionedStore<K,V> implements VersionedStore<K, V> {
    private volatile long currentVersion = 0;
    private final ConcurrentHashMap<K, ConcurrentSkipListMap<Long,Change<K, V>>> keyHistoryMap = new ConcurrentHashMap<>();

    public InMemoryVersionedStore() {
    }

    @Override
    public long put(K key, V value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return performLockedPut(key, value);
    }

    @Override
    public long remove(K key) {
        Objects.requireNonNull(key, "key must not be null");
        return performLockedRemove(key);
    }

    @Override
    public Optional<V> get(K key) {
        long currentImmutableVersion = currentVersion;
        return this.getAt(key, currentImmutableVersion);
    }

    @Override
    public Optional<V> getAt(K key, long version) {
        Objects.requireNonNull(key, "key must not be null");
        long currentSafeVersion = currentVersion;
        long realFloorVersion = Math.min(currentSafeVersion,version);

        ConcurrentSkipListMap<Long, Change<K, V>> historyForKey = keyHistoryMap.get(key);
        if (historyForKey == null) {
            return Optional.empty();
        }

        Map.Entry<Long, Change<K, V>> flooredVersion = historyForKey.floorEntry(realFloorVersion);
        if (flooredVersion == null) {
            return Optional.empty();
        }

        return switch (flooredVersion.getValue()) {
            case Change.Put<K, V>(_, _, V value) -> Optional.of(value);
            case Change.Remove<K, V>(_, _) -> Optional.empty();
        };
    }

    @Override
    public long currentVersion() {
        return currentVersion;
    }

    @Override
    public Stream<Change<K, V>> changesSince(long sinceVersion) {
        return Stream.empty();
    }

    @Override
    public Snapshot<K, V> snapshot() {
        return new VersionSnapshot<>(this, currentVersion);
    }

    private synchronized long performLockedPut(K key, V value) {
        long newVersion = this.currentVersion + 1;
        Change<K,V> putOperation = new Change.Put<>(newVersion, key, value);
        ConcurrentSkipListMap<Long, Change<K, V>> history = getOrCreateKeyHistory(key);
        history.put(newVersion, putOperation);
        this.currentVersion = newVersion;
        return newVersion;
    }

    private synchronized long performLockedRemove(K key) {
        ConcurrentSkipListMap<Long, Change<K, V>> history = keyHistoryMap.get(key);
        boolean keyIsPresent = history != null
                && history.lastEntry().getValue() instanceof Change.Put;
        if (!keyIsPresent) {
            return this.currentVersion;
        }
        long newVersion = this.currentVersion + 1;
        Change<K, V> removeOperation = new Change.Remove<>(newVersion, key);
        history.put(newVersion, removeOperation);
        this.currentVersion = newVersion;
        return newVersion;
    }

    private ConcurrentSkipListMap<Long, Change<K, V>> getOrCreateKeyHistory(K key) {
        return keyHistoryMap.computeIfAbsent(key, k -> new ConcurrentSkipListMap<>());
    }

    private record VersionSnapshot<K, V>(InMemoryVersionedStore<K, V> store, long version) implements Snapshot<K, V> {

        @Override
        public Optional<V> get(K key) {
            return store.getAt(key, version);
        }

        @Override
        public Map<K, V> asMap() {
            Map<K, V> view = new HashMap<>();
            for (K key : store.keyHistoryMap.keySet()) {
                store.getAt(key, version).ifPresent(value -> view.put(key, value));
            }
            return Map.copyOf(view);
        }
    }
}
