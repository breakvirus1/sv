package com.example.orderservice.dto;

import com.example.clientservice.entity.Client;
import com.example.employeeservice.entity.Employee;
import com.example.orderservice.entity.Workshop;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderFilterOptionsResponse {
    private List<Client> clients;
    private List<Employee> managers;
    private List<Workshop> workshops;
}
