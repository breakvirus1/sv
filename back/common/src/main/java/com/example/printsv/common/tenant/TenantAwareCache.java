package com.example.printsv.common.tenant;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Обёртка над {@link Cache}, добавляющая идентификатор компании к ключу кэша.
 * Без этого записи разных компаний с одинаковыми id (например, "id_42") перекрывали бы друг друга.
 */
public class TenantAwareCache implements Cache {

    private static final String MASTER_PREFIX = "master";

    private final Cache delegate;

    public TenantAwareCache(Cache delegate) {
        this.delegate = delegate;
    }

    private Object tenantKey(@Nullable Object key) {
        Long tenantId = TenantContext.getTenantId();
        String prefix = tenantId == null ? MASTER_PREFIX : "company_" + tenantId;
        return prefix + "::" + key;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        return delegate.get(tenantKey(key));
    }

    @Override
    @SuppressWarnings("deprecation")
    public <T> T get(Object key, Class<T> type) {
        return delegate.get(tenantKey(key), type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return delegate.get(tenantKey(key), valueLoader);
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        delegate.put(tenantKey(key), value);
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
        return delegate.putIfAbsent(tenantKey(key), value);
    }

    @Override
    public void evict(Object key) {
        delegate.evict(tenantKey(key));
    }

    /**
     * Очищает только записи текущей компании. Если нативный кэш не позволяет выбрать
     * записи по префиксу — очищается весь кэш (консервативное поведение).
     */
    @Override
    public void clear() {
        if (!clearTenantEntries()) {
            delegate.clear();
        }
    }

    private boolean clearTenantEntries() {
        Object nativeCache = delegate.getNativeCache();
        if (nativeCache == null) {
            return false;
        }
        try {
            Method asMap = nativeCache.getClass().getMethod("asMap");
            Object result = asMap.invoke(nativeCache);
            if (result instanceof Map<?, ?> map) {
                String prefix = tenantKey("").toString();
                map.keySet().removeIf(k -> k != null && k.toString().startsWith(prefix));
                return true;
            }
        } catch (Exception ignored) {
            // нативный кэш не поддерживает выборочную очистку
        }
        return false;
    }
}
