package com.example.mentorship;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Single class containing all user-related data and operations.
 * Includes both Mentor and Mentee data models with in-memory storage.
 */
public class User {
    // Fields
    private Integer id;
    private String email;
    @JsonIgnore
    private String password;
    private String name;
    private String role; // "mentor" or "mentee"
    private String bio;
    private List<String> skills; // Only for mentors
    @JsonIgnore
    private byte[] imageData;

    // Static in-memory storage
    public static final ConcurrentHashMap<Integer, User> USERS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, User> USERS_BY_EMAIL = new ConcurrentHashMap<>();
    private static final AtomicInteger USER_ID_COUNTER = new AtomicInteger(1);
    
    // Match request storage
    public static final ConcurrentHashMap<Integer, MatchRequest> MATCH_REQUESTS = new ConcurrentHashMap<>();
    private static final AtomicInteger MATCH_REQUEST_ID_COUNTER = new AtomicInteger(1);

    // Constructors
    public User() {
        this.skills = new ArrayList<>();
    }

    // Static methods for data operations
    public static User save(User user) {
        if (user.getId() == null) {
            user.setId(USER_ID_COUNTER.getAndIncrement());
        }
        
        // Validate user data
        validateUser(user);
        
        // Store user
        USERS.put(user.getId(), user);
        USERS_BY_EMAIL.put(user.getEmail(), user);
        
        return user;
    }
    
    public static User findById(Integer id) {
        return USERS.get(id);
    }
    
    public static User findByEmail(String email) {
        return USERS_BY_EMAIL.get(email);
    }
    
    public static List<User> findMentors() {
        return USERS.values().stream()
                .filter(user -> "mentor".equals(user.getRole()))
                .collect(Collectors.toList());
    }
    
    public static List<User> findMentorsBySkill(String skill) {
        return USERS.values().stream()
                .filter(user -> "mentor".equals(user.getRole()))
                .filter(user -> user.getSkills() != null && 
                       user.getSkills().stream().anyMatch(s -> 
                           s.toLowerCase().contains(skill.toLowerCase())))
                .collect(Collectors.toList());
    }
    
    public static List<User> findMentorsSortedByName() {
        return USERS.values().stream()
                .filter(user -> "mentor".equals(user.getRole()))
                .sorted((u1, u2) -> u1.getName().compareToIgnoreCase(u2.getName()))
                .collect(Collectors.toList());
    }
    
    public static List<User> findMentorsSortedBySkill() {
        return USERS.values().stream()
                .filter(user -> "mentor".equals(user.getRole()))
                .sorted((u1, u2) -> {
                    String skills1 = u1.getSkills() == null ? "" : String.join(",", u1.getSkills());
                    String skills2 = u2.getSkills() == null ? "" : String.join(",", u2.getSkills());
                    return skills1.compareToIgnoreCase(skills2);
                })
                .collect(Collectors.toList());
    }
    
    // Match Request methods
    public static MatchRequest saveMatchRequest(MatchRequest request) {
        if (request.getId() == null) {
            request.setId(MATCH_REQUEST_ID_COUNTER.getAndIncrement());
        }
        
        // Validate request
        validateMatchRequest(request);
        
        MATCH_REQUESTS.put(request.getId(), request);
        return request;
    }
    
    public static MatchRequest findMatchRequestById(Integer id) {
        return MATCH_REQUESTS.get(id);
    }
    
    public static List<MatchRequest> findIncomingMatchRequests(Integer mentorId) {
        return MATCH_REQUESTS.values().stream()
                .filter(req -> req.getMentorId().equals(mentorId))
                .collect(Collectors.toList());
    }
    
    public static List<MatchRequest> findOutgoingMatchRequests(Integer menteeId) {
        return MATCH_REQUESTS.values().stream()
                .filter(req -> req.getMenteeId().equals(menteeId))
                .collect(Collectors.toList());
    }
    
    public static boolean hasPendingRequests(Integer menteeId) {
        return MATCH_REQUESTS.values().stream()
                .anyMatch(req -> req.getMenteeId().equals(menteeId) && 
                         "pending".equals(req.getStatus()));
    }
    
    public static boolean hasAcceptedRequests(Integer mentorId) {
        return MATCH_REQUESTS.values().stream()
                .anyMatch(req -> req.getMentorId().equals(mentorId) && 
                         "accepted".equals(req.getStatus()));
    }
    
    public static void rejectOtherRequests(Integer mentorId, Integer exceptRequestId) {
        MATCH_REQUESTS.values().stream()
                .filter(req -> req.getMentorId().equals(mentorId) && 
                        !req.getId().equals(exceptRequestId) && 
                        "pending".equals(req.getStatus()))
                .forEach(req -> {
                    req.setStatus("rejected");
                    MATCH_REQUESTS.put(req.getId(), req);
                });
    }
    
    // Validation methods
    private static void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        
        if (user.getRole() == null || (!user.getRole().equals("mentor") && !user.getRole().equals("mentee"))) {
            throw new IllegalArgumentException("Role must be either 'mentor' or 'mentee'");
        }
    }
    
    private static void validateMatchRequest(MatchRequest request) {
        User mentor = findById(request.getMentorId());
        if (mentor == null || !"mentor".equals(mentor.getRole())) {
            throw new IllegalArgumentException("Invalid mentor ID");
        }
        
        User mentee = findById(request.getMenteeId());
        if (mentee == null || !"mentee".equals(mentee.getRole())) {
            throw new IllegalArgumentException("Invalid mentee ID");
        }
        
        if (hasPendingRequests(request.getMenteeId()) && "pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("Mentee already has a pending request");
        }
    }
    
    // Inner class for MatchRequest
    public static class MatchRequest {
        private Integer id;
        private Integer mentorId;
        private Integer menteeId;
        private String message;
        private String status; // "pending", "accepted", "rejected", "cancelled"
        
        public MatchRequest() {
            this.status = "pending";
        }
        
        // Getters and Setters
        public Integer getId() {
            return id;
        }
        
        public void setId(Integer id) {
            this.id = id;
        }
        
        public Integer getMentorId() {
            return mentorId;
        }
        
        public void setMentorId(Integer mentorId) {
            this.mentorId = mentorId;
        }
        
        public Integer getMenteeId() {
            return menteeId;
        }
        
        public void setMenteeId(Integer menteeId) {
            this.menteeId = menteeId;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public List<String> getSkills() {
        return skills;
    }
    
    public void setSkills(List<String> skills) {
        this.skills = skills;
    }
    
    public byte[] getImageData() {
        return imageData;
    }
    
    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }
    
    public String getImageDataBase64() {
        if (imageData == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(imageData);
    }
    
    public void setImageDataFromBase64(String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            this.imageData = null;
            return;
        }
        
        // Remove the data URL prefix if present
        if (base64Image.startsWith("data:")) {
            base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
        }
        
        this.imageData = Base64.getDecoder().decode(base64Image);
    }
}
