package com.example.test.printsv;

import com.example.test.printsv.entity.ERole;
import com.example.test.printsv.entity.Role;
import com.example.test.printsv.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PrintsvApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(PrintsvApplication.class, args);
	}
//	@Bean
//	CommandLineRunner cleanDatabase(JdbcTemplate jdbcTemplate) {
//		return args -> {
//			jdbcTemplate.execute("ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS FKh8ciramu9cc9q3qcqiv4ue8a6");
//			jdbcTemplate.execute("DELETE FROM user_roles");
//			jdbcTemplate.execute("DELETE FROM users");
//			jdbcTemplate.execute("DELETE FROM roles");
//		};
//	}

	@Bean
	CommandLineRunner initRoles(RoleRepository roleRepository) {
		return args -> {
			if (!roleRepository.existsByName(ERole.ROLE_MANAGER)) {
				roleRepository.save(new Role(null, ERole.ROLE_MANAGER));
			}
			if (!roleRepository.existsByName(ERole.ROLE_ADMIN)) {
				roleRepository.save(new Role(null, ERole.ROLE_ADMIN));
			}
			if (!roleRepository.existsByName(ERole.ROLE_OPERATOR)) {
				roleRepository.save(new Role(null, ERole.ROLE_OPERATOR));
			}
		};
	}

//	@Bean
//	CommandLineRunner initUserAdmin(AuthService authService, RoleRepository roleRepository) {
//		return args -> {
//			RegisterRequest request = new RegisterRequest();
//
//			request.setUsername("vas");
//			request.setPassword("1");
//			Set<Role> roles = new HashSet<>();
//			roles.add(new Role(null, ERole.ROLE_ADMIN));
//			request.setRoles(roles);
//			authService.registerForInit(request);
//			System.out.println(request);
//		};
//	}
//
//	@Bean
//    CommandLineRunner initUserManager(AuthService authService, RequestToViewNameTranslator requestToViewNameTranslator, RoleRepository roleRepository, RequestService requestBuilder) {
//		return args -> {
//			for (int i = 0; i < 10; i++) {
//				RegisterRequest request1 = new RegisterRequest();
//				request1.setUsername("vas" + i);
//				request1.setPassword("1");
//				Set<Role> roles = new HashSet<>();
//				roles.add(new Role(null, ERole.ROLE_OPERATOR));
//				request1.setRoles(roles);
//				authService.registerForInit(request1);
//				System.out.println(request1);
//			}
//		};
//	}
}
