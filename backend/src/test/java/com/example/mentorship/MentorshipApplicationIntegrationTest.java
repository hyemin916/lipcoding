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

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    @WithMockUser(username = "user@example.com", authorities = {"mentor"})
    public void testUpdateProfileEndpoint() throws Exception {
        // 1. 테스트 사용자 생성
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("password123");
        user.setName("Original Name");
        user.setRole("mentor");
        user.setBio("Original bio");
        User.save(user);
        
        // 2. 프로필 업데이트 요청 데이터 준비
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("id", user.getId());
        updateData.put("name", "Updated Name");
        updateData.put("role", "mentor");
        updateData.put("bio", "Updated bio information");
        updateData.put("image", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="); // 샘플 Base64 이미지 데이터
        
        List<String> skills = new ArrayList<>();
        skills.add("Java");
        skills.add("Spring Boot");
        updateData.put("skills", skills);
        
        // 3. 프로필 업데이트 요청 및 검증
        mockMvc.perform(put("/api/profile")
                .requestAttr("userId", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.role").value("mentor"))
                .andExpect(jsonPath("$.profile.name").value("Updated Name"))
                .andExpect(jsonPath("$.profile.bio").value("Updated bio information"))
                .andExpect(jsonPath("$.profile.imageUrl").exists())
                .andExpect(jsonPath("$.profile.skills[0]").value("Java"))
                .andExpect(jsonPath("$.profile.skills[1]").value("Spring Boot"));
        
        // 4. 실제로 업데이트 되었는지 확인
        User updatedUser = User.findById(user.getId());
        assert "Updated Name".equals(updatedUser.getName());
        assert "Updated bio information".equals(updatedUser.getBio());
        assert updatedUser.getSkills().contains("Java");
    }

    @Test
    @WithMockUser(username = "mentee@example.com", authorities = {"mentee"})
    public void testGetMentorsEndpoint() throws Exception {
        // 1. 테스트용 멘토 데이터 생성
        User mentor1 = new User();
        mentor1.setEmail("mentor1@example.com");
        mentor1.setPassword("password123");
        mentor1.setName("Mentor A");
        mentor1.setRole("mentor");
        mentor1.setBio("Java Expert");
        mentor1.getSkills().add("Java");
        mentor1.getSkills().add("Spring");
        User.save(mentor1);
        
        User mentor2 = new User();
        mentor2.setEmail("mentor2@example.com");
        mentor2.setPassword("password123");
        mentor2.setName("Mentor B");
        mentor2.setRole("mentor");
        mentor2.setBio("React Expert");
        mentor2.getSkills().add("React");
        mentor2.getSkills().add("JavaScript");
        User.save(mentor2);
        
        // 2. 테스트용 멘티 생성 (현재 사용자)
        User mentee = new User();
        mentee.setEmail("mentee@example.com");
        mentee.setPassword("password123");
        mentee.setName("Test Mentee");
        mentee.setRole("mentee");
        User.save(mentee);
        
        // 3. 전체 멘토 목록 조회 테스트
        mockMvc.perform(get("/api/mentors")
                .requestAttr("userId", mentee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("mentor"))
                .andExpect(jsonPath("$[1].role").value("mentor"))
                .andExpect(jsonPath("$[?(@.email == 'mentor1@example.com')]").exists())
                .andExpect(jsonPath("$[?(@.email == 'mentor2@example.com')]").exists());
        
        // 4. 스킬로 필터링 테스트
        mockMvc.perform(get("/api/mentors")
                .param("skill", "Java")
                .requestAttr("userId", mentee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("mentor1@example.com"))
                .andExpect(jsonPath("$[0].profile.skills[?(@=='Java')]").exists());
        
        // 5. 이름으로 정렬 테스트
        mockMvc.perform(get("/api/mentors")
                .param("order_by", "name")
                .requestAttr("userId", mentee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profile.name").value("Mentor A"))
                .andExpect(jsonPath("$[1].profile.name").value("Mentor B"));
        
        // 6. 스킬로 정렬 테스트
        mockMvc.perform(get("/api/mentors")
                .param("order_by", "skill")
                .requestAttr("userId", mentee.getId()))
                .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(username = "mentee@example.com", authorities = {"mentee"})
    public void testCreateMatchRequestEndpoint() throws Exception {
        // 1. 테스트용 멘토 생성
        User mentor = new User();
        mentor.setEmail("mentor@example.com");
        mentor.setPassword("password123");
        mentor.setName("Test Mentor");
        mentor.setRole("mentor");
        User.save(mentor);
        
        // 2. 테스트용 멘티 생성
        User mentee = new User();
        mentee.setEmail("mentee@example.com");
        mentee.setPassword("password123");
        mentee.setName("Test Mentee");
        mentee.setRole("mentee");
        User.save(mentee);
        
        // 3. 매칭 요청 데이터 준비
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("mentorId", mentor.getId());
        requestData.put("menteeId", mentee.getId());
        requestData.put("message", "멘토링 요청합니다.");
        
        // 4. 매칭 요청 생성 API 호출 및 검증
        mockMvc.perform(post("/api/match-requests")
                .requestAttr("userId", mentee.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mentorId").value(mentor.getId()))
                .andExpect(jsonPath("$.menteeId").value(mentee.getId()))
                .andExpect(jsonPath("$.message").value("멘토링 요청합니다."))
                .andExpect(jsonPath("$.status").value("pending"));
        
        // 5. 매칭 요청이 실제로 생성되었는지 확인
        List<User.MatchRequest> outgoingRequests = User.findOutgoingMatchRequests(mentee.getId());
        assert !outgoingRequests.isEmpty();
        assert outgoingRequests.get(0).getMentorId().equals(mentor.getId());
        assert "pending".equals(outgoingRequests.get(0).getStatus());
    }
    
    @Test
    @WithMockUser(username = "mentor@example.com", authorities = {"mentor"})
    public void testGetIncomingMatchRequestsEndpoint() throws Exception {
        // 1. 테스트용 멘토 생성
        User mentor = new User();
        mentor.setEmail("mentor@example.com");
        mentor.setPassword("password123");
        mentor.setName("Test Mentor");
        mentor.setRole("mentor");
        User.save(mentor);
        
        // 2. 테스트용 멘티 생성
        User mentee1 = new User();
        mentee1.setEmail("mentee1@example.com");
        mentee1.setPassword("password123");
        mentee1.setName("Test Mentee 1");
        mentee1.setRole("mentee");
        User.save(mentee1);
        
        User mentee2 = new User();
        mentee2.setEmail("mentee2@example.com");
        mentee2.setPassword("password123");
        mentee2.setName("Test Mentee 2");
        mentee2.setRole("mentee");
        User.save(mentee2);
        
        // 3. 매칭 요청 생성
        User.MatchRequest request1 = new User.MatchRequest();
        request1.setMentorId(mentor.getId());
        request1.setMenteeId(mentee1.getId());
        request1.setMessage("첫 번째 멘토링 요청");
        request1.setStatus("pending");
        User.saveMatchRequest(request1);
        
        User.MatchRequest request2 = new User.MatchRequest();
        request2.setMentorId(mentor.getId());
        request2.setMenteeId(mentee2.getId());
        request2.setMessage("두 번째 멘토링 요청");
        request2.setStatus("pending");
        User.saveMatchRequest(request2);
        
        // 4. 들어온 매칭 요청 목록 조회 API 호출 및 검증
        mockMvc.perform(get("/api/match-requests/incoming")
                .requestAttr("userId", mentor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mentorId").value(mentor.getId()))
                .andExpect(jsonPath("$[1].mentorId").value(mentor.getId()))
                .andExpect(jsonPath("$[?(@.menteeId == " + mentee1.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.menteeId == " + mentee2.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.message == '첫 번째 멘토링 요청')]").exists())
                .andExpect(jsonPath("$[?(@.message == '두 번째 멘토링 요청')]").exists())
                .andExpect(jsonPath("$[0].status").value("pending"))
                .andExpect(jsonPath("$[1].status").value("pending"));
    }
    
    @Test
    @WithMockUser(username = "mentee@example.com", authorities = {"mentee"})
    public void testGetOutgoingMatchRequestsEndpoint() throws Exception {
        // 1. 테스트용 멘토 생성
        User mentor1 = new User();
        mentor1.setEmail("mentor1@example.com");
        mentor1.setPassword("password123");
        mentor1.setName("Test Mentor 1");
        mentor1.setRole("mentor");
        User.save(mentor1);
        
        User mentor2 = new User();
        mentor2.setEmail("mentor2@example.com");
        mentor2.setPassword("password123");
        mentor2.setName("Test Mentor 2");
        mentor2.setRole("mentor");
        User.save(mentor2);
        
        // 2. 테스트용 멘티 생성
        User mentee = new User();
        mentee.setEmail("mentee@example.com");
        mentee.setPassword("password123");
        mentee.setName("Test Mentee");
        mentee.setRole("mentee");
        User.save(mentee);
        
        // 3. 매칭 요청 생성
        User.MatchRequest request1 = new User.MatchRequest();
        request1.setMentorId(mentor1.getId());
        request1.setMenteeId(mentee.getId());
        request1.setMessage("첫 번째 멘토에게 요청");
        request1.setStatus("pending");
        User.saveMatchRequest(request1);
        
        User.MatchRequest request2 = new User.MatchRequest();
        request2.setMentorId(mentor2.getId());
        request2.setMenteeId(mentee.getId());
        request2.setMessage("두 번째 멘토에게 요청");
        request2.setStatus("accepted");
        User.saveMatchRequest(request2);
        
        // 4. 보낸 매칭 요청 목록 조회 API 호출 및 검증
        mockMvc.perform(get("/api/match-requests/outgoing")
                .requestAttr("userId", mentee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].menteeId").value(mentee.getId()))
                .andExpect(jsonPath("$[1].menteeId").value(mentee.getId()))
                .andExpect(jsonPath("$[?(@.mentorId == " + mentor1.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.mentorId == " + mentor2.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.status == 'pending')]").exists())
                .andExpect(jsonPath("$[?(@.status == 'accepted')]").exists());
    }
    
    @Test
    @WithMockUser(username = "mentor@example.com", authorities = {"mentor"})
    public void testAcceptMatchRequestEndpoint() throws Exception {
        // 1. 테스트용 멘토 생성
        User mentor = new User();
        mentor.setEmail("mentor@example.com");
        mentor.setPassword("password123");
        mentor.setName("Test Mentor");
        mentor.setRole("mentor");
        User.save(mentor);
        
        // 2. 테스트용 멘티 생성
        User mentee = new User();
        mentee.setEmail("mentee@example.com");
        mentee.setPassword("password123");
        mentee.setName("Test Mentee");
        mentee.setRole("mentee");
        User.save(mentee);
        
        // 3. 매칭 요청 생성
        User.MatchRequest request = new User.MatchRequest();
        request.setMentorId(mentor.getId());
        request.setMenteeId(mentee.getId());
        request.setMessage("멘토링 요청합니다.");
        request.setStatus("pending");
        User.saveMatchRequest(request);
        
        // 4. 매칭 요청 수락 API 호출 및 검증
        mockMvc.perform(put("/api/match-requests/" + request.getId() + "/accept")
                .requestAttr("userId", mentor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId()))
                .andExpect(jsonPath("$.mentorId").value(mentor.getId()))
                .andExpect(jsonPath("$.menteeId").value(mentee.getId()))
                .andExpect(jsonPath("$.status").value("accepted"));
        
        // 5. 실제로 수락 처리되었는지 확인
        User.MatchRequest updatedRequest = User.findMatchRequestById(request.getId());
        assert "accepted".equals(updatedRequest.getStatus());
    }
    
    @Test
    @WithMockUser(username = "mentor@example.com", authorities = {"mentor"})
    public void testRejectMatchRequestEndpoint() throws Exception {
        // 1. 테스트용 멘토 생성
        User mentor = new User();
        mentor.setEmail("mentor@example.com");
        mentor.setPassword("password123");
        mentor.setName("Test Mentor");
        mentor.setRole("mentor");
        User.save(mentor);
        
        // 2. 테스트용 멘티 생성
        User mentee = new User();
        mentee.setEmail("mentee@example.com");
        mentee.setPassword("password123");
        mentee.setName("Test Mentee");
        mentee.setRole("mentee");
        User.save(mentee);
        
        // 3. 매칭 요청 생성
        User.MatchRequest request = new User.MatchRequest();
        request.setMentorId(mentor.getId());
        request.setMenteeId(mentee.getId());
        request.setMessage("멘토링 요청합니다.");
        request.setStatus("pending");
        User.saveMatchRequest(request);
        
        // 4. 매칭 요청 거절 API 호출 및 검증
        mockMvc.perform(put("/api/match-requests/" + request.getId() + "/reject")
                .requestAttr("userId", mentor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId()))
                .andExpect(jsonPath("$.mentorId").value(mentor.getId()))
                .andExpect(jsonPath("$.menteeId").value(mentee.getId()))
                .andExpect(jsonPath("$.status").value("rejected"));
        
        // 5. 실제로 거절 처리되었는지 확인
        User.MatchRequest updatedRequest = User.findMatchRequestById(request.getId());
        assert "rejected".equals(updatedRequest.getStatus());
    }
    
    @Test
    @WithMockUser(username = "mentee@example.com", authorities = {"mentee"})
    public void testDeleteMatchRequestEndpoint() throws Exception {
        // 1. 테스트용 멘토 생성
        User mentor = new User();
        mentor.setEmail("mentor@example.com");
        mentor.setPassword("password123");
        mentor.setName("Test Mentor");
        mentor.setRole("mentor");
        User.save(mentor);
        
        // 2. 테스트용 멘티 생성
        User mentee = new User();
        mentee.setEmail("mentee@example.com");
        mentee.setPassword("password123");
        mentee.setName("Test Mentee");
        mentee.setRole("mentee");
        User.save(mentee);
        
        // 3. 매칭 요청 생성
        User.MatchRequest request = new User.MatchRequest();
        request.setMentorId(mentor.getId());
        request.setMenteeId(mentee.getId());
        request.setMessage("멘토링 요청합니다.");
        request.setStatus("pending");
        User.saveMatchRequest(request);
        
        // 4. 매칭 요청 삭제 API 호출 및 검증
        mockMvc.perform(delete("/api/match-requests/" + request.getId())
                .requestAttr("userId", mentee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId()))
                .andExpect(jsonPath("$.status").value("cancelled"));
        
        // 5. 실제로 취소 처리되었는지 확인
        User.MatchRequest updatedRequest = User.findMatchRequestById(request.getId());
        assert "cancelled".equals(updatedRequest.getStatus());
    }
    
    @Test
    @WithMockUser(username = "user@example.com", authorities = {"mentee"})
    public void testGetProfileImageEndpoint() throws Exception {
        // 1. 테스트용 멘토 생성 (이미지 포함)
        User mentor = new User();
        mentor.setEmail("mentor@example.com");
        mentor.setPassword("password123");
        mentor.setName("Test Mentor");
        mentor.setRole("mentor");
        
        // 샘플 이미지 데이터 설정
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
        mentor.setImageData(Base64.getDecoder().decode(base64Image));
        User.save(mentor);
        
        // 2. 이미지 조회 API 호출 및 검증
        mockMvc.perform(get("/api/images/mentor/" + mentor.getId())
                .requestAttr("userId", mentor.getId()))
                .andExpect(status().isOk());
    }
}
