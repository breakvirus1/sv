package com.example.clientservice.service;

import com.example.clientservice.dto.CompanyDatabaseInfo;
import com.example.printsv.common.tenant.DatabaseProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final DatabaseProvisioningService databaseProvisioningService;

    public boolean hasDatabaseForCompany(Long companyId) {
        return databaseProvisioningService.hasDatabaseForCompany(companyId);
    }

    public byte[] backupCompanyDatabase(Long companyId) {
        String databaseName = databaseProvisioningService.getDatabaseName(companyId);
        if (databaseName == null) {
            throw new RuntimeException("База данных для компании не найдена");
        }
        log.info("Backing up database {} for company {}", databaseName, companyId);
        try {
            return databaseProvisioningService.backupDatabase(databaseName);
        } catch (Exception e) {
            log.error("Error backing up database {}", databaseName, e);
            throw new RuntimeException("Ошибка бэкапа: " + e.getMessage(), e);
        }
    }

    public List<CompanyDatabaseInfo> listCompanyDatabases() {
        List<Map<String, Object>> dbList = databaseProvisioningService.listCompanyDatabases();
        return dbList.stream().map(row -> {
            CompanyDatabaseInfo info = new CompanyDatabaseInfo();
            info.setCompanyId(((Number) row.get("companyId")).longValue());
            info.setCompanyName((String) row.get("companyName"));
            info.setDatabaseName((String) row.get("databaseName"));
            info.setBackupAvailable(true);
            return info;
        }).collect(Collectors.toList());
    }
}
