package com.example.employeeservice.service;

import com.example.common.entity.Employee;
import com.example.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeAdminService {

    private final EmployeeRepository employeeRepository;
    private Random random = new Random();

    private static final String[] FIRST_NAMES = {
        "Наталья", "Станислав", "Ольга", "Игорь", "Елена", "Сергей", "Анна", "Михаил", "Татьяна", "Александр"
    };
    private static final String[] LAST_NAMES = {
        "Мохирёва", "Шипилов", "Кузнецова", "Попов", "Сидорова", "Иванов", "Петрова", "Смирнов", "Фёдорова", "Николаев"
    };
    private static final String[] POSITIONS = {
        "Менеджер", "Печатник", "Бухгалтер", "Производственник", "Дизайнер", "Ламинатор"
    };

    public List<Employee> generateTestEmployees(int count) {
        List<Employee> employees = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Employee employee = new Employee();
            employee.setFullName(
                LAST_NAMES[random.nextInt(LAST_NAMES.length)] + " " +
                FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]
            );
            employee.setUsername("user" + (i + 1));
            employee.setPosition(POSITIONS[random.nextInt(POSITIONS.length)]);
            employee.setPhone("+7(9" + (100 + random.nextInt(900)) + ")" + String.format("%03d-%02d-%02d",
                random.nextInt(1000), random.nextInt(100), random.nextInt(100)));
            employee.setEmail("employee" + (i + 1) + "@example.com");

            employees.add(employeeRepository.save(employee));
        }

        return employees;
    }
}
