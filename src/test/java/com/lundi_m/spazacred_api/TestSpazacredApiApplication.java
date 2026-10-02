package com.lundi_m.spazacred_api;

import org.springframework.boot.SpringApplication;

public class TestSpazacredApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpazacredApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
