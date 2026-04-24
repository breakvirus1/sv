import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

export const useAuth = () => useContext(AuthContext)

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const hash = window.location.hash
    if (hash && hash.includes('access_token')) {
      const params = new URLSearchParams(hash.substring(1))
      const token = params.get('access_token')
      if (token) {
        localStorage.setItem('token', token)
        const payload = JSON.parse(atob(token.split('.')[1]))
        setUser({
          username: payload.preferred_username || payload.sub,
          roles: payload.roles || [],
          ...payload
        })
        window.location.hash = ''
      }
    } else {
      const token = localStorage.getItem('token')
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]))
        setUser({
          username: payload.preferred_username || payload.sub,
          roles: payload.roles || [],
          ...payload
        })
      }
    }
    setLoading(false)
  }, [])

  const login = () => {
    const state = Math.random().toString(36).substring(7)
    localStorage.setItem('oauth_state', state)
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: 'web-app',
      redirect_uri: `${window.location.origin}/callback`,
      scope: 'openid read write',
      state: state
    })
    // Use relative path to go through gateway/nginx
    window.location.href = `/auth/oauth2/authorize?${params}`
  }

  const logout = () => {
    localStorage.removeItem('token')
    setUser(null)
    window.location.href = '/'
  }

  const handleCallback = async (code, state) => {
    const savedState = localStorage.getItem('oauth_state')
    if (state !== savedState) {
      throw new Error('Invalid state parameter')
    }

    // Exchange authorization code for tokens at token endpoint
    const response = await fetch('/auth/oauth2/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': 'Basic ' + btoa('web-app:secret')
      },
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        code,
        redirect_uri: `${window.location.origin}/callback`
      })
    })

    if (!response.ok) {
      throw new Error('Failed to obtain token')
    }

    const data = await response.json()
    localStorage.setItem('token', data.access_token)
    const payload = JSON.parse(atob(data.access_token.split('.')[1]))
    setUser({
      username: payload.preferred_username || payload.sub,
      roles: payload.roles || [],
      ...payload
    })
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, handleCallback }}>
      {children}
    </AuthContext.Provider>
  )
}