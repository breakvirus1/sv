package com.example.printsv.common.tenant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Автоконфигурация мультитенантности.
 * Включается свойством app.tenant.enabled=true.
 * Маршрутизирующий DataSource дополнительно включается app.tenant.routing.enabled=true.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "app.tenant.enabled", havingValue = "true")
public class TenantAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.tenant.routing.enabled", havingValue = "true")
    @ConditionalOnMissingBean(TenantRoutingDataSource.class)
    public TenantRoutingDataSource tenantRoutingDataSource() {
        return new TenantRoutingDataSource();
    }

    @Bean
    @ConditionalOnMissingBean(DatabaseProvisioningService.class)
    public DatabaseProvisioningService databaseProvisioningService(
            ObjectProvider<TenantRoutingDataSource> routingDataSourceProvider) {
        return new DatabaseProvisioningService(routingDataSourceProvider);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(TenantFilter.class)
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration() {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(new TenantFilter());
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
        registration.setName("tenantFilter");
        return registration;
    }

    /**
     * Обёртывает все CacheManager, чтобы кэш не смешивал данные разных компаний.
     * Статический метод — BeanPostProcessor должен регистрироваться до инициализации бинов.
     */
    @Bean
    static TenantCachePostProcessor tenantCachePostProcessor() {
        return new TenantCachePostProcessor();
    }
}
