package com.example.test.printsv.repository;

import com.example.test.printsv.entity.ClassRole;
import com.example.test.printsv.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<ClassRole, Long> {

    Optional<ClassRole> findByName(Role name);
}
