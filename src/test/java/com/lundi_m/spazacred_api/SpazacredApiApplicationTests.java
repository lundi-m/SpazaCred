package com.lundi_m.spazacred_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestContainersConfiguration.class)
@SpringBootTest(properties = "jwt.secret=test-only-secret-key-that-is-at-least-32-bytes-long")
class SpazacredApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
