import axios from 'axios';

// Create axios instance with base URL
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add request interceptor for adding auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor for handling errors
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Handle 401 unauthorized errors
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API calls
export const signup = async (userData) => {
  try {
    const response = await api.post('/signup', userData);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const login = async (credentials) => {
  try {
    const response = await api.post('/login', credentials);
    const { token } = response.data;
    
    // Store token in localStorage
    localStorage.setItem('token', token);
    
    return { token };
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
    const response = await api.get('/me');
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const updateProfile = async (profileData) => {
  try {
    const response = await api.put('/profile', profileData);
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
export const getMentors = async (skill, orderBy) => {
  try {
    let url = '/mentors';
    const params = new URLSearchParams();
    
    if (skill) {
      params.append('skill', skill);
    }
    
    if (orderBy) {
      params.append('order_by', orderBy);
    }
    
    if (params.toString()) {
      url += `?${params.toString()}`;
    }
    
    const response = await api.get(url);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

// Match Request API calls
export const createMatchRequest = async (requestData) => {
  try {
    const response = await api.post('/match-requests', requestData);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const getIncomingMatchRequests = async () => {
  try {
    const response = await api.get('/match-requests/incoming');
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const getOutgoingMatchRequests = async () => {
  try {
    const response = await api.get('/match-requests/outgoing');
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const acceptMatchRequest = async (requestId) => {
  try {
    const response = await api.put(`/match-requests/${requestId}/accept`);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const rejectMatchRequest = async (requestId) => {
  try {
    const response = await api.put(`/match-requests/${requestId}/reject`);
    return response.data;
  } catch (error) {
    throw error.response ? error.response.data : { error: 'Network error' };
  }
};

export const cancelMatchRequest = async (requestId) => {
  try {
    const response = await api.delete(`/match-requests/${requestId}`);
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
