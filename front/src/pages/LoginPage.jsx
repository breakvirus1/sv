import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Box, Button, Typography, TextField, Alert } from '@mui/material'

const LoginPage = () => {
  const navigate = useNavigate()
  const { loginWithPassword, login, isAuthenticated, authError, setAuthError, user } = useAuth()

  const getRedirectPath = () => {
    if (user?.roles?.includes('ROLE_ADMIN') || user?.roles?.includes('ROLE_MANAGER')) return '/manager'
    if (user?.roles?.includes('ROLE_PRODUCTION')) return '/production'
    return '/orders'
  }
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handlePasswordLogin = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setAuthError(null)
    const result = await loginWithPassword(username, password)
    setSubmitting(false)
  }

  useEffect(() => {
    if (isAuthenticated) {
      const path = getRedirectPath()
      navigate(path, { replace: true })
    }
  }, [isAuthenticated])

  const handleKeycloakLogin = () => {
    login()
  }

  if (isAuthenticated) {
    return (
      <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" minHeight="100vh" gap={2}>
        <Typography variant="h5">Вы уже авторизованы</Typography>
        <Button variant="contained" color="primary" onClick={() => navigate(getRedirectPath())}>
          Перейти к заказам
        </Button>
      </Box>
    )
  }

  return (
    <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" minHeight="100vh" gap={3} px={2}>
      <Box textAlign="center">
        <Typography variant="h4" gutterBottom>Авторизация</Typography>
        <Typography variant="h6" color="text.secondary">Система управления производством</Typography>
      </Box>

      <Box component="form" onSubmit={handlePasswordLogin} display="flex" flexDirection="column" gap={2} width="100%" maxWidth={400}>
        <TextField label="Логин" value={username} onChange={(e) => setUsername(e.target.value)} required fullWidth autoFocus />
        <TextField label="Пароль" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required fullWidth />
        {authError && <Alert severity="error">{authError}</Alert>}
        <Button type="submit" variant="contained" color="primary" size="large" fullWidth disabled={submitting}>
          {submitting ? 'Вход...' : 'Войти'}
        </Button>
      </Box>

      <Typography variant="body2" color="text.secondary">
        или
      </Typography>

      <Button variant="outlined" color="primary" size="large" fullWidth onClick={handleKeycloakLogin} sx={{ maxWidth: 400 }}>
        Войти через Keycloak
      </Button>

      <Typography variant="caption" color="text.secondary" textAlign="center">
        Тестовые пользователи: admin/admin, manager/manager, production/production, accountant/accountant
      </Typography>
    </Box>
  )
}

export default LoginPage