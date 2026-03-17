package no.nav.oebs.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String MEMBERSHIP_CACHE = "memberships";

    /**
     * TTL settes til det dobbelte av refresh-intervallet som sikkerhetsnett.
     * Normalt vil {@code @CachePut} i MembershipCacheService holde cachen varm.
     */
    @Bean
    public CacheManager cacheManager(
            @Value("${oebs.cache.membership-ttl-minutes:30}") long ttlMinutes) {

        CaffeineCacheManager manager = new CaffeineCacheManager(MEMBERSHIP_CACHE);
        manager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .recordStats()
        );
        return manager;
    }
}

