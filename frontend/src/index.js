import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// Simple CSS for minimal styling
const style = document.createElement('style');
style.textContent = `
  * {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
  }
  
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
    line-height: 1.6;
    color: #333;
    padding: 20px;
  }
  
  .app-container {
    max-width: 1200px;
    margin: 0 auto;
  }
  
  .navigation {
    margin-bottom: 20px;
    padding-bottom: 10px;
    border-bottom: 1px solid #ddd;
  }
  
  .navigation ul {
    display: flex;
    list-style: none;
  }
  
  .navigation li {
    margin-right: 20px;
  }
  
  .navigation a {
    text-decoration: none;
    color: #0066cc;
  }
  
  h2 {
    margin-bottom: 20px;
  }
  
  .form-group {
    margin-bottom: 15px;
  }
  
  label {
    display: block;
    margin-bottom: 5px;
  }
  
  input, select, textarea {
    width: 100%;
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 4px;
  }
  
  button {
    padding: 8px 16px;
    background-color: #0066cc;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
  
  button:disabled {
    background-color: #cccccc;
  }
  
  .error-message {
    color: #d9534f;
    margin-bottom: 15px;
    padding: 10px;
    background-color: #f9f2f2;
    border-radius: 4px;
  }
  
  .success-message {
    color: #5cb85c;
    margin-bottom: 15px;
    padding: 10px;
    background-color: #f2f9f2;
    border-radius: 4px;
  }
  
  .profile-image-container {
    margin-bottom: 20px;
    text-align: center;
  }
  
  .profile-image {
    width: 150px;
    height: 150px;
    object-fit: cover;
    border-radius: 50%;
  }
  
  .mentor {
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 15px;
    margin-bottom: 20px;
    display: flex;
    flex-wrap: wrap;
  }
  
  .mentor-info {
    flex: 1;
    min-width: 300px;
  }
  
  .mentor-image {
    width: 100px;
    height: 100px;
    object-fit: cover;
    border-radius: 50%;
    margin-right: 15px;
    float: left;
  }
  
  .request-form {
    flex: 1;
    min-width: 300px;
    padding-left: 15px;
  }
  
  .request-status {
    margin-top: 10px;
    color: #d9534f;
  }
  
  .requests-list {
    margin-top: 20px;
  }
  
  .request-item {
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 15px;
    margin-bottom: 15px;
  }
  
  .request-actions {
    margin-top: 10px;
  }
  
  .request-actions button {
    margin-right: 10px;
  }
`;
document.head.appendChild(style);

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
