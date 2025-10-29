package com.example.test.printsv;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.test.printsv.entity.ERole;
import com.example.test.printsv.entity.Role;
import com.example.test.printsv.repository.RoleRepository;
import com.example.test.printsv.request.RegisterRequest;
import com.example.test.printsv.service.AuthService;

@SpringBootApplication
public class PrintsvApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(PrintsvApplication.class, args);
	}

	// @Bean
    // @Transactional
	// CommandLineRunner initRoles(RoleRepository roleRepository) {
    //     System.out.println("initialize roles...");
	// 	return args -> {
	// 		// if (!roleRepository.existsByName(ERole.ROLE_MANAGER)) {
	// 		// 	roleRepository.save(new Role(null, ERole.ROLE_MANAGER));
    //         //     System.out.println("ROLE_MANAGER saved");
	// 		// }
	// 		// if (!roleRepository.existsByName(ERole.ROLE_ADMIN)) {
	// 		// 	roleRepository.save(new Role(null, ERole.ROLE_ADMIN));
    //         //     System.out.println("ROLE_ADMIN saved");
	// 		// }
	// 		// if (!roleRepository.existsByName(ERole.ROLE_OPERATOR)) {
	// 		// 	roleRepository.save(new Role(null, ERole.ROLE_OPERATOR));
    //         //     System.out.println("ROLE_OPERATOR saved");
	// 		// }
    //         // System.out.println("Roles in database: " + roleRepository.findAll());
	// 	};
	// }

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
            // System.out.println(Arrays.asList(adminRoles));

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
        };
    }
}

