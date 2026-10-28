package com.enonic.task1;

import com.enonic.assignment.store.Change;
import com.enonic.assignment.store.Snapshot;
import com.enonic.assignment.store.VersionedStore;

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
    public long remove(Object key) {
        return 0;
    }

    @Override
    public Optional get(Object key) {
        return Optional.empty();
    }

    @Override
    public Optional getAt(Object key, long version) {
        return Optional.empty();
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
    public Snapshot snapshot() {
        return null;
    }

    private synchronized long performLockedPut(K key, V value) {
        long newVersion = this.currentVersion + 1;
        Change<K,V> putOperation = new Change.Put<>(newVersion, key, value);
        ConcurrentSkipListMap<Long, Change<K, V>> history = getOrCreateKeyHistory(key);
        history.put(newVersion, putOperation);
        this.currentVersion = newVersion;
        return newVersion;
    }

    private ConcurrentSkipListMap<Long, Change<K, V>> getOrCreateKeyHistory(K key) {
        return keyHistoryMap.computeIfAbsent(key, k -> new ConcurrentSkipListMap<>());
    }
}
