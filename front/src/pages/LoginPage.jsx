import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Box, Button, Typography, Alert } from '@mui/material'

const LoginPage = () => {
  const navigate = useNavigate()
  const { login, isAuthenticated, authError, user } = useAuth()

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

  return (
    <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" minHeight="100vh" gap={3} px={2}>
      <Typography variant="h4">Print SV</Typography>
      <Typography variant="body1" color="text.secondary" textAlign="center">
        Вход выполняется через корпоративный Keycloak (OAuth2 Authorization Code + PKCE)
      </Typography>
      {authError && <Alert severity="error">{authError}</Alert>}
      <Button variant="contained" color="primary" size="large" onClick={login}>
        Войти
      </Button>
    </Box>
  )
}

export default LoginPage
