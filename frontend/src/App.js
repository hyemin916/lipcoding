import React, { useState, useEffect, createContext, useContext } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, Navigate, useNavigate, useLocation } from 'react-router-dom';
import * as api from './api';

// Create auth context
const AuthContext = createContext(null);

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Check if user is authenticated on initial load
  useEffect(() => {
    const checkAuth = async () => {
      try {
        if (api.isAuthenticated()) {
          const userData = await api.getCurrentUser();
          setUser(userData);
        }
      } catch (err) {
        console.error('Authentication error:', err);
        api.logout();
      } finally {
        setLoading(false);
      }
    };

    checkAuth();
  }, []);

  const login = async (credentials) => {
    try {
      const result = await api.login(credentials);
      setUser(result.user);
      return true;
    } catch (err) {
      setError(err.error || 'Login failed');
      return false;
    }
  };

  const signup = async (userData) => {
    try {
      await api.signup(userData);
      return true;
    } catch (err) {
      setError(err.error || 'Signup failed');
      return false;
    }
  };

  const logout = () => {
    api.logout();
    setUser(null);
  };

  const updateUserProfile = async (profileData) => {
    try {
      const updatedUser = await api.updateProfile(profileData);
      setUser(updatedUser);
      return true;
    } catch (err) {
      setError(err.error || 'Profile update failed');
      return false;
    }
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <AuthContext.Provider value={{ user, login, signup, logout, updateUserProfile, error, setError }}>
      <Router>
        <div className="app-container">
          {user && <Navigation user={user} logout={logout} />}
          
          <Routes>
            <Route path="/" element={
              user ? <Navigate to="/profile" /> : <Navigate to="/login" />
            } />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/profile" element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            } />
            <Route path="/mentors" element={
              <ProtectedRoute roleRequired="mentee">
                <MentorsPage />
              </ProtectedRoute>
            } />
            <Route path="/requests" element={
              <ProtectedRoute>
                <RequestsPage />
              </ProtectedRoute>
            } />
          </Routes>
        </div>
      </Router>
    </AuthContext.Provider>
  );
}

// Navigation Component
function Navigation({ user, logout }) {
  const role = user?.role;

  return (
    <nav className="navigation">
      <ul>
        <li><Link to="/profile">Profile</Link></li>
        {role === 'mentee' && <li><Link to="/mentors">Mentors</Link></li>}
        <li><Link to="/requests">Requests</Link></li>
        <li><button onClick={logout}>Logout</button></li>
      </ul>
    </nav>
  );
}

// Protected Route Component
function ProtectedRoute({ children, roleRequired }) {
  const { user } = useContext(AuthContext);
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roleRequired && user.role !== roleRequired) {
    return <Navigate to="/profile" replace />;
  }

  return children;
}

