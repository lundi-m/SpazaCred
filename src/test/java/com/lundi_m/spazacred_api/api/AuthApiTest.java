package com.lundi_m.spazacred_api.api;

import com.lundi_m.spazacred_api.auth.entity.Role;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestBase {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String CHANGE_PASSWORD = "/api/v1/auth/change-password";
    private static final String AUTH = HttpHeaders.AUTHORIZATION;

    @Value("${jwt.secret}")
    private String jwtSecret;


    @Test
    void loginReturnsATokenTheRoleAndThePasswordFlag() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(JSON).content(loginJson(employee.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void aWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(JSON).content(loginJson(employee.getEmail(), "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void anUnknownEmailGetsTheSameAnswerSoNobodyCanProbeForAccounts() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(JSON).content(loginJson("nobody@test.local", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void aDisabledAccountCannotLogIn() throws Exception {
        employee.setEnabled(false);
        userRepository.saveAndFlush(employee);

        mockMvc.perform(post(LOGIN).contentType(JSON).content(loginJson(employee.getEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void aMalformedLoginRequestListsTheBadFields() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(JSON).content(loginJson("not-an-email", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }


    @Test
    void aRequestWithoutATokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void aGarbageTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/customers").header(AUTH, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void anExpiredTokenIsReportedAsExpired() throws Exception {
        Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        String expired = Jwts.builder()
                .subject(employee.getEmail())
                .issuedAt(Date.from(twoHoursAgo.minus(1, ChronoUnit.HOURS)))
                .expiration(Date.from(twoHoursAgo))
                .signWith(key(jwtSecret))
                .compact();

        mockMvc.perform(get("/api/v1/customers").header(AUTH, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void aTokenSignedWithAnotherKeyIsInvalid() throws Exception {
        String forged = Jwts.builder()
                .subject(employee.getEmail())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key("a-completely-different-secret-key-of-32-plus-bytes"))
                .compact();

        mockMvc.perform(get("/api/v1/customers").header(AUTH, "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void aTokenForADisabledUserStopsWorkingImmediately() throws Exception {
        String token = bearer(employee);
        employee.setEnabled(false);
        userRepository.saveAndFlush(employee);

        mockMvc.perform(get("/api/v1/customers").header(AUTH, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void aTokenForAUserThatDoesNotExistIsInvalid() throws Exception {
        SpazaCredUser ghost = new SpazaCredUser("Ghost", null, "User",
                "ghost@test.local", "hash", Role.ADMIN);

        mockMvc.perform(get("/api/v1/customers").header(AUTH, bearer(ghost)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void rolesComeFromTheDatabaseNotFromTheToken() throws Exception {
        String employeeToken = bearer(employee);
        mockMvc.perform(get("/api/v1/users").header(AUTH, employeeToken))
                .andExpect(status().isForbidden());

        employee.setRole(Role.ADMIN);
        userRepository.saveAndFlush(employee);

        mockMvc.perform(get("/api/v1/users").header(AUTH, employeeToken))
                .andExpect(status().isOk());
    }


    @Test
    void aUserWhoMustChangeTheirPasswordIsBlockedEverywhereElse() throws Exception {
        SpazaCredUser user = userWhoMustChangePassword();

        mockMvc.perform(get("/api/v1/customers").header(AUTH, bearer(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    void loginTellsTheClientThePasswordMustBeChanged() throws Exception {
        SpazaCredUser user = userWhoMustChangePassword();

        mockMvc.perform(post(LOGIN).contentType(JSON).content(loginJson(user.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    @Test
    void aWrongCurrentPasswordIsABadRequestNotALogout() throws Exception {
        SpazaCredUser user = userWhoMustChangePassword();

        mockMvc.perform(post(CHANGE_PASSWORD).header(AUTH, bearer(user)).contentType(JSON)
                        .content(changeJson("not-my-password", "N3wPassw0rd!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
    }

    @Test
    void theNewPasswordMustDifferFromTheOldOne() throws Exception {
        SpazaCredUser user = userWhoMustChangePassword();

        mockMvc.perform(post(CHANGE_PASSWORD).header(AUTH, bearer(user)).contentType(JSON)
                        .content(changeJson(PASSWORD, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_UNCHANGED"));
    }

    @Test
    void aTooShortNewPasswordIsRejected() throws Exception {
        SpazaCredUser user = userWhoMustChangePassword();

        mockMvc.perform(post(CHANGE_PASSWORD).header(AUTH, bearer(user)).contentType(JSON)
                        .content(changeJson(PASSWORD, "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void changingThePasswordUnlocksTheSameTokenAndReplacesTheOldPassword() throws Exception {
        SpazaCredUser user = userWhoMustChangePassword();
        String token = bearer(user);

        mockMvc.perform(post(CHANGE_PASSWORD).header(AUTH, token).contentType(JSON)
                        .content(changeJson(PASSWORD, "N3wPassw0rd!")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/customers").header(AUTH, token))
                .andExpect(status().isOk());
        mockMvc.perform(post(LOGIN).contentType(JSON).content(loginJson(user.getEmail(), "N3wPassw0rd!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
        mockMvc.perform(post(LOGIN).contentType(JSON).content(loginJson(user.getEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changingThePasswordNeedsALogin() throws Exception {
        mockMvc.perform(post(CHANGE_PASSWORD).contentType(JSON).content(changeJson(PASSWORD, "N3wPassw0rd!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }


    @Test
    void anAdminCreatesAUserWhoMustChangeThePasswordAtFirstLogin() throws Exception {
        mockMvc.perform(post("/api/v1/users").header(AUTH, bearer(admin)).contentType(JSON)
                        .content(newUserJson("new.staff@test.local", "Temp12345", "EMPLOYEE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.staff@test.local"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.temporaryPassword").doesNotExist());

        mockMvc.perform(post(LOGIN).contentType(JSON).content(loginJson("new.staff@test.local", "Temp12345")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    @Test
    void aDuplicateEmailIsAConflict() throws Exception {
        mockMvc.perform(post("/api/v1/users").header(AUTH, bearer(admin)).contentType(JSON)
                        .content(newUserJson(employee.getEmail(), "Temp12345", "EMPLOYEE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").isNotEmpty());
    }

    @Test
    void aTooShortTemporaryPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/users").header(AUTH, bearer(admin)).contentType(JSON)
                        .content(newUserJson("short@test.local", "short", "EMPLOYEE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.temporaryPassword").exists());
    }

    @Test
    void anUnknownRoleIsAMalformedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users").header(AUTH, bearer(admin)).contentType(JSON)
                        .content(newUserJson("boss@test.local", "Temp12345", "BOSS")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void onlyAnAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users").header(AUTH, bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users").header(AUTH, bearer(employee)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }


    private SpazaCredUser userWhoMustChangePassword() {
        SpazaCredUser user = user(Role.EMPLOYEE);
        user.setMustChangePassword(true);
        return userRepository.saveAndFlush(user);
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}""".formatted(email, password);
    }

    private String changeJson(String currentPassword, String newPassword) {
        return """
                {"currentPassword":"%s","newPassword":"%s"}""".formatted(currentPassword, newPassword);
    }

    private String newUserJson(String email, String temporaryPassword, String role) {
        return """
                {"firstName":"New","lastName":"Staff","email":"%s","temporaryPassword":"%s","role":"%s"}"""
                .formatted(email, temporaryPassword, role);
    }
}