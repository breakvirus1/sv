package com.example.test.printsv;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.test.printsv.entity.ERole;
import com.example.test.printsv.entity.Material;
import com.example.test.printsv.entity.Role;
import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.entity.User;
import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.repository.MaterialRepository;
import com.example.test.printsv.repository.RoleRepository;
import com.example.test.printsv.repository.SubZakazRepository;
import com.example.test.printsv.repository.UserRepository;
import com.example.test.printsv.repository.ZakazRepository;
import com.example.test.printsv.request.RegisterRequest;
import com.example.test.printsv.service.AuthService;
import com.example.test.printsv.service.SubZakazService;
import com.example.test.printsv.service.ZakazService;

@SpringBootApplication
public class PrintsvApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(PrintsvApplication.class, args);
    }

@Autowired
    UserRepository userRepository;
    @Autowired
    ZakazRepository zakazRepository;
    @Autowired
    SubZakazService subZakazService;
    @Autowired
    MaterialRepository materialRepository;
    @Autowired
    SubZakazRepository subZakazRepository;

    @Bean
    CommandLineRunner createDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Create the database if it doesn't exist
                jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS svdb");
                System.out.println("Database 'svdb' created or already exists");
            } catch (Exception e) {
                System.err.println("Error creating database: " + e.getMessage());
            }
        };
    }

    @Bean
    CommandLineRunner initUsers(AuthService authService, RoleRepository roleRepository) {
        System.out.println("FIND ROLES IN REPOSITORY: " + roleRepository.findAll());
        return args -> {
            if (!roleRepository.existsByName(ERole.ROLE_MANAGER)) {
                roleRepository.save(new Role(null, ERole.ROLE_MANAGER));
                System.out.println("ROLE_MANAGER saved");
            }
            if (!roleRepository.existsByName(ERole.ROLE_ADMIN)) {
                roleRepository.save(new Role(null, ERole.ROLE_ADMIN));
                System.out.println("ROLE_ADMIN saved");
            }
            if (!roleRepository.existsByName(ERole.ROLE_OPERATOR)) {
                roleRepository.save(new Role(null, ERole.ROLE_OPERATOR));
                System.out.println("ROLE_OPERATOR saved");
			}
            System.out.println("Roles in database: " + roleRepository.findAll());

            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Role ROLE_ADMIN not found"));
            Role managerRole = roleRepository.findByName(ERole.ROLE_MANAGER)
                    .orElseThrow(() -> new RuntimeException("Role ROLE_MANAGER not found"));
            Role operatorRole = roleRepository.findByName(ERole.ROLE_OPERATOR)
                    .orElseThrow(() -> new RuntimeException("Role ROLE_OPERATOR not found"));

            RegisterRequest adminRequest = new RegisterRequest();
            adminRequest.setUsername("admin");
            adminRequest.setPassword("111111");
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);

            authService.registerForInit(adminRequest, adminRoles);
            System.out.println("Created user: " + adminRequest.toString());

            RegisterRequest managerRequest = new RegisterRequest();
            managerRequest.setUsername("manager");
            managerRequest.setPassword("111111");
            Set<Role> managerRoles = new HashSet<>();
            managerRoles.add(managerRole);
            authService.registerForInit(managerRequest, managerRoles);
            System.out.println("Created user: " + managerRequest.toString());

            RegisterRequest operatorRequest = new RegisterRequest();
            operatorRequest.setUsername("operator");
            operatorRequest.setPassword("111111");
            Set<Role> operatorRoles = new HashSet<>();
            operatorRoles.add(operatorRole);
            authService.registerForInit(operatorRequest, operatorRoles);
            System.out.println("Created user: " + operatorRequest.toString());

            Long zakazcount = 5L;
            Long subzakazcount = 5L;
            Long MaterialCount = 5L;

            // Создаем материалы
            for(int k=0; k<MaterialCount; k++){
                Material material = new Material();
                material.setName("Material"+k);
                material.setPrice(Integer.valueOf(100+k*10));
                materialRepository.save(material);
                System.out.println("Created material: " + material.getName()+" "+material.getId());
            }

            // Получаем всех пользователей
            List<User> users = userRepository.findAll();

            // Создаем заказы
            for (int i=0; i<zakazcount; i++){
                Zakaz zakaz = new Zakaz();
                zakaz.setCreatedAt(LocalDateTime.now());

                // Используем остаток от деления для выбора пользователя
                int userIndex = i % users.size();
                zakaz.setUser(users.get(userIndex));
                zakazRepository.save(zakaz);
                for (Long j=0L; j<subzakazcount; j++){
                    SubZakaz subZakaz = new SubZakaz();
                    subZakaz.setZakaz(zakaz);
                    subZakaz.setWidth(Double.valueOf(100+i*3));
                    subZakaz.setHeight(Double.valueOf(30+j*3));
                    subZakaz.setCena(Double.valueOf(i*10));

                    // Используем остаток от деления для выбора материала
                    Long materialIndex = 1L;
                    System.out.println("MaterialIndex: " + materialIndex);
                    if (materialIndex <6) {
                    subZakaz.setMaterial(materialRepository.findById(materialIndex)
                        .orElseThrow(() -> new RuntimeException("Material not found")));
                        materialIndex++;
                    }
                    else {
                        materialIndex = 1L;
                        subZakaz.setMaterial(materialRepository.findById(1L)
                        .orElseThrow(() -> new RuntimeException("Material not found")));
                    }
                    subZakazService.addSubZakaz(zakaz.getId(), subZakaz);
                    System.out.println("SubZakaz: " + subZakaz.getId()+ " добавлен к заказу: " + zakaz.getId() + " с материалом "+subZakaz.getMaterial());
                    subZakazRepository.save(subZakaz);
                }
                zakazRepository.save(zakaz);
            }
        };
    }
}


