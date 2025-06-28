import axios from 'axios';

// Axios 인스턴스 생성 및 JWT 자동 첨부
const axiosInstance = axios.create({
  baseURL: 'http://localhost:8080/api',
});

axiosInstance.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// Auth API calls
export const signup = async (userData) => {
  try {
    const response = await axiosInstance.post('/signup', userData);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const login = async (credentials) => {
  try {
    const response = await axiosInstance.post('/login', credentials);
    const { token, user } = response.data;
    
    // Store token in localStorage
    localStorage.setItem('token', token);
    
    return { token, user };
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const logout = () => {
  localStorage.removeItem('token');
};

export const isAuthenticated = () => {
  return localStorage.getItem('token') !== null;
};

// User API calls
export const getCurrentUser = async () => {
  try {
    const response = await axiosInstance.get('/me');
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const updateProfile = async (profileData) => {
  try {
    const response = await axiosInstance.put('/profile', profileData);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

// Image utility function
export const getImageUrl = (role, id) => {
  if (!role || !id) {
    return role === 'mentor'
      ? 'https://placehold.co/500x500.jpg?text=MENTOR'
      : 'https://placehold.co/500x500.jpg?text=MENTEE';
  }
  return `http://localhost:8080/api/images/${role}/${id}`;
};

// Mentor API calls
// 백엔드 파라미터명에 맞춰 order_by로 전송
export const getMentors = async (skill, orderBy) => {
  try {
    const params = {};
    if (skill) params.skill = skill;
    if (orderBy) params.order_by = orderBy;
    const response = await axiosInstance.get('/mentors', { params });
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

// Match Request API calls
export const createMatchRequest = async (requestData) => {
  try {
    const response = await axiosInstance.post('/match-requests', requestData);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const getIncomingMatchRequests = async () => {
  try {
    const response = await axiosInstance.get('/match-requests/incoming');
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const getOutgoingMatchRequests = async () => {
  try {
    const response = await axiosInstance.get('/match-requests/outgoing');
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const acceptMatchRequest = async (requestId) => {
  try {
    const response = await axiosInstance.put(`/match-requests/${requestId}/accept`);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const rejectMatchRequest = async (requestId) => {
  try {
    const response = await axiosInstance.put(`/match-requests/${requestId}/reject`);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const cancelMatchRequest = async (requestId) => {
  try {
    const response = await axiosInstance.delete(`/match-requests/${requestId}`);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

// File to base64
export const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = (error) => reject(error);
  });
};

// Token parsing (for getting user info from JWT without backend call)
export const parseJwt = (token) => {
  try {
    return JSON.parse(atob(token.split('.')[1]));
  } catch (e) {
    return null;
  }
};

export const getUserRoleFromToken = () => {
  const token = localStorage.getItem('token');
  if (!token) return null;
  
  const decoded = parseJwt(token);
  return decoded ? decoded.role : null;
};
