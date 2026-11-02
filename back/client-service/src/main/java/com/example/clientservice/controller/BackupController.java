package com.example.clientservice.controller;

import com.example.clientservice.dto.CompanyDatabaseInfo;
import com.example.clientservice.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Backup API", description = "Бэкап баз данных компаний")
public class BackupController {

    private final BackupService backupService;

    @Operation(summary = "Получить список баз данных компаний")
    @GetMapping("/databases")
    @PreAuthorize("hasRole('GOD')")
    public ResponseEntity<List<CompanyDatabaseInfo>> getCompanyDatabases() {
        return ResponseEntity.ok(backupService.listCompanyDatabases());
    }

    @Operation(summary = "Скачать бэкап базы данных компании (SQL-дамп)")
    @PostMapping("/{companyId}/backup")
    @PreAuthorize("hasRole('GOD')")
    public ResponseEntity<byte[]> backupCompanyDatabase(@PathVariable Long companyId) {
        byte[] dump = backupService.backupCompanyDatabase(companyId);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "sv_company_" + companyId + "_" + timestamp + ".sql";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(dump);
    }

    @Operation(summary = "Проверить наличие БД у компании")
    @GetMapping("/{companyId}/database-status")
    @PreAuthorize("hasRole('GOD')")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus(@PathVariable Long companyId) {
        boolean hasDb = backupService.hasDatabaseForCompany(companyId);
        return ResponseEntity.ok(Map.of(
            "companyId", companyId,
            "hasDatabase", hasDb
        ));
    }
}
