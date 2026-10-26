package com.example.hms.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties="careflow.demo-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginShouldReturnCorrelationIdHeader() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void refreshTokenShouldRotateAndInvalidateOldRefreshToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginData = readData(loginResult);
        String refreshToken = loginData.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshData = readData(refreshResult);
        String rotatedRefresh = refreshData.get("refreshToken").asText();

        if (refreshToken.equals(rotatedRefresh)) {
            throw new AssertionError("Refresh token was not rotated");
        }

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutShouldBlacklistAccessAndRefreshTokens() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginData = readData(loginResult);
        String accessToken = loginData.get("accessToken").asText();
        String refreshToken = loginData.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/patients")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanPromoteUserToDoctorAndDoctorProfileIsCreated() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = readData(loginResult).get("accessToken").asText();

        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "doctor.promote@example.com",
                                  "password": "doctor123",
                                  "role": "RECEPTIONIST"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult usersResult = mockMvc.perform(get("/api/auth/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode users = readData(usersResult);
        Long userId = null;
        for (JsonNode user : users) {
            if ("doctor.promote@example.com".equals(user.get("username").asText())) {
                userId = user.get("id").asLong();
                break;
            }
        }

        if (userId == null) {
            throw new AssertionError("Created user was not returned by /api/auth/users");
        }

        mockMvc.perform(put("/api/auth/users/{userId}/role", userId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "DOCTOR",
                                  "fullName": "Dr Promote User",
                                  "specialization": "General Medicine",
                                  "email": "doctor.promote@example.com"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult doctorsResult = mockMvc.perform(get("/api/doctors")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode doctors = readData(doctorsResult);
        boolean doctorCreated = false;
        for (JsonNode doctor : doctors) {
            if ("doctor.promote@example.com".equals(doctor.get("email").asText())) {
                doctorCreated = true;
                break;
            }
        }

        if (!doctorCreated) {
            throw new AssertionError("Doctor profile was not created during promotion");
        }
    }

    private JsonNode readData(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data");
    }
}
