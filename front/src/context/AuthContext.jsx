import { createContext, useContext, useEffect, useState } from 'react';
import { UserManager, WebStorageStateStore } from 'oidc-client-ts';

const userManager = new UserManager({
  authority: 'http://localhost:8080/realms/print-sv',
  client_id: 'frontend',
  redirect_uri: 'http://localhost:5174/callback',
  post_logout_redirect_uri: 'http://localhost:5174/',
  response_type: 'code',
  scope: 'openid profile email',
  userStore: new WebStorageStateStore({ store: window.localStorage })
});

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      try {
        const storedUser = await userManager.getUser();
        if (storedUser && !storedUser.expired) {
          setUser({
            name: storedUser.profile?.name || storedUser.profile?.preferred_username,
            email: storedUser.profile?.email,
            roles: storedUser.profile?.realm_access?.roles || [],
            accessToken: storedUser.access_token
          });
          // Сохраняем токен для api.js
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
      const user = await userManager.signinRedirectCallback();
      setUser({
        name: user.profile?.name || user.profile?.preferred_username,
        email: user.profile?.email,
        roles: user.profile?.realm_access?.roles || [],
        accessToken: user.access_token
      });
      // Сохраняем токен для api.js
      localStorage.setItem('token', user.access_token);
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

