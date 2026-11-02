package com.example.employeeservice.repository;

import com.example.employeeservice.entity.Employee;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByUsername(String username);

    @Query("SELECT e.username FROM Employee e")
    Set<String> findAllUsernames();

    @Cacheable(value = "employees", key = "'all'")
    @Query("SELECT e FROM Employee e WHERE e.deleted = false")
    List<Employee> findAllActive();
}
