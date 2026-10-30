package com.example.printsv.common.tenant;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Маршрутизатор DataSource на основе company_id из TenantContext.
 * Ключ "master" — общая база (svdb), ключи "company_{id}" — базы компаний.
 */
@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private static final String MASTER_KEY = "master";

    @Value("${app.tenant.master-url:jdbc:postgresql://localhost:5432/svdb}")
    private String masterUrl;

    @Value("${app.tenant.username:postgres}")
    private String username;

    @Value("${app.tenant.password:postgres}")
    private String password;

    private final Map<Object, Object> resolvedTargets = new ConcurrentHashMap<>();

    private HikariDataSource masterDataSource;

    @PostConstruct
    public void init() {
        masterDataSource = createDataSource(masterUrl, username, password);
        resolvedTargets.put(MASTER_KEY, masterDataSource);
        applyTargets();
        log.info("TenantRoutingDataSource initialized with master URL: {}", masterUrl);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return MASTER_KEY;
        }
        String lookupKey = "company_" + tenantId;
        if (!resolvedTargets.containsKey(lookupKey)) {
            synchronized (this) {
                if (!resolvedTargets.containsKey(lookupKey)) {
                    String dbName = getDatabaseNameByTenantId(tenantId);
                    if (dbName == null) {
                        log.warn("No tenant database registered for company {}, falling back to master", tenantId);
                        return MASTER_KEY;
                    }
                    HikariDataSource ds = createDataSource(buildDbUrl(dbName), username, password);
                    resolvedTargets.put(lookupKey, ds);
                    applyTargets();
                    log.info("Registered DataSource for tenant {} -> {}", tenantId, dbName);
                }
            }
        }
        return lookupKey;
    }

    private void applyTargets() {
        Map<Object, Object> targets = new HashMap<>(resolvedTargets);
        setTargetDataSources(targets);
        setDefaultTargetDataSource(masterDataSource);
        afterPropertiesSet();
    }

    private String getDatabaseNameByTenantId(Long tenantId) {
        try (var conn = masterDataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT database_name FROM svschema.tenant_databases WHERE company_id = " + tenantId)) {
            if (rs.next()) {
                return rs.getString("database_name");
            }
        } catch (Exception e) {
            log.error("Error fetching database name for tenant {}", tenantId, e);
        }
        return null;
    }

    /** Строит JDBC-URL для базы компании на том же хосте/порту, что и мастер. */
    String buildDbUrl(String databaseName) {
        int idx = masterUrl.lastIndexOf('/');
        String prefix = idx > 0 ? masterUrl.substring(0, idx) : "jdbc:postgresql://localhost:5432";
        return prefix + "/" + databaseName;
    }

    public HikariDataSource createDataSource(String url, String user, String pass) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(20000);
        ds.setIdleTimeout(300000);
        ds.setMaxLifetime(1200000);
        return ds;
    }

    public synchronized void addTenantDatabase(Long companyId, String databaseName) {
        String key = "company_" + companyId;
        if (!resolvedTargets.containsKey(key)) {
            HikariDataSource ds = createDataSource(buildDbUrl(databaseName), username, password);
            resolvedTargets.put(key, ds);
            applyTargets();
            log.info("Added tenant database: {} -> {}", companyId, databaseName);
        }
    }

    public synchronized void removeTenantDatabase(Long companyId) {
        String key = "company_" + companyId;
        Object ds = resolvedTargets.remove(key);
        if (ds instanceof HikariDataSource hds) {
            hds.close();
            applyTargets();
            log.info("Removed tenant database: {}", companyId);
        }
    }
}
