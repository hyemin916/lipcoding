---
# Basic rule

필요하다면 나에게 질문을 하세요.
한번에 너무 많은 구현을 하려 하지 말고, 작은 단위로 나누어 구현하세요.
구현 후 테스트를 실행하고, 문제가 있다면 수정하세요.

---

Create minimal project structure with only essential files:

mentor-mentee-app/
├── backend/
│   ├── pom.xml (minimal dependencies only)
│   └── src/main/java/
│       ├── MentorshipApplication.java  # Main + All APIs + Security Config
│       ├── User.java                   # Single data model for everything
│       └── JwtUtil.java                # JWT utilities only
└── frontend/
    ├── package.json (minimal dependencies only)
    └── src/
        ├── index.js                    # Entry point
        ├── App.js                      # All pages + routing + state in one file
        └── api.js                      # All API calls in one file

Backend dependencies ONLY: spring-boot-starter-web, spring-boot-starter-security, java-jwt
Frontend dependencies ONLY: react, react-dom, react-router-dom, axios

ㅁo additional configuration files, no separate folders, no extra dependencies.

---

Create src/main/java/User.java as ONE class containing:
- All user fields (id, email, password, name, role, bio, skills, imageData)
- MatchRequest as inner class with all fields
- Static ConcurrentHashMap for in-memory storage (no database setup)
- Static counters for ID generation
- All data operations (save, find, delete) as static methods within this class
- All validation logic within this class

NO separate repository, service, entity, or DTO classes.
Everything data-related in this single User class.

---

Create src/main/java/JwtUtil.java as ONE utility class containing:
- JWT token generation with ALL RFC 7519 required claims
- Token validation and user extraction
- Secret key and algorithm configuration
- All JWT operations in static methods within this class

NO separate configuration, service, or security classes for JWT.
Everything JWT-related in this single utility class.

---

Create src/main/java/MentorshipApplication.java as ONE class containing:
- @SpringBootApplication main method
- @RestController with ALL API endpoints in this single class
- Spring Security configuration as @Bean methods within this class
- CORS configuration within this class
- ALL 12 API endpoints implemented as methods in this class:
  * POST /api/signup
  * POST /api/login
  * GET /api/me
  * PUT /api/profile
  * GET /api/images/{role}/{id}
  * GET /api/mentors
  * POST /api/match-requests
  * GET /api/match-requests/incoming
  * GET /api/match-requests/outgoing
  * PUT /api/match-requests/{id}/accept
  * PUT /api/match-requests/{id}/reject
  * DELETE /api/match-requests/{id}
- All business logic inline within each endpoint method
- JWT extraction and validation as private methods within this class
- Swagger redirect from root URL

NO separate controller, service, repository, configuration, or security classes.
Everything backend-related in this single main class.

---

Create src/api.js as ONE file containing:
- Axios instance configuration with base URL
- Token management (localStorage, header setting)
- ALL API call functions for every endpoint
- Error handling
- Token refresh logic

NO separate API files, no axios interceptor files, no auth utility files.
Everything API-related in this single file.

---

Create src/App.js as ONE massive component containing:
- All routing with React Router
- Authentication context and state management
- ALL page components defined within this single file:
  * SignupPage component with EXACT element IDs (email, password, role, signup)
  * LoginPage component with EXACT element IDs (email, password, login)
  * ProfilePage component with EXACT element IDs (name, bio, skillsets, profile-photo, profile, save)
  * MentorsPage component with EXACT element IDs (mentor class, search, name/skill radio, message with data attributes, request, request-status)
  * RequestsPage component with EXACT element IDs (request-message class with mentee attribute, accept, reject)
- Navigation component within this file
- All state management and API calls within this file
- Role-based access control within this file

NO separate page files, no separate component files, no separate hooks files.
Everything frontend-related in this single App.js file except API calls.

---

Create src/index.js as minimal React entry point that just renders App component.

Update package.json with minimal start script and ensure port 3000.

NO additional configuration files, no separate routing files, no context files.

---

Configure backend application.properties with only essential settings:
- Port 8080
- No database configuration (using in-memory maps)
- Minimal security settings
- CORS for localhost:3000

Add Swagger configuration within MentorshipApplication.java main class.
Ensure root URL redirects to Swagger UI.

NO separate configuration files, no database setup, no additional properties.

---

Create validation checklist for minimal implementation:

File Count Verification:
✓ Backend: Exactly 3 Java files (MentorshipApplication.java, User.java, JwtUtil.java)
✓ Frontend: Exactly 3 JS files (index.js, App.js, api.js)
✓ Config: Only pom.xml and package.json

API Compliance:
✓ All 12 endpoints respond with correct status codes
✓ JWT tokens contain all RFC 7519 claims
✓ Response formats match API specification exactly
✓ Image upload/download works
✓ Role-based access control functions

UI Compliance:
✓ All element IDs from specification exist and are functional
✓ All CSS classes from specification exist
✓ All data attributes are present
✓ Forms submit successfully
✓ Navigation works between pages
✓ Role-based UI differences work

Integration:
✓ Backend runs on port 8080 with single command
✓ Frontend runs on port 3000 with single command
✓ API calls work end-to-end
✓ Authentication flow works
✓ File upload works
✓ Swagger UI accessible

NO complex business logic, NO advanced validation, NO sophisticated error handling.
Only what's required to pass evaluation tests.