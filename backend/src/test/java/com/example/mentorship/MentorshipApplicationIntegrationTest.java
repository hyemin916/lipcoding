package com.example.mentorship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
public class MentorshipApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        // Clear any existing users
        User.USERS.clear();
        User.USERS_BY_EMAIL.clear();
        User.MATCH_REQUESTS.clear();
    }

    @Test
    public void testSignupEndpoint() throws Exception {
        // Test signup with valid data
        Map<String, Object> signup = new HashMap<>();
        signup.put("email", "newuser@example.com");
        signup.put("password", "password123");
        signup.put("name", "New User");
        signup.put("role", "mentee");

        mockMvc.perform(post("/api/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated());

        // Test signup with duplicate email
        mockMvc.perform(post("/api/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }
    
    @Test
    public void testLoginEndpoint() throws Exception {
        // Create a user for testing
        User user = new User();
        user.setEmail("testuser@example.com");
        user.setPassword("password123");
        user.setName("Test User");
        user.setRole("mentee");
        User.save(user);

        // Test login with valid credentials
        Map<String, String> login = new HashMap<>();
        login.put("email", "testuser@example.com");
        login.put("password", "password123");

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @WithMockUser(username = "mentor@example.com", authorities = {"mentor"})
    public void testGetMeEndpoint() throws Exception {
        // 1. 테스트 사용자 생성 (멘토)
        User mentor = new User();
        mentor.setEmail("mentor@example.com");
        mentor.setPassword("password123");
        mentor.setName("Test Mentor");
        mentor.setRole("mentor");
        mentor.setBio("Experienced Java mentor");
        mentor.getSkills().add("Java");
        mentor.getSkills().add("Spring Boot");
        User.save(mentor);
        
        // 테스트 - 내 정보 조회 API
        mockMvc.perform(get("/api/me")
                .requestAttr("userId", mentor.getId())) // userId 속성 추가
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mentor.getId()))
                .andExpect(jsonPath("$.email").value(mentor.getEmail()))
                .andExpect(jsonPath("$.role").value(mentor.getRole()))
                .andExpect(jsonPath("$.profile.name").value(mentor.getName()))
                .andExpect(jsonPath("$.profile.bio").value(mentor.getBio()))
                .andExpect(jsonPath("$.profile.skills[0]").exists());
    }
}
