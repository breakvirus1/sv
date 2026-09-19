import axios from 'axios'

const SERVER_IP = import.meta.env.VITE_SERVER_IP || '192.168.1.40';
const API_BASE_URL = `http://${SERVER_IP}:8085`;

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

export { API_BASE_URL, api as default }

let isRefreshing = false;
let refreshSubscribers = [];

const subscribeTokenRefresh = (cb) => {
  refreshSubscribers.push(cb);
};

const onRefreshed = (token) => {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
};

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const originalRequest = error.config;
      if (!originalRequest || originalRequest._retry) {
        localStorage.removeItem('token');
        localStorage.removeItem('auth');
        window.location.href = '/login';
        return Promise.reject(error);
      }

      originalRequest._retry = true;

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((token) => {
            if (token) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
              resolve(api(originalRequest));
            } else {
              localStorage.removeItem('token');
              localStorage.removeItem('auth');
              window.location.href = '/login';
              reject(error);
            }
          });
        });
      }

      isRefreshing = true;
      try {
        const stored = JSON.parse(localStorage.getItem('auth') || '{}');
        const refreshToken = stored?.refresh_token;
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const params = new URLSearchParams();
        params.append('grant_type', 'refresh_token');
        params.append('client_id', import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'frontend');
        params.append('refresh_token', refreshToken);

        const response = await axios.post(`${API_BASE_URL}/api/v1/auth/refresh`, params.toString(), {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        });

        const newToken = response.data.access_token;
        const newRefreshToken = response.data.refresh_token || refreshToken;
        const expiresIn = response.data.expires_in || 3600;

        const auth = {
          ...(JSON.parse(localStorage.getItem('auth') || '{}')),
          access_token: newToken,
          refresh_token: newRefreshToken,
          expiresAt: Date.now() + expiresIn * 1000,
        };
        localStorage.setItem('auth', JSON.stringify(auth));
        localStorage.setItem('token', newToken);

        onRefreshed(newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('token');
        localStorage.removeItem('auth');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  }
)
