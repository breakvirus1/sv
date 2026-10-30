package com.example.clientservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDatabaseInfo {
    private Long companyId;
    private String companyName;
    private String databaseName;
    private Boolean backupAvailable;
}
