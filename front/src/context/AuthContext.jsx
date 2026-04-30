import { createContext, useContext, useEffect, useState } from 'react';
import { UserManager, WebStorageStateStore } from 'oidc-client-ts';

const userManager = new UserManager({
  authority: import.meta.env.VITE_KEYCLOAK_ISSUER,
  client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'frontend',
  redirect_uri: 'http://localhost:5174/callback',
  post_logout_redirect_uri: 'http://localhost:5174/',
  response_type: 'code',
  scope: 'openid profile email',
  userStore: new WebStorageStateStore({ store: window.localStorage })
});

const AuthContext = createContext(null);

// Helper to decode JWT payload (base64url)
function decodeJwt(token) {
  try {
    const payload = token.split('.')[1];
    // Convert base64url to base64
    let base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const pad = base64.length % 4;
    if (pad) base64 += '='.repeat(4 - pad);
    const decoded = atob(base64);
    return JSON.parse(decoded);
  } catch (e) {
    console.error('JWT decode error:', e);
    return null;
  }
}

function extractRoles(user) {
  let roles = [];
  
  // Try ID token profile first
  if (user.profile?.realm_access?.roles) {
    roles = user.profile.realm_access.roles;
  } 
  // Fallback: decode access token
  else if (user.access_token) {
    const decoded = decodeJwt(user.access_token);
    if (decoded?.realm_access?.roles) {
      roles = decoded.realm_access.roles;
    }
  }
  
  return roles.map(role => 
    role.startsWith('ROLE_') ? role : `ROLE_${role}`
  );
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      try {
        const storedUser = await userManager.getUser();
        console.log('Stored user:', storedUser);
        if (storedUser && !storedUser.expired) {
          const roles = extractRoles(storedUser);
          console.log('User roles (raw):', storedUser.profile?.realm_access?.roles, '→ transformed:', roles);
          
          setUser({
            name: storedUser.profile?.name || storedUser.profile?.preferred_username,
            email: storedUser.profile?.email,
            roles: roles,
            accessToken: storedUser.access_token
          });
          localStorage.setItem('token', storedUser.access_token);
        }
      } catch (err) {
        console.error('Auth init error:', err);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = async () => {
    try {
      await userManager.signinRedirect();
    } catch (err) {
      console.error('Login error:', err);
    }
  };

  const logout = async () => {
    try {
      localStorage.removeItem('token');
      await userManager.signoutRedirect();
    } catch (err) {
      console.error('Logout error:', err);
    }
  };

  const handleCallback = async () => {
    try {
      const oidcUser = await userManager.signinRedirectCallback();
      console.log('Callback user:', oidcUser);
      const roles = extractRoles(oidcUser);
      
      setUser({
        name: oidcUser.profile?.name || oidcUser.profile?.preferred_username,
        email: oidcUser.profile?.email,
        roles: roles,
        accessToken: oidcUser.access_token
      });
      localStorage.setItem('token', oidcUser.access_token);
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
