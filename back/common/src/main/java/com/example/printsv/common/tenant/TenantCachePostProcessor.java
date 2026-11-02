package com.example.printsv.common.tenant;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.CacheManager;

/**
 * Автоматически оборачивает все {@link CacheManager} теневыми обёртками,
 * чтобы ключи кэша содержали идентификатор компании.
 */
public class TenantCachePostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof CacheManager cm && !(bean instanceof TenantAwareCacheManager)) {
            return new TenantAwareCacheManager(cm);
        }
        return bean;
    }
}
