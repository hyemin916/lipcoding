package com.example.mentorship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = MentorshipApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MentorshipApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String mentorToken;
    private String menteeToken;
    private Integer mentorId;
    private Integer menteeId;
    private Integer matchRequestId;

    @BeforeEach
    public void setup() throws Exception {
        // Clear any existing users
        User.USERS.clear();
        User.USERS_BY_EMAIL.clear();
        User.MATCH_REQUESTS.clear();

        // Create test users and get tokens
        registerAndLoginUsers();
    }

    private void registerAndLoginUsers() throws Exception {
        // Register mentor
        Map<String, Object> mentorSignup = new HashMap<>();
        mentorSignup.put("email", "mentor@example.com");
        mentorSignup.put("password", "password123");
        mentorSignup.put("name", "Test Mentor");
        mentorSignup.put("role", "mentor");

        mockMvc.perform(post("/api/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mentorSignup)))
                .andExpect(status().isCreated());

        // Login mentor
        Map<String, String> mentorLogin = new HashMap<>();
        mentorLogin.put("email", "mentor@example.com");
        mentorLogin.put("password", "password123");

        MvcResult mentorResult = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mentorLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        Map<String, String> mentorResponse = objectMapper.readValue(
                mentorResult.getResponse().getContentAsString(), 
                Map.class);
        mentorToken = mentorResponse.get("token");

        // Get mentor ID
        MvcResult mentorMeResult = mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andReturn();
        
        Map<String, Object> mentorMe = objectMapper.readValue(
                mentorMeResult.getResponse().getContentAsString(), 
                Map.class);
        mentorId = (Integer) mentorMe.get("id");

        // Register mentee
        Map<String, Object> menteeSignup = new HashMap<>();
        menteeSignup.put("email", "mentee@example.com");
        menteeSignup.put("password", "password123");
        menteeSignup.put("name", "Test Mentee");
        menteeSignup.put("role", "mentee");

        mockMvc.perform(post("/api/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(menteeSignup)))
                .andExpect(status().isCreated());

        // Login mentee
        Map<String, String> menteeLogin = new HashMap<>();
        menteeLogin.put("email", "mentee@example.com");
        menteeLogin.put("password", "password123");

        MvcResult menteeResult = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(menteeLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        Map<String, String> menteeResponse = objectMapper.readValue(
                menteeResult.getResponse().getContentAsString(), 
                Map.class);
        menteeToken = menteeResponse.get("token");
        
        // Get mentee ID
        MvcResult menteeMeResult = mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer " + menteeToken))
                .andExpect(status().isOk())
                .andReturn();
        
        Map<String, Object> menteeMe = objectMapper.readValue(
                menteeMeResult.getResponse().getContentAsString(), 
                Map.class);
        menteeId = (Integer) menteeMe.get("id");
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

        // Test signup with invalid role
        signup.put("email", "anotheruser@example.com");
        signup.put("role", "invalid");

        mockMvc.perform(post("/api/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLoginEndpoint() throws Exception {
        // Test login with valid credentials
        Map<String, String> login = new HashMap<>();
        login.put("email", "mentor@example.com");
        login.put("password", "password123");

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // Test login with invalid password
        login.put("password", "wrongpassword");

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());

        // Test login with non-existent email
        login.put("email", "nonexistent@example.com");

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetCurrentUserEndpoint() throws Exception {
        // Test get current user with valid token
        mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("mentor@example.com"))
                .andExpect(jsonPath("$.role").value("mentor"))
                .andExpect(jsonPath("$.profile").exists());

        // Test get current user with invalid token
        mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateProfileEndpoint() throws Exception {
        // Update mentor profile
        Map<String, Object> mentorProfile = new HashMap<>();
        mentorProfile.put("id", mentorId);
        mentorProfile.put("name", "Updated Mentor");
        mentorProfile.put("role", "mentor");
        mentorProfile.put("bio", "I am a mentor");
        
        List<String> skills = new ArrayList<>();
        skills.add("Java");
        skills.add("Spring");
        mentorProfile.put("skills", skills);

        mockMvc.perform(put("/api/profile")
                .header("Authorization", "Bearer " + mentorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mentorProfile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.name").value("Updated Mentor"))
                .andExpect(jsonPath("$.profile.bio").value("I am a mentor"))
                .andExpect(jsonPath("$.profile.skills[0]").value("Java"))
                .andExpect(jsonPath("$.profile.skills[1]").value("Spring"));

        // Update mentee profile
        Map<String, Object> menteeProfile = new HashMap<>();
        menteeProfile.put("id", menteeId);
        menteeProfile.put("name", "Updated Mentee");
        menteeProfile.put("role", "mentee");
        menteeProfile.put("bio", "I am a mentee");

        mockMvc.perform(put("/api/profile")
                .header("Authorization", "Bearer " + menteeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(menteeProfile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.name").value("Updated Mentee"))
                .andExpect(jsonPath("$.profile.bio").value("I am a mentee"));

        // Try to update another user's profile (should fail)
        menteeProfile.put("id", mentorId);

        mockMvc.perform(put("/api/profile")
                .header("Authorization", "Bearer " + menteeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(menteeProfile)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetMentorsEndpoint() throws Exception {
        // Update mentor profile with skills
        Map<String, Object> mentorProfile = new HashMap<>();
        mentorProfile.put("id", mentorId);
        mentorProfile.put("name", "Java Mentor");
        mentorProfile.put("role", "mentor");
        mentorProfile.put("bio", "I teach Java");
        
        List<String> skills = new ArrayList<>();
        skills.add("Java");
        skills.add("Spring");
        mentorProfile.put("skills", skills);

        mockMvc.perform(put("/api/profile")
                .header("Authorization", "Bearer " + mentorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mentorProfile)))
                .andExpect(status().isOk());

        // Get all mentors
        mockMvc.perform(get("/api/mentors")
                .header("Authorization", "Bearer " + menteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].profile.name").value("Java Mentor"));

        // Get mentors filtered by skill
        mockMvc.perform(get("/api/mentors?skill=Java")
                .header("Authorization", "Bearer " + menteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].profile.skills", hasItem("Java")));

        // Get mentors sorted by name
        mockMvc.perform(get("/api/mentors?order_by=name")
                .header("Authorization", "Bearer " + menteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // Get mentors sorted by skill
        mockMvc.perform(get("/api/mentors?order_by=skill")
                .header("Authorization", "Bearer " + menteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // Mentors endpoint should be accessible only by mentees
        mockMvc.perform(get("/api/mentors")
                .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testMatchRequestFlow() throws Exception {
        // 1. Create match request (mentee to mentor)
        Map<String, Object> matchRequest = new HashMap<>();
        matchRequest.put("mentorId", mentorId);
        matchRequest.put("menteeId", menteeId);
        matchRequest.put("message", "I want to learn from you");

        MvcResult createResult = mockMvc.perform(post("/api/match-requests")
                .header("Authorization", "Bearer " + menteeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mentorId").value(mentorId))
                .andExpect(jsonPath("$.menteeId").value(menteeId))
                .andExpect(jsonPath("$.message").value("I want to learn from you"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andReturn();

        Map<String, Object> createdRequest = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), 
                Map.class);
        matchRequestId = (Integer) createdRequest.get("id");

        // 2. Check incoming requests (mentor view)
        mockMvc.perform(get("/api/match-requests/incoming")
                .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(matchRequestId))
                .andExpect(jsonPath("$[0].status").value("pending"));

        // 3. Check outgoing requests (mentee view)
        mockMvc.perform(get("/api/match-requests/outgoing")
                .header("Authorization", "Bearer " + menteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(matchRequestId))
                .andExpect(jsonPath("$[0].status").value("pending"));

        // 4. Mentor accepts request
        mockMvc.perform(put("/api/match-requests/" + matchRequestId + "/accept")
                .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchRequestId))
                .andExpect(jsonPath("$.status").value("accepted"));

        // 5. Verify status updated in outgoing requests
        mockMvc.perform(get("/api/match-requests/outgoing")
                .header("Authorization", "Bearer " + menteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("accepted"));

        // 6. Create another match request with a new mentee
        // Register another mentee
        Map<String, Object> mentee2Signup = new HashMap<>();
        mentee2Signup.put("email", "mentee2@example.com");
        mentee2Signup.put("password", "password123");
        mentee2Signup.put("name", "Test Mentee 2");
        mentee2Signup.put("role", "mentee");

        mockMvc.perform(post("/api/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mentee2Signup)))
                .andExpect(status().isCreated());

        // Login mentee2
        Map<String, String> mentee2Login = new HashMap<>();
        mentee2Login.put("email", "mentee2@example.com");
        mentee2Login.put("password", "password123");

        MvcResult mentee2Result = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mentee2Login)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, String> mentee2Response = objectMapper.readValue(
                mentee2Result.getResponse().getContentAsString(), 
                Map.class);
        String mentee2Token = mentee2Response.get("token");
        
        // Get mentee2 ID
        MvcResult mentee2MeResult = mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer " + mentee2Token))
                .andExpect(status().isOk())
                .andReturn();
        
        Map<String, Object> mentee2Me = objectMapper.readValue(
                mentee2MeResult.getResponse().getContentAsString(), 
                Map.class);
        Integer mentee2Id = (Integer) mentee2Me.get("id");

        // Create another match request
        Map<String, Object> matchRequest2 = new HashMap<>();
        matchRequest2.put("mentorId", mentorId);
        matchRequest2.put("menteeId", mentee2Id);
        matchRequest2.put("message", "I want to learn from you too");

        MvcResult createResult2 = mockMvc.perform(post("/api/match-requests")
                .header("Authorization", "Bearer " + mentee2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matchRequest2)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> createdRequest2 = objectMapper.readValue(
                createResult2.getResponse().getContentAsString(), 
                Map.class);
        Integer matchRequestId2 = (Integer) createdRequest2.get("id");

        // 7. Mentor rejects this request
        mockMvc.perform(put("/api/match-requests/" + matchRequestId2 + "/reject")
                .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchRequestId2))
                .andExpect(jsonPath("$.status").value("rejected"));

        // 8. Mentee cancels their request
        // Create a third request from original mentee
        // First create a second mentor
        Map<String, Object> mentor2Signup = new HashMap<>();
        mentor2Signup.put("email", "mentor2@example.com");
        mentor2Signup.put("password", "password123");
        mentor2Signup.put("name", "Test Mentor 2");
        mentor2Signup.put("role", "mentor");

        mockMvc.perform(post("/api/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mentor2Signup)))
                .andExpect(status().isCreated());

        // Login mentor2
        Map<String, String> mentor2Login = new HashMap<>();
        mentor2Login.put("email", "mentor2@example.com");
        mentor2Login.put("password", "password123");

        MvcResult mentor2Result = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mentor2Login)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, String> mentor2Response = objectMapper.readValue(
                mentor2Result.getResponse().getContentAsString(), 
                Map.class);
        String mentor2Token = mentor2Response.get("token");
        
        // Get mentor2 ID
        MvcResult mentor2MeResult = mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer " + mentor2Token))
                .andExpect(status().isOk())
                .andReturn();
        
        Map<String, Object> mentor2Me = objectMapper.readValue(
                mentor2MeResult.getResponse().getContentAsString(), 
                Map.class);
        Integer mentor2Id = (Integer) mentor2Me.get("id");

        // Create request to second mentor
        Map<String, Object> matchRequest3 = new HashMap<>();
        matchRequest3.put("mentorId", mentor2Id);
        matchRequest3.put("menteeId", mentee2Id);
        matchRequest3.put("message", "I want to learn from you instead");

        MvcResult createResult3 = mockMvc.perform(post("/api/match-requests")
                .header("Authorization", "Bearer " + mentee2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matchRequest3)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> createdRequest3 = objectMapper.readValue(
                createResult3.getResponse().getContentAsString(), 
                Map.class);
        Integer matchRequestId3 = (Integer) createdRequest3.get("id");

        // Mentee cancels the request
        mockMvc.perform(delete("/api/match-requests/" + matchRequestId3)
                .header("Authorization", "Bearer " + mentee2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchRequestId3))
                .andExpect(jsonPath("$.status").value("cancelled"));
    }
}
