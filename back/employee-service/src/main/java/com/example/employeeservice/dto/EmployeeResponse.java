package com.example.employeeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String fullName;
    private String position;
    private String phone;
    private String email;
    private String username;
    private Long workshopId;
    private java.math.BigDecimal managerCashPercent;
    private Long roleId;
    private List<String> roles;
}