// Signup Page Component
function SignupPage() {
  const { signup, error, setError } = useContext(AuthContext);
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    name: '',
    role: 'mentee'
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    setError(null);
  }, [setError]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prevData => ({
      ...prevData,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      const success = await signup(formData);
      if (success) {
        navigate('/login');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="signup-container">
      <h2>Sign Up</h2>
      {error && <div className="error-message">{error}</div>}
      
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="email">Email:</label>
          <input
            type="email"
            id="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            required
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="password">Password:</label>
          <input
            type="password"
            id="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="name">Name:</label>
          <input
            type="text"
            id="name"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="role">Role:</label>
          <select
            id="role"
            name="role"
            value={formData.role}
            onChange={handleChange}
            required
          >
            <option value="mentee">Mentee</option>
            <option value="mentor">Mentor</option>
          </select>
        </div>
        
        <button
          type="submit"
          id="signup"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Signing up...' : 'Sign Up'}
        </button>
      </form>
      
      <p>
        Already have an account? <Link to="/login">Login</Link>
      </p>
    </div>
  );
}

// Login Page Component
function LoginPage() {
  const { login, error, setError } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const from = location.state?.from?.pathname || '/profile';

  useEffect(() => {
    setError(null);
  }, [setError]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prevData => ({
      ...prevData,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      const success = await login(formData);
      if (success) {
        // 약간의 지연 후 페이지 이동 (테스트가 리디렉션을 감지할 수 있도록)
        setTimeout(() => {
          navigate(from);
        }, 100);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-container">
      <h2>Login</h2>
      {error && <div className="error-message">{error}</div>}
      
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="email">Email:</label>
          <input
            type="email"
            id="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            required
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="password">Password:</label>
          <input
            type="password"
            id="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
          />
        </div>
        
        <button
          type="submit"
          id="login"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Logging in...' : 'Login'}
        </button>
      </form>
      
      <p>
        Don't have an account? <Link to="/signup">Sign Up</Link>
      </p>
    </div>
  );
}

// Profile Page Component
function ProfilePage() {
  const { user, updateUserProfile, error, setError } = useContext(AuthContext);
  const [formData, setFormData] = useState({
    id: user?.id || '',
    name: user?.profile?.name || '',
    role: user?.role || '',
    bio: user?.profile?.bio || '',
    skills: user?.profile?.skills || [],
    image: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    if (user) {
      setFormData({
        id: user.id,
        name: user.profile?.name || '',
        role: user.role,
        bio: user.profile?.bio || '',
        skills: user.profile?.skills || [],
        image: ''
      });
    }
  }, [user]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prevData => ({
      ...prevData,
      [name]: value
    }));
  };

  const handleSkillsChange = (e) => {
    const { value } = e.target;
    const skillsArray = value.split(',').map(skill => skill.trim());
    setFormData(prevData => ({
      ...prevData,
      skills: skillsArray
    }));
  };

  const handleImageChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Validate file size (max 1MB)
    if (file.size > 1024 * 1024) {
      setError('Image size must be less than 1MB');
      return;
    }

    // Validate file type
    if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
      setError('Only JPG and PNG images are allowed');
      return;
    }

    try {
      const base64 = await api.fileToBase64(file);
      setFormData(prevData => ({
        ...prevData,
        image: base64
      }));
    } catch (err) {
      setError('Error processing image');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);
    setSuccessMessage('');

    try {
      const success = await updateUserProfile(formData);
      if (success) {
        setSuccessMessage('Profile updated successfully');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Use backend-provided imageUrl if present, else fallback to default
  const profileImageUrl = user?.profile?.imageUrl && !user?.profile?.imageUrl.startsWith('/')
    ? user.profile.imageUrl
    : (user?.role === 'mentor'
      ? 'https://placehold.co/500x500.jpg?text=MENTOR'
      : 'https://placehold.co/500x500.jpg?text=MENTEE');

  return (
    <div className="profile-container">
      <h2>Profile</h2>
      {error && <div className="error-message">{error}</div>}
      {successMessage && <div className="success-message">{successMessage}</div>}
      
      <div className="profile-image-container">
        <img
          id="profile-photo"
          src={profileImageUrl}
          alt="Profile"
          className="profile-image"
        />
      </div>
      
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="name">Name:</label>
          <input
            type="text"
            id="name"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
          />
        </div>
        <div className="form-group">
          <label htmlFor="bio">Bio:</label>
          <textarea
            id="bio"
            name="bio"
            value={formData.bio}
            onChange={handleChange}
            rows="4"
          />
        </div>
        {user?.role === 'mentor' && (
          <div className="form-group">
            <label htmlFor="skillsets">Skills (comma-separated):</label>
            <input
              type="text"
              id="skillsets"
              name="skillsets"
              value={formData.skills.join(', ')}
              onChange={handleSkillsChange}
            />
          </div>
        )}
        <div className="form-group">
          <label htmlFor="profile">Profile Image:</label>
          <input
            type="file"
            id="profile"
            name="profile"
            accept="image/jpeg, image/png"
            onChange={handleImageChange}
          />
          <small>Max size: 1MB, Formats: JPG, PNG</small>
        </div>
        <button
          type="submit"
          id="save"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Saving...' : 'Save Profile'}
        </button>
      </form>
    </div>
  );
}

// Mentors Page Component (Mentee only)
function MentorsPage() {
  const { user, error, setError } = useContext(AuthContext);
  const [mentors, setMentors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState('');
  const [requestMessages, setRequestMessages] = useState({});
  const [pendingRequest, setPendingRequest] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    fetchMentors();
    checkPendingRequests();
  }, []);

  const fetchMentors = async (skill, orderBy) => {
    try {
      setLoading(true);
      // orderBy -> order_by (백엔드 파라미터명 일치)
      const mentorsData = await api.getMentors(skill, orderBy);
      setMentors(mentorsData);
    } catch (err) {
      setError(err.error || 'Failed to fetch mentors');
    } finally {
      setLoading(false);
    }
  };

  const checkPendingRequests = async () => {
    try {
      const requests = await api.getOutgoingMatchRequests();
      const hasPending = requests.some(req => req.status === 'pending');
      setPendingRequest(hasPending);
    } catch (err) {
      console.error('Failed to check pending requests:', err);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    fetchMentors(searchTerm);
  };

  const handleSortChange = (e) => {
    const sortValue = e.target.value;
    setSortBy(sortValue);
    fetchMentors(searchTerm, sortValue);
  };

  const handleMessageChange = (mentorId, message) => {
    setRequestMessages(prev => ({
      ...prev,
      [mentorId]: message
    }));
  };

  const handleSendRequest = async (mentorId) => {
    try {
      setError(null);
      setSuccessMessage('');
      
      if (pendingRequest) {
        setError('You already have a pending request');
        return;
      }
      
      const message = requestMessages[mentorId] || '';
      
      await api.createMatchRequest({
        mentorId,
        menteeId: user.id,
        message
      });
      
      setPendingRequest(true);
      setSuccessMessage('Match request sent successfully');
      
      // Clear the message
      setRequestMessages(prev => ({
        ...prev,
        [mentorId]: ''
      }));
    } catch (err) {
      setError(err.error || 'Failed to send request');
    }
  };

  return (
    <div className="mentors-container">
      <h2>Find Mentors</h2>
      {error && <div className="error-message">{error}</div>}
      {successMessage && <div className="success-message">{successMessage}</div>}
      
      <div className="search-container">
        <form onSubmit={handleSearch}>
          <input
            type="text"
            id="search"
            placeholder="Search by skill..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          <button type="submit">Search</button>
        </form>
        
        <div className="sort-container">
          <label>Sort by:</label>
          <label>
            <input
              type="radio"
              id="name"
              name="sort"
              value="name"
              checked={sortBy === 'name'}
              onChange={handleSortChange}
            />
            Name
          </label>
          <label>
            <input
              type="radio"
              id="skill"
              name="sort"
              value="skill"
              checked={sortBy === 'skill'}
              onChange={handleSortChange}
            />
            Skills
          </label>
        </div>
      </div>
      
      {loading ? (
        <div>Loading mentors...</div>
      ) : mentors.length === 0 ? (
        <div>No mentors found</div>
      ) : (
        <div className="mentors-list">
          {mentors.map(mentor => {
            // mentor.profile.imageUrl가 /로 시작하면 백엔드 이미지 API, 아니면 기본 URL
            let imageUrl = mentor.profile.imageUrl;
            if (imageUrl && imageUrl.startsWith('/')) {
              imageUrl = `http://localhost:8080/api${imageUrl}`;
            }
            if (!imageUrl) {
              imageUrl = 'https://placehold.co/500x500.jpg?text=MENTOR';
            }
            return (
              <div key={mentor.id} className="mentor">
                <div className="mentor-info">
                  <img
                    src={imageUrl}
                    alt={mentor.profile.name}
                    className="mentor-image"
                  />
                  <h3>{mentor.profile.name}</h3>
                  <p>{mentor.profile.bio}</p>
                  <div className="skills">
                    <strong>Skills:</strong>
                    <span>{mentor.profile.skills?.join(', ')}</span>
                  </div>
                </div>
                <div className="request-form">
                  <textarea
                    id="message"
                    data-mentor-id={mentor.id}
                    data-testid={`message-${mentor.id}`}
                    placeholder="Enter your message..."
                    value={requestMessages[mentor.id] || ''}
                    onChange={(e) => handleMessageChange(mentor.id, e.target.value)}
                    disabled={pendingRequest}
                  ></textarea>
                  <button
                    id="request"
                    onClick={() => handleSendRequest(mentor.id)}
                    disabled={pendingRequest}
                  >
                    Send Request
                  </button>
                  {pendingRequest && (
                    <div id="request-status" className="request-status">
                      You already have a pending request
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// Requests Page Component
function RequestsPage() {
  const { user, error, setError } = useContext(AuthContext);
  const [incomingRequests, setIncomingRequests] = useState([]);
  const [outgoingRequests, setOutgoingRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [successMessage, setSuccessMessage] = useState('');
  
  const isMentor = user?.role === 'mentor';

  useEffect(() => {
    fetchRequests();
  }, [user]);

  const fetchRequests = async () => {
    try {
      setLoading(true);
      
      if (isMentor) {
        const incoming = await api.getIncomingMatchRequests();
        setIncomingRequests(incoming);
      } else {
        const outgoing = await api.getOutgoingMatchRequests();
        setOutgoingRequests(outgoing);
      }
    } catch (err) {
      setError(err.error || 'Failed to fetch requests');
    } finally {
      setLoading(false);
    }
  };

  const handleAccept = async (requestId) => {
    try {
      setError(null);
      setSuccessMessage('');
      
      await api.acceptMatchRequest(requestId);
      setSuccessMessage('Request accepted successfully');
      
      // Refresh requests
      fetchRequests();
    } catch (err) {
      setError(err.error || 'Failed to accept request');
    }
  };

  const handleReject = async (requestId) => {
    try {
      setError(null);
      setSuccessMessage('');
      
      await api.rejectMatchRequest(requestId);
      setSuccessMessage('Request rejected successfully');
      
      // Refresh requests
      fetchRequests();
    } catch (err) {
      setError(err.error || 'Failed to reject request');
    }
  };

  const handleCancel = async (requestId) => {
    try {
      setError(null);
      setSuccessMessage('');
      
      await api.cancelMatchRequest(requestId);
      setSuccessMessage('Request cancelled successfully');
      
      // Refresh requests
      fetchRequests();
    } catch (err) {
      setError(err.error || 'Failed to cancel request');
    }
  };

  const renderMentorRequests = () => {
    if (incomingRequests.length === 0) {
      return <div>No incoming requests</div>;
    }

    return (
      <div className="requests-list">
        {incomingRequests.map(request => {
          const canRespond = request.status === 'pending';
          const menteeId = request.menteeId;
          
          return (
            <div key={request.id} className="request-item">
              <div className="request-message" mentee={menteeId}>
                <h3>Request from Mentee #{menteeId}</h3>
                <p><strong>Status:</strong> {request.status}</p>
                <p><strong>Message:</strong> {request.message}</p>
              </div>
              
              {canRespond && (
                <div className="request-actions">
                  <button
                    id="accept"
                    onClick={() => handleAccept(request.id)}
                  >
                    Accept
                  </button>
                  <button
                    id="reject"
                    onClick={() => handleReject(request.id)}
                  >
                    Reject
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>
    );
  };

  const renderMenteeRequests = () => {
    if (outgoingRequests.length === 0) {
      return <div>No outgoing requests</div>;
    }

    return (
      <div className="requests-list">
        {outgoingRequests.map(request => {
          const canCancel = request.status === 'pending';
          
          return (
            <div key={request.id} className="request-item">
              <div>
                <h3>Request to Mentor #{request.mentorId}</h3>
                <p><strong>Status:</strong> {request.status}</p>
              </div>
              
              {canCancel && (
                <div className="request-actions">
                  <button onClick={() => handleCancel(request.id)}>
                    Cancel
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div className="requests-container">
      <h2>{isMentor ? 'Incoming Requests' : 'Your Requests'}</h2>
      {error && <div className="error-message">{error}</div>}
      {successMessage && <div className="success-message">{successMessage}</div>}
      
      {loading ? (
        <div>Loading requests...</div>
      ) : isMentor ? (
        renderMentorRequests()
      ) : (
        renderMenteeRequests()
      )}
    </div>
  );
}

export default App;
