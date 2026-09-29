package com.icastar.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icastar.platform.entity.ArtistType;
import com.icastar.platform.entity.User;
import com.icastar.platform.repository.ArtistTypeRepository;
import com.icastar.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerRegisterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArtistTypeRepository artistTypeRepository;

    @BeforeEach
    void setUp() {
        // Clean up users with test emails before each test
        userRepository.findByEmail("testuser@example.com")
                .ifPresent(user -> userRepository.delete(user));

        // Ensure OTHER artist type exists for artist registration
        if (artistTypeRepository.findByName("OTHER").isEmpty()) {
            ArtistType otherType = new ArtistType();
            otherType.setName("OTHER");
            otherType.setDisplayName("Other");
            otherType.setDescription("Other artist type");
            artistTypeRepository.save(otherType);
        }
    }

    @Test
    void register_withAcceptedTermsFalse_returns400() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", "testuser@example.com");
        request.put("mobile", "9876543210");
        request.put("password", "password123");
        request.put("role", "ARTIST");
        request.put("firstName", "Test");
        request.put("lastName", "User");
        request.put("acceptedTerms", false);

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You must accept the Terms & Conditions and Privacy Policy"))
                .andReturn();

        // Verify user was not created
        Optional<User> user = userRepository.findByEmail("testuser@example.com");
        assertFalse(user.isPresent(), "User should not be created when terms not accepted");
    }

    @Test
    void register_withAcceptedTermsMissing_returns400() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", "testuser@example.com");
        request.put("mobile", "9876543210");
        request.put("password", "password123");
        request.put("role", "ARTIST");
        request.put("firstName", "Test");
        request.put("lastName", "User");
        // acceptedTerms is NOT included

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You must accept the Terms & Conditions and Privacy Policy"))
                .andReturn();

        // Verify user was not created
        Optional<User> user = userRepository.findByEmail("testuser@example.com");
        assertFalse(user.isPresent(), "User should not be created when acceptedTerms is missing");
    }

    @Test
    void register_withAcceptedTermsTrue_returns200AndSavesTermsInfo() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", "testuser@example.com");
        request.put("mobile", "9876543210");
        request.put("password", "password123");
        request.put("role", "ARTIST");
        request.put("firstName", "Test");
        request.put("lastName", "User");
        request.put("acceptedTerms", true);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("testuser@example.com"));

        // Verify user was created with terms info
        Optional<User> userOpt = userRepository.findByEmail("testuser@example.com");
        assertTrue(userOpt.isPresent(), "User should be created");

        User user = userOpt.get();
        assertNotNull(user.getTermsAcceptedAt(), "termsAcceptedAt should be set");
        assertNotNull(user.getTermsVersion(), "termsVersion should be set");
        assertEquals("2025-12", user.getTermsVersion(), "termsVersion should match configured version");
    }
}