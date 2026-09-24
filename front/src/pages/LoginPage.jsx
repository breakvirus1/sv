import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Box, Button, Typography, TextField, Alert } from '@mui/material'

const LoginPage = () => {
  const navigate = useNavigate()
  const { loginWithPassword, login, isAuthenticated, authError, setAuthError, user } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handlePasswordLogin = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setAuthError(null)
    const result = await loginWithPassword(username, password)
    setSubmitting(false)
    if (result.success) {
      const roles = user?.roles || []
      if (roles.includes('ROLE_PRODUCTION')) {
        navigate('/production/orders')
      } else {
        navigate('/orders')
      }
    }
  }

  const handleKeycloakLogin = () => {
    login()
  }

  if (isAuthenticated) {
    return (
      <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" minHeight="100vh" gap={2}>
        <Typography variant="h5">Вы уже авторизованы</Typography>
        <Button variant="contained" color="primary" onClick={() => navigate('/orders')}>
          Перейти к заказам
        </Button>
      </Box>
    )
  }

  return (
    <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" minHeight="100vh" gap={3} px={2}>

      <Box component="form" onSubmit={handlePasswordLogin} display="flex" flexDirection="column" gap={2} width="100%" maxWidth={400}>
        <TextField label="Логин" value={username} onChange={(e) => setUsername(e.target.value)} required fullWidth autoFocus />
        <TextField label="Пароль" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required fullWidth />
        {authError && <Alert severity="error">{authError}</Alert>}
        <Button type="submit" variant="contained" color="primary" size="large" fullWidth disabled={submitting}>
          {submitting ? 'Вход...' : 'Войти'}
        </Button>
      </Box>


      <Typography variant="caption" color="text.secondary" textAlign="center">
        Тестовые пользователи: 
        admin manager production
      </Typography>
    </Box>
  )
}

export default LoginPage