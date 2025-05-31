package ai.labs.eddi.engine.caching;

import ai.labs.eddi.engine.caching.bootstrap.CachingModule.CaffeineEmbeddedCacheManager;
import lombok.Getter;
import com.github.benmanes.caffeine.cache.Cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@ApplicationScoped
public class CacheFactory implements ICacheFactory {
    @Getter
    private final CaffeineEmbeddedCacheManager cacheManager;

    @Inject
    public CacheFactory(CaffeineEmbeddedCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public <K, V> ICache<K, V> getCache(String cacheName) {
        Cache<K, V> cache;
        if (cacheName != null) {
            cache = this.cacheManager.getCache(cacheName, true);
        } else {
            cache = this.cacheManager.getCache();
        }

        return new CaffeineCache<>(cacheName, cache);
    }
}
