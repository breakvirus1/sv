package com.example.employeeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeCreateRequest {
    private String fullName;
    private String username;
    private String position;
    private String phone;
    private String email;
    private Long workshopId;
    private java.math.BigDecimal managerCashPercent;
}
