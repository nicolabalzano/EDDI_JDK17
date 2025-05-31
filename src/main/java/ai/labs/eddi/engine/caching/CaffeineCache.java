package ai.labs.eddi.engine.caching;

import com.github.benmanes.caffeine.cache.Cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CaffeineCache<K, V> implements ICache<K, V> {
    private final String cacheName;
    private final Cache<K, V> cache;

    public CaffeineCache(String cacheName, Cache<K, V> cache) {
        this.cacheName = cacheName != null ? cacheName : "default";
        this.cache = cache;
    }

    @Override
    public String getCacheName() {
        return cacheName;
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit unit) {
        V oldValue = cache.getIfPresent(key);
        cache.put(key, value);
        return oldValue;
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
        return cache.get(key, k -> value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
        cache.putAll(map);
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit unit) {
        V oldValue = cache.getIfPresent(key);
        if (oldValue != null) {
            cache.put(key, value);
        }
        return oldValue;
    }

    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
        V currentValue = cache.getIfPresent(key);
        if (currentValue != null && currentValue.equals(oldValue)) {
            cache.put(key, value);
            return true;
        }
        return false;
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return put(key, value, lifespan, lifespanUnit);
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return putIfAbsent(key, value, lifespan, lifespanUnit);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return cache.get(key, k -> value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        V currentValue = cache.getIfPresent((K) key);
        if (currentValue != null && currentValue.equals(value)) {
            cache.invalidate((K) key);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        V currentValue = cache.getIfPresent(key);
        if (currentValue != null && currentValue.equals(oldValue)) {
            cache.put(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        V oldValue = cache.getIfPresent(key);
        if (oldValue != null) {
            cache.put(key, value);
        }
        return oldValue;
    }

    @Override
    public int size() {
        return (int) cache.estimatedSize();
    }

    @Override
    public boolean isEmpty() {
        return cache.estimatedSize() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.getIfPresent((K) key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return cache.asMap().containsValue(value);
    }

    @Override
    public V get(Object key) {
        return cache.getIfPresent((K) key);
    }

    @Override
    public V put(K key, V value) {
        V oldValue = cache.getIfPresent(key);
        cache.put(key, value);
        return oldValue;
    }

    @Override
    public V remove(Object key) {
        V oldValue = cache.getIfPresent((K) key);
        cache.invalidate((K) key);
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        cache.putAll(m);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public Set<K> keySet() {
        return cache.asMap().keySet();
    }

    @Override
    public Collection<V> values() {
        return cache.asMap().values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return cache.asMap().entrySet();
    }
}
