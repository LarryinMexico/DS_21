package com.finalproject.FinalProject.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        ConcurrentMapCache movieCache = new ConcurrentMapCache("movieCache", 
            new ConcurrentHashMap<>(256), 
            false);
        
        cacheManager.setCaches(Arrays.asList(movieCache));
        return cacheManager;
    }
}