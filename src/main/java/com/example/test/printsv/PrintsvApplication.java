package com.example.test.printsv;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PrintsvApplication implements CommandLineRunner {

	public static void main(String[] args) throws Exception {

		SpringApplication.run(PrintsvApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		for (int i=1;i<10;i++){

		}
	}
}
