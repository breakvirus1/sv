import { createContext, useContext, useEffect, useState } from 'react';
import api from '../services/api';

const API_BASE_URL = api.defaults.baseURL;

function generateState() {
  try {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
  } catch (e) {
    console.warn('crypto.randomUUID is not available, using fallback', e);
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

const KEYCLOAK_ISSUER = import.meta.env.VITE_KEYCLOAK_ISSUER || `http://${import.meta.env.VITE_SERVER_IP || '192.168.1.40'}:8080/realms/print-sv`;
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'frontend';
const BUILD_BUSTER = 1;

function decodeJwt(token) {
  try {
    const payload = token.split('.')[1];
    let base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const pad = base64.length % 4;
    if (pad) base64 += '='.repeat(4 - pad);
    return JSON.parse(atob(base64));
  } catch (e) {
    console.error('JWT decode error:', e);
    return null;
  }
}

function extractRolesFromToken(accessToken) {
  const decoded = decodeJwt(accessToken);
  if (!decoded?.realm_access?.roles) return [];
  return decoded.realm_access.roles.map((role) => (role.startsWith('ROLE_') ? role : `ROLE_${role}`));
}

function getStoredAuth() {
  try {
    const raw = localStorage.getItem('auth');
    if (!raw) return null;
    const auth = JSON.parse(raw);
    if (auth.expiresAt && Date.now() > auth.expiresAt) {
      localStorage.removeItem('auth');
      return null;
    }
    return auth;
  } catch (e) {
    return null;
  }
}

function storeAuth(auth) {
  localStorage.setItem('auth', JSON.stringify(auth));
}

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState(null);

  useEffect(() => {
    const initAuth = async () => {
      try {
        const stored = getStoredAuth();
        console.log('Stored auth:', stored ? 'found' : 'none');
        if (stored) {
          const roles = extractRolesFromToken(stored.access_token);
          console.log('User roles (raw):', stored.realm_access?.roles, '-> transformed:', roles);
          setUser({
            name: stored.name || stored.preferred_username,
            email: stored.email,
            roles,
            accessToken: stored.access_token,
            username: stored.preferred_username || stored.sub
          });
          localStorage.setItem('token', stored.access_token);
          api.post('/api/v1/employees/sync').catch((err) => console.error('Employee sync failed:', err));
        }
      } catch (err) {
        console.error('Auth init error:', err);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const applyToken = (accessToken, expiresIn, extra = {}) => {
    const expiresAt = Date.now() + (expiresIn || 3600) * 1000;
    const decoded = decodeJwt(accessToken) || {};
    const auth = {
      ...decoded,
      access_token: accessToken,
      expiresAt,
      ...extra
    };
    storeAuth(auth);
    const roles = extractRolesFromToken(accessToken);
    setUser({
      name: auth.name || auth.preferred_username,
      email: auth.email,
      roles,
      accessToken,
      username: auth.preferred_username || auth.sub
    });
    localStorage.setItem('token', accessToken);
    setAuthError(null);
    api.post('/api/v1/employees/sync').catch((err) => console.error('Employee sync failed:', err));
  };

  const loginWithPassword = async (username, password) => {
    setAuthError(null);
    try {
      const params = new URLSearchParams();
      params.append('grant_type', 'password');
      params.append('client_id', CLIENT_ID);
      params.append('username', username);
      params.append('password', password);

      const response = await fetch(`${KEYCLOAK_ISSUER}/protocol/openid-connect/token`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params.toString(),
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error_description || data.error || 'Ошибка авторизации');
      }

      const accessToken = data.access_token;
      const refreshToken = data.refresh_token;
      const expiresIn = data.expires_in || 3600;

      applyToken(accessToken, expiresIn, { refresh_token: refreshToken });
      return { success: true, roles: extractRolesFromToken(accessToken) };
    } catch (error) {
      const message = error.message || 'Не удалось войти';
      setAuthError(message);
      return { success: false, error: message };
    }
  };

  const refreshToken = async () => {
    try {
      const stored = getStoredAuth();
      const refreshToken = stored?.refresh_token;
      if (!refreshToken) {
        logout();
        return { success: false };
      }

      const params = new URLSearchParams();
      params.append('grant_type', 'refresh_token');
      params.append('client_id', CLIENT_ID);
      params.append('refresh_token', refreshToken);

      const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params.toString(),
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Ошибка обновления токена');
      }

      applyToken(data.access_token, data.expires_in || 3600, { refresh_token: data.refresh_token || refreshToken });
      return { success: true };
    } catch (error) {
      logout();
      return { success: false };
    }
  };

  const login = () => {
    const state = generateState();
    const redirectUri = `${window.location.origin}/callback`;
    const url = new URL(`${KEYCLOAK_ISSUER}/protocol/openid-connect/auth`);
    url.searchParams.set('client_id', CLIENT_ID);
    url.searchParams.set('redirect_uri', redirectUri);
    url.searchParams.set('response_type', 'id_token token');
    url.searchParams.set('scope', 'openid profile email');
    url.searchParams.set('state', state);
    url.searchParams.set('nonce', generateState());
    url.searchParams.set('response_mode', 'fragment');
    window.location.assign(url.toString());
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('auth');
    const postLogoutRedirectUri = `${window.location.origin}/`;
    const url = new URL(`${KEYCLOAK_ISSUER}/protocol/openid-connect/logout`);
    url.searchParams.set('client_id', CLIENT_ID);
    url.searchParams.set('post_logout_redirect_uri', postLogoutRedirectUri);
    window.location.href = url.toString();
  };

  const handleCallback = () => {
    try {
      const hash = window.location.hash.replace(/^#\/?/, '');
      const params = Object.fromEntries(new URLSearchParams(hash));
      const accessToken = params.access_token;
      const idToken = params.id_token;
      const expiresIn = Number(params.expires_in || '3600');

      if (!accessToken || !idToken) {
        const searchParams = Object.fromEntries(new URLSearchParams(window.location.search));
        const error = params.error || searchParams.error;
        const errorDescription = params.error_description || searchParams.error_description;
        console.error('Missing tokens in callback. Hash:', hash, 'Query:', window.location.search, 'Error:', error, 'Description:', errorDescription);
        if (!error) {
          window.location.replace('/');
          return;
        }
        throw new Error(errorDescription || 'Missing tokens in callback');
      }

      const decoded = decodeJwt(idToken) || {};
      const auth = {
        ...decoded,
        access_token: accessToken,
        expiresAt: Date.now() + expiresIn * 1000
      };

      storeAuth(auth);
      const roles = extractRolesFromToken(accessToken);
      setUser({
        name: auth.name || auth.preferred_username,
        email: auth.email,
        roles,
        accessToken,
        username: auth.preferred_username || auth.sub
      });
      localStorage.setItem('token', accessToken);
      api.post('/api/v1/employees/sync').catch((err) => console.error('Employee sync failed:', err));
      window.history.replaceState({}, document.title, '/');
    } catch (err) {
      console.error('Callback error:', err);
      throw err;
    }
  };

  const value = {
    user,
    loading,
    login,
    logout,
    handleCallback,
    loginWithPassword,
    refreshToken,
    authError,
    setAuthError,
    isAuthenticated: !!user
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
