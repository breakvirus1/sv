import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Box, Button, Typography, Alert, TextField, Paper } from '@mui/material'

const LoginPage = () => {
  const navigate = useNavigate()
  const { loginWithPassword, isAuthenticated, authError, user, setAuthError } = useAuth()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const showError = error || authError

  if (isAuthenticated) {
    return (
      <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" minHeight="100vh" gap={2}>
        <Typography variant="h5">Вы уже авторизованы</Typography>
        <Button variant="contained" color="primary" onClick={() => navigate(user?.roles?.includes('ROLE_GOD') ? '/admin-dashboard' : '/orders?my=1')}>
          Перейти
        </Button>
      </Box>
    )
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setAuthError?.(null)

    if (!username.trim() || !password) {
      setError('Введите логин и пароль')
      return
    }

    setSubmitting(true)
    try {
      const { roles } = await loginWithPassword(username.trim(), password)
      navigate(roles?.includes('ROLE_GOD') ? '/admin-dashboard' : '/orders?my=1', { replace: true })
    } catch (err) {
      setError(err.message || 'Не удалось выполнить вход')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" minHeight="100vh" gap={3} px={2}>
      <Paper elevation={3} sx={{ p: { xs: 3, sm: 4 }, width: '100%', maxWidth: 400 }}>
        {showError && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => { setError(''); setAuthError?.(null) }}>
            {showError}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit} noValidate>
          <TextField
            label="Логин"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            fullWidth
            margin="normal"
            autoFocus
            autoComplete="username"
            disabled={submitting}
          />
          <TextField
            label="Пароль"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            fullWidth
            margin="normal"
            autoComplete="current-password"
            disabled={submitting}
          />
          <Button
            type="submit"
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            sx={{ mt: 2 }}
            disabled={submitting}
          >
            {submitting ? 'Входим...' : 'Войти'}
          </Button>
        </Box>
      </Paper>
    </Box>
  )
}

export default LoginPage
