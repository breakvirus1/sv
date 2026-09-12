import { createContext, useContext, useEffect, useState } from 'react';
import api from '../services/api';

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

const KEYCLOAK_ISSUER = import.meta.env.VITE_KEYCLOAK_ISSUER;
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

function extractRoles(user) {
  let roles = [];
  if (user?.realm_access?.roles) {
    roles = user.realm_access.roles;
  } else if (user?.access_token) {
    const decoded = decodeJwt(user.access_token);
    if (decoded?.realm_access?.roles) {
      roles = decoded.realm_access.roles;
    }
  }
  return roles.map((role) => (role.startsWith('ROLE_') ? role : `ROLE_${role}`));
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

  useEffect(() => {
    const initAuth = async () => {
      try {
        const stored = getStoredAuth();
        console.log('Stored auth:', stored ? 'found' : 'none');
        if (stored) {
          const roles = extractRoles(stored);
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
        throw new Error('Missing tokens in callback');
      }

      const decoded = decodeJwt(idToken) || {};
      const auth = {
        ...decoded,
        access_token: accessToken,
        expiresAt: Date.now() + expiresIn * 1000
      };

      storeAuth(auth);
      const roles = extractRoles(auth);
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
