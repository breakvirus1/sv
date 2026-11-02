package com.example.printsv.common.tenant;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кэш-менеджер, добавляющий идентификатор компании к ключам кэша.
 */
public class TenantAwareCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final Map<String, Cache> wrapped = new ConcurrentHashMap<>();

    public TenantAwareCacheManager(CacheManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Cache getCache(String name) {
        Cache delegateCache = delegate.getCache(name);
        if (delegateCache == null) {
            return null;
        }
        return wrapped.computeIfAbsent(name, n -> new TenantAwareCache(delegateCache));
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
