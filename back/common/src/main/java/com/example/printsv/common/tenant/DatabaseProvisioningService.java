package com.example.printsv.common.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Управление базами данных компаний: создание, удаление, регистрация маршрутизации, бэкап.
 */
@Slf4j
public class DatabaseProvisioningService {

    @Value("${app.tenant.master-url:jdbc:postgresql://localhost:5432/svdb}")
    private String masterUrl;

    @Value("${app.tenant.username:postgres}")
    private String username;

    @Value("${app.tenant.password:postgres}")
    private String password;

    @Value("${app.tenant.pg-host:localhost}")
    private String pgHost;

    @Value("${app.tenant.pg-port:5432}")
    private int pgPort;

    private final ObjectProvider<TenantRoutingDataSource> routingDataSourceProvider;

    public DatabaseProvisioningService(ObjectProvider<TenantRoutingDataSource> routingDataSourceProvider) {
        this.routingDataSourceProvider = routingDataSourceProvider;
    }

    /** Проверяет, зарегистрирована ли база данных для компании. */
    public boolean hasDatabaseForCompany(Long companyId) {
        try (var conn = getConnection();
             var ps = conn.prepareStatement(
                 "SELECT 1 FROM svschema.tenant_databases WHERE company_id = ?")) {
            ps.setLong(1, companyId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            log.error("Error checking database for company {}", companyId, e);
            return false;
        }
    }

    /** Возвращает имя базы данных компании или null. */
    public String getDatabaseName(Long companyId) {
        try (var conn = getConnection();
             var ps = conn.prepareStatement(
                 "SELECT database_name FROM svschema.tenant_databases WHERE company_id = ?")) {
            ps.setLong(1, companyId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("database_name") : null;
            }
        } catch (Exception e) {
            log.error("Error fetching database name for company {}", companyId, e);
            return null;
        }
    }

    /** Создаёт базу данных для компании, копирует схему svschema и регистрирует её. */
    public String createDatabaseForCompany(Long companyId, String databaseName) throws Exception {
        // CREATE DATABASE нельзя выполнять внутри транзакции — отдельное соединение
        try (var conn = getConnection(); var stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE " + quoteIdentifier(databaseName));
            log.info("Created database: {}", databaseName);
        }

        // Копируем схему svschema из мастер-БД
        try {
            copySchemaFromMaster(databaseName);
        } catch (Exception e) {
            // Откатываем созданную БД при ошибке схемы
            try (var conn = getConnection(); var stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP DATABASE IF EXISTS " + quoteIdentifier(databaseName) + " WITH (FORCE)");
            }
            throw new RuntimeException("Не удалось скопировать схему: " + e.getMessage(), e);
        }

        try (var conn = getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO svschema.tenant_databases (company_id, database_name) VALUES (?, ?) "
                     + "ON CONFLICT (company_id) DO UPDATE SET database_name = EXCLUDED.database_name")) {
            ps.setLong(1, companyId);
            ps.setString(2, databaseName);
            ps.executeUpdate();
            log.info("Registered database {} for company {}", databaseName, companyId);
        }

        // Оповещаем роутер (если он включён в этом сервисе)
        TenantRoutingDataSource router = routingDataSourceProvider.getIfAvailable();
        if (router != null) {
            router.addTenantDatabase(companyId, databaseName);
        }

        return databaseName;
    }

    /** Удаляет базу данных компании и её регистрацию. */
    public void dropDatabaseForCompany(Long companyId, String databaseName) throws Exception {
        TenantRoutingDataSource router = routingDataSourceProvider.getIfAvailable();
        if (router != null) {
            router.removeTenantDatabase(companyId);
        }

        try (var conn = getConnection(); var stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + quoteIdentifier(databaseName) + " WITH (FORCE)");
            log.info("Dropped database: {}", databaseName);
        }

