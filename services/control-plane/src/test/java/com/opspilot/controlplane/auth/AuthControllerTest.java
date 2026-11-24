package com.opspilot.controlplane.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.controlplane.dto.auth.LoginRequest;
import com.opspilot.controlplane.dto.auth.RegisterRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Authentication API.
 * Tests registration, login, /me endpoint and duplicate slug detection.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Unique slug per test run to avoid H2 state bleeding between tests
    private static final String ORG_SLUG = "auth-test-org-" + System.nanoTime();
    private static String capturedToken;

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    void register_validRequest_returns201WithJwtAndOrgDetails() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .organizationName("Auth Test Organisation")
                .organizationSlug(ORG_SLUG)
                .adminEmail("admin@authtest.example.com")
                .adminPassword("Str0ng!Pass")
                .adminFullName("Auth Admin")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.organization.slug").value(ORG_SLUG))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.user.email").value("admin@authtest.example.com"))
                .andReturn();

        // Capture token for /me test
        String body = result.getResponse().getContentAsString();
        capturedToken = objectMapper.readTree(body).path("data").path("token").asText();
    }

    @Test
    @Order(2)
    void register_duplicateSlug_returns409Conflict() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .organizationName("Duplicate Org")
                .organizationSlug(ORG_SLUG)          // same slug
                .adminEmail("other@example.com")
                .adminPassword("AnotherPass1")
                .adminFullName("Other Admin")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(3)
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .organizationName("Bad Email Org")
                .organizationSlug("bad-email-org-" + System.nanoTime())
                .adminEmail("not-an-email")
                .adminPassword("Str0ng!Pass")
                .adminFullName("Admin")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    void login_validCredentials_returns200WithJwt() throws Exception {
        // Ensure the org and user exist (re-register with unique slug)
        String loginSlug = "login-test-" + System.nanoTime();
        RegisterRequest reg = RegisterRequest.builder()
                .organizationName("Login Test Org")
                .organizationSlug(loginSlug)
                .adminEmail("login@logintest.example.com")
                .adminPassword("MyPassword99")
                .adminFullName("Login User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .email("login@logintest.example.com")
                .password("MyPassword99")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("login@logintest.example.com"));
    }

    @Test
    @Order(5)
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest login = LoginRequest.builder()
                .email("admin@authtest.example.com")
                .password("WrongPassword!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(6)
    void login_nonExistentUser_returns401() throws Exception {
        LoginRequest login = LoginRequest.builder()
                .email("nobody@nowhere.com")
                .password("SomePassword1")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // /me
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    void getMe_withValidJwt_returns200WithUserInfo() throws Exception {
        // Use token from registration test; fall back to a fresh one if parallel execution
        String token = capturedToken;
        if (token == null || token.isBlank()) {
            // Re-register to obtain a token
            String fallbackSlug = "me-test-" + System.nanoTime();
            RegisterRequest reg = RegisterRequest.builder()
                    .organizationName("Me Test Org")
                    .organizationSlug(fallbackSlug)
                    .adminEmail("me@metest.example.com")
                    .adminPassword("MePassword1")
                    .adminFullName("Me Admin")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reg)))
                    .andReturn();
            token = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("token").asText();
        }

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @Order(8)
    void getMe_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    void getMe_withInvalidJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }
}
