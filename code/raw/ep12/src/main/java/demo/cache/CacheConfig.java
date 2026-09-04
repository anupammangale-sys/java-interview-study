package demo.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caching turned on by hand rather than by a starter, so this runs with no
 * extra dependency and no server.
 *
 * ConcurrentMapCacheManager is a map in this JVM. Swapping it for Redis is a
 * change to this one bean and nothing else, which is the point worth knowing:
 * @Cacheable does not care where the cache lives.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("prices");
    }
}
