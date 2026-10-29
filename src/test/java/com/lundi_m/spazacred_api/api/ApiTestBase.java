package com.lundi_m.spazacred_api.api;

import com.lundi_m.spazacred_api.auth.entity.Role;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.repository.RepositoryTestBase;
import com.lundi_m.spazacred_api.security.JwtService;
import com.lundi_m.spazacred_api.security.SpazaCredUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

public abstract class ApiTestBase extends RepositoryTestBase {

    protected static final String PASSWORD = "Password08!";
    private static final AtomicInteger USER_COUNTER = new AtomicInteger(1);
    private static volatile String encodedPassword;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    protected MockMvc mockMvc;
    protected SpazaCredUser admin;
    protected SpazaCredUser owner;
    protected SpazaCredUser employee;

    @BeforeEach
    void setUpApi() {

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        admin = user(Role.ADMIN);
        owner = user(Role.SHOP_OWNER);
        employee = user(Role.EMPLOYEE);
    }

    protected SpazaCredUser user(Role role) {
        if (encodedPassword == null) {
            encodedPassword = passwordEncoder.encode(PASSWORD);
        }
        int n = USER_COUNTER.getAndIncrement();
        return userRepository.save(new SpazaCredUser(
                "Api", null, role.name(), "api" + n + "@test.local", encodedPassword, role));
    }

    protected String bearer(SpazaCredUser user) {
        return "Bearer " + jwtService.generateToken(new SpazaCredUserDetails(user));
    }
}