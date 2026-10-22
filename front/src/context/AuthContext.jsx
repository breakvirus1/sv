import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import api from '../services/api';

const API_BASE_URL = api.defaults.baseURL;
const KEYCLOAK_ISSUER = '/realms/print-sv';
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'frontend';
const AUTH_STORAGE_KEY = 'auth';
const TOKEN_STORAGE_KEY = 'token';
const PKCE_STORAGE_KEY = 'pkce';

let callbackProcessed = false;

function getStorage() {
  return window.sessionStorage;
}

function clearLegacyLocalStorage() {
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
}

function randomString(length = 64) {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
  const bytes = new Uint8Array(length);
  if (window.crypto?.getRandomValues) {
    window.crypto.getRandomValues(bytes);
  } else {
    for (let i = 0; i < length; i += 1) bytes[i] = Math.floor(Math.random() * 256);
  }
  return Array.from(bytes, (b) => alphabet[b % alphabet.length]).join('');
}

function base64UrlEncode(bytes) {
  let binary = '';
  bytes.forEach((b) => { binary += String.fromCharCode(b); });
  return window.btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function createCodeChallenge(verifier) {
  if (window.crypto?.subtle) {
    const data = new TextEncoder().encode(verifier);
    const digest = await window.crypto.subtle.digest('SHA-256', data);
    return { challenge: base64UrlEncode(new Uint8Array(digest)), method: 'S256' };
  }
  // Keycloak умеет plain PKCE. S256 используется всегда, когда доступен secure context.
  return { challenge: verifier, method: 'plain' };
}

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

function extractCompanyIdFromToken(accessToken) {
  const decoded = decodeJwt(accessToken);
  if (!decoded) return null;
  const claim = decoded.company_id;
  if (claim === undefined || claim === null) return null;
  if (typeof claim === 'number') return claim;
  if (typeof claim === 'string') return parseInt(claim, 10);
  if (Array.isArray(claim) && claim.length > 0) return parseInt(claim[0], 10);
  return null;
}

function getStoredAuth() {
  try {
    clearLegacyLocalStorage();
    const raw = getStorage().getItem(AUTH_STORAGE_KEY);
    if (!raw) return null;
    const auth = JSON.parse(raw);
    if (auth.expiresAt && Date.now() > auth.expiresAt) {
      getStorage().removeItem(AUTH_STORAGE_KEY);
      getStorage().removeItem(TOKEN_STORAGE_KEY);
      return null;
    }
    return auth;
  } catch (e) {
    return null;
  }
}

function storeAuth(auth) {
  getStorage().setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
  getStorage().setItem(TOKEN_STORAGE_KEY, auth.access_token);
}

function clearAuth() {
  getStorage().removeItem(AUTH_STORAGE_KEY);
  getStorage().removeItem(TOKEN_STORAGE_KEY);
  getStorage().removeItem(PKCE_STORAGE_KEY);
  clearLegacyLocalStorage();
}

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState(null);

  const applyTokens = useCallback((tokens) => {
    const accessToken = tokens.access_token;
    const idToken = tokens.id_token;
    const decodedId = idToken ? decodeJwt(idToken) : {};
    const decodedAccess = decodeJwt(accessToken) || {};
    const expiresIn = Number(tokens.expires_in || 3600);
    const companyId = extractCompanyIdFromToken(accessToken);
    const auth = {
      ...(decodedId || {}),
      access_token: accessToken,
      refresh_token: tokens.refresh_token,
      id_token: idToken,
      expiresAt: Date.now() + expiresIn * 1000,
      companyId
    };

    storeAuth(auth);
    const roles = extractRolesFromToken(accessToken);
    setUser({
      name: auth.name || auth.preferred_username || decodedAccess.preferred_username,
      email: auth.email,
      roles,
      accessToken,
      username: auth.preferred_username || auth.sub || decodedAccess.preferred_username,
      companyId
    });
    setAuthError(null);
    api.post('/api/v1/employees/sync').catch((err) => console.error('Employee sync failed:', err));
    return { auth, roles };
  }, []);

  useEffect(() => {
    const initAuth = async () => {
      try {
        const stored = getStoredAuth();
        if (stored) {
          const roles = extractRolesFromToken(stored.access_token);
          const companyId = stored.companyId ?? extractCompanyIdFromToken(stored.access_token);
          setUser({
            name: stored.name || stored.preferred_username,
            email: stored.email,
            roles,
            accessToken: stored.access_token,
            username: stored.preferred_username || stored.sub,
            companyId
          });
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

  const login = useCallback(async () => {
    setAuthError(null);
    const state = randomString(32);
    const nonce = randomString(32);
    const codeVerifier = randomString(64);
    const { challenge, method } = await createCodeChallenge(codeVerifier);
    const redirectUri = `${window.location.origin}/callback`;

    getStorage().setItem(PKCE_STORAGE_KEY, JSON.stringify({
      state,
      nonce,
      codeVerifier,
      redirectUri
    }));

    const url = new URL(`${KEYCLOAK_ISSUER}/protocol/openid-connect/auth`, window.location.origin);
    url.searchParams.set('client_id', CLIENT_ID);
    url.searchParams.set('redirect_uri', redirectUri);
    url.searchParams.set('response_type', 'code');
    url.searchParams.set('scope', 'openid profile email');
    url.searchParams.set('state', state);
    url.searchParams.set('nonce', nonce);
    url.searchParams.set('code_challenge', challenge);
    url.searchParams.set('code_challenge_method', method);
    window.location.assign(url.toString());
  }, []);

  const logout = useCallback(() => {
    const stored = getStoredAuth();
    clearAuth();
    setUser(null);
    const url = new URL(`${KEYCLOAK_ISSUER}/protocol/openid-connect/logout`, window.location.origin);
    url.searchParams.set('client_id', CLIENT_ID);
    url.searchParams.set('post_logout_redirect_uri', `${window.location.origin}/`);
    if (stored?.id_token) {
      url.searchParams.set('id_token_hint', stored.id_token);
    }
    window.location.href = url.toString();
  }, []);

  const exchangeCode = useCallback(async (code, redirectUri, codeVerifier) => {
    const params = new URLSearchParams();
    params.append('grant_type', 'authorization_code');
    params.append('client_id', CLIENT_ID);
    params.append('code', code);
    params.append('redirect_uri', redirectUri);
    params.append('code_verifier', codeVerifier);

    const response = await fetch(`${API_BASE_URL}/api/v1/auth/token`, {
      method: 'POST',
      credentials: 'omit',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString()
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error || data.error_description || 'Ошибка обмена authorization code');
    }
    return data;
  }, []);

  const handleCallback = useCallback(async () => {
    if (callbackProcessed) return window.location.pathname;
    try {
      const query = new URLSearchParams(window.location.search);
      const hash = new URLSearchParams(window.location.hash.replace(/^#\/?/, ''));
      const code = query.get('code');
      const state = query.get('state');
      const error = query.get('error') || hash.get('error');
      const errorDescription = query.get('error_description') || hash.get('error_description');

      if (error) {
        throw new Error(errorDescription || error);
      }
      if (!code) {
        throw new Error('В ответе Keycloak отсутствует authorization code');
      }

      const stored = JSON.parse(getStorage().getItem(PKCE_STORAGE_KEY) || 'null');
      if (!stored?.codeVerifier || !stored?.state) {
        throw new Error('Сессия входа не найдена, повторите вход');
      }
      if (state !== stored.state) {
        throw new Error('Некорректный state в ответе Keycloak');
      }

      const tokens = await exchangeCode(code, stored.redirectUri, stored.codeVerifier);
      if (tokens.id_token) {
        const idClaims = decodeJwt(tokens.id_token);
        if (stored.nonce && idClaims?.nonce && idClaims.nonce !== stored.nonce) {
          throw new Error('Некорректный nonce в id_token');
        }
      }

      getStorage().removeItem(PKCE_STORAGE_KEY);
      const { roles } = applyTokens(tokens);
      callbackProcessed = true;
      const redirectTarget = roles.includes('ROLE_GOD') ? '/admin-dashboard' : '/orders?my=1';
      window.history.replaceState({}, document.title, redirectTarget);
      return redirectTarget;
    } catch (err) {
      console.error('Callback error:', err);
      setAuthError(err.message || 'Не удалось завершить вход');
      getStorage().removeItem(PKCE_STORAGE_KEY);
      throw err;
    }
  }, [applyTokens, exchangeCode]);

  const refreshToken = useCallback(async () => {
    try {
      const stored = getStoredAuth();
      const storedRefreshToken = stored?.refresh_token;
      if (!storedRefreshToken) {
        clearAuth();
        setUser(null);
        return { success: false };
      }

      const params = new URLSearchParams();
      params.append('grant_type', 'refresh_token');
      params.append('client_id', CLIENT_ID);
      params.append('refresh_token', storedRefreshToken);

      const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Ошибка обновления токена');
      }

      applyTokens({
        ...data,
        refresh_token: data.refresh_token || storedRefreshToken
      });
      return { success: true };
    } catch (error) {
      clearAuth();
      setUser(null);
      return { success: false };
    }
  }, [applyTokens]);

  const value = {
    user,
    loading,
    login,
    logout,
    handleCallback,
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
