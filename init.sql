CREATE DATABASE keycloak_db;
CREATE SCHEMA IF NOT EXISTS ordschema;
CREATE SCHEMA IF NOT EXISTS svschema;

-- Таблица для маршрутизации баз данных по компаниям
CREATE TABLE IF NOT EXISTS svschema.tenant_databases (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL UNIQUE,
    database_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
