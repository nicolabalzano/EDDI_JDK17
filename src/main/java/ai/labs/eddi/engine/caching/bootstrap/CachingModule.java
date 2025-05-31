package ai.labs.eddi.engine.caching.bootstrap;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import java.util.concurrent.TimeUnit;

/**
 * @author ginccc
 */
@ApplicationScoped
public class CachingModule {
    @Produces
    @ApplicationScoped
    CaffeineEmbeddedCacheManager provideEmbeddedCacheManager() {
        return new CaffeineEmbeddedCacheManager();
    }
    
    // Wrapper class per simulare EmbeddedCacheManager con Caffeine
    public static class CaffeineEmbeddedCacheManager {
        public <K, V> Cache<K, V> getCache(String cacheName, boolean createIfAbsent) {
            return getCache();
        }
        
        public <K, V> Cache<K, V> getCache() {
            return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
        }
    }
}