        try (var conn = getConnection();
             var ps = conn.prepareStatement("DELETE FROM svschema.tenant_databases WHERE company_id = ?")) {
            ps.setLong(1, companyId);
            ps.executeUpdate();
        }
    }

    /** Копирует DDL схемы svschema и таблиц public из мастер-БД в базу компании. */
    public void copySchemaFromMaster(String databaseName) throws Exception {
        List<String> dumpCmd = new ArrayList<>(List.of(
            "pg_dump",
            "-h", pgHost,
            "-p", String.valueOf(pgPort),
            "-U", username,
            "--schema-only",
            "--no-owner",
            "--no-privileges",
            "-n", "svschema",
            masterDatabaseName()
        ));

        runPipeline(dumpCmd, databaseName);

        // Таблицы public, используемые comment-service
        List<String> publicDumpCmd = new ArrayList<>(List.of(
            "pg_dump",
            "-h", pgHost,
            "-p", String.valueOf(pgPort),
            "-U", username,
            "--schema-only",
            "--no-owner",
            "--no-privileges",
            "-t", "public.comments",
            "-t", "public.comment_replies",
            "-t", "public.comment_images",
            "-t", "public.reply_images",
            "-t", "public.images",
            "-t", "public.notifications",
            masterDatabaseName()
        ));

        runPipeline(publicDumpCmd, databaseName);
        log.info("Schema copied from master to {}", databaseName);
    }

    private static final char BACKSLASH = (char) 92;

    /**
     * Переписывает вывод pg_dump, отбрасывая конструкции, которые не понимает сервер PostgreSQL 15:
     * SET transaction_timeout (добавлен в PG 17) и метакоманды restrict / unrestrict (pg_dump 18).
     */
    private static void filterDump(InputStream in, java.io.OutputStream out) throws java.io.IOException {
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        java.io.BufferedWriter writer = new java.io.BufferedWriter(
            new java.io.OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8));
        String psqlCommandPrefix = String.valueOf(BACKSLASH);
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("SET transaction_timeout")
                || line.startsWith(psqlCommandPrefix + "restrict")
                || line.startsWith(psqlCommandPrefix + "unrestrict")) {
                continue;
            }
            writer.write(line);
            writer.newLine();
        }
        writer.flush();
    }

    private void runPipeline(List<String> dumpCmd, String targetDatabase) throws Exception {
        List<String> psqlCmd = List.of(
            "psql",
            "-h", pgHost,
            "-p", String.valueOf(pgPort),
            "-U", username,
            "-q",
            "-v", "ON_ERROR_STOP=1",
            "-d", targetDatabase
        );

        ProcessBuilder pbDump = new ProcessBuilder(dumpCmd);
        pbDump.environment().put("PGPASSWORD", password);
        ProcessBuilder pbPsql = new ProcessBuilder(psqlCmd);
        pbPsql.environment().put("PGPASSWORD", password);

        Process dumpProcess = pbDump.start();
        Process psqlProcess = pbPsql.start();

        // Перекидываем вывод pg_dump в stdin psql, отфильтровывая несовместимые строки
        Thread pipeThread = new Thread(() -> {
            try (InputStream in = dumpProcess.getInputStream();
                 java.io.OutputStream out = psqlProcess.getOutputStream()) {
                filterDump(in, out);
            } catch (Exception e) {
                log.error("Error piping pg_dump to psql", e);
            }
        });
        pipeThread.start();

        // Читаем stderr обоих процессов, чтобы не заблокировать их
        String dumpErr = new String(dumpProcess.getErrorStream().readAllBytes());
        String psqlErr = new String(psqlProcess.getErrorStream().readAllBytes());

        int dumpExit = dumpProcess.waitFor();
        int psqlExit = psqlProcess.waitFor();
        pipeThread.join();

        if (dumpExit != 0 || psqlExit != 0) {
            throw new RuntimeException(
                "Копирование схемы завершилось с кодами (dump=" + dumpExit + ", psql=" + psqlExit + "): "
                    + dumpErr + psqlErr);
        }
    }

    /** Имя базы данных мастера, извлечённое из app.tenant.master-url. */
    String masterDatabaseName() {
        String url = masterUrl;
        int q = url.indexOf('?');
        if (q >= 0) {
            url = url.substring(0, q);
        }
        int idx = url.lastIndexOf('/');
        return idx > 0 ? url.substring(idx + 1) : "svdb";
    }

    /** Делает pg_dump базы данных компании, возвращает SQL-дамп. */
    public byte[] backupDatabase(String databaseName) throws Exception {
        List<String> command = List.of(
            "pg_dump",
            "-h", pgHost,
            "-p", String.valueOf(pgPort),
            "-U", username,
            "--no-owner",
            "--no-privileges",
            databaseName
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = pb.environment();
        env.put("PGPASSWORD", password);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        byte[] dump;
        try (InputStream in = process.getInputStream();
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            filterDump(in, bos);
            dump = bos.toByteArray();
        }

        String err = new String(process.getErrorStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("pg_dump завершился с кодом " + exitCode + ": " + err);
        }

        log.info("Backup of {} completed: {} bytes", databaseName, dump.length);
        return dump;
    }

    /** Список всех баз данных компаний с названиями компаний. */
    public List<Map<String, Object>> listCompanyDatabases() {
        List<Map<String, Object>> result = new ArrayList<>();
        try (var conn = getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT td.company_id, c.name AS company_name, td.database_name "
                     + "FROM svschema.tenant_databases td "
                     + "JOIN svschema.companies c ON td.company_id = c.id")) {
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("companyId", rs.getLong("company_id"));
                entry.put("companyName", rs.getString("company_name"));
                entry.put("databaseName", rs.getString("database_name"));
                result.add(entry);
            }
        } catch (Exception e) {
            log.error("Error listing company databases", e);
        }
        return result;
    }

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(masterUrl, username, password);
    }

    private static String quoteIdentifier(String name) {
        if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Некорректное имя базы данных: " + name);
        }
        return name;
    }
}
