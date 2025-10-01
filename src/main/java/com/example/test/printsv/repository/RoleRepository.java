package com.example.test.printsv.repository;

import com.example.test.printsv.entity.ERole;
import com.example.test.printsv.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);

    boolean existsByName(ERole name);

}
