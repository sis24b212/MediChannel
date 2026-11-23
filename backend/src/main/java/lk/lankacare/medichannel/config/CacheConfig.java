package lk.lankacare.medichannel.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures TTL caching (5 minutes) for public doctor and specialization lists (Point 7 Performance).
 */
@Configuration
public class CacheConfig {

    public static final String SPECIALIZATIONS_CACHE = "publicSpecializations";
    public static final String DOCTORS_CACHE = "publicDoctors";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(SPECIALIZATIONS_CACHE, DOCTORS_CACHE);
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(200)
        );
        return manager;
    }
}
