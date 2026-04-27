import { useAuth } from '../context/AuthContext'
import { Box, Button, Card, CardContent, Typography, Container } from '@mui/material'

const LoginPage = () => {
  const { login, isAuthenticated } = useAuth()

  const handleLogin = () => {
    login()
  }

  if (isAuthenticated) {
    return (
      <Container maxWidth="sm">
        <Box mt={10}>
          <Card>
            <CardContent>
              <Typography variant="h5" gutterBottom>
                Вы уже авторизованы
              </Typography>
              <Button
                variant="contained"
                color="primary"
                fullWidth
                onClick={() => window.location.href = '/orders'}
              >
                Перейти к заказам
              </Button>
            </CardContent>
          </Card>
        </Box>
      </Container>
    )
  }

  return (
    <Container maxWidth="sm">
      <Box mt={10}>
        <Card>
          <CardContent>
            <Typography variant="h4" gutterBottom align="center" sx={{ mb: 3 }}>
              Print SV
            </Typography>
            <Typography variant="h6" gutterBottom align="center" sx={{ mb: 4 }}>
              Система управления производством
            </Typography>
            <Typography variant="body1" paragraph align="center" sx={{ mb: 3 }}>
              Войдите через Keycloak для доступа к системе
            </Typography>
            <Button
              variant="contained"
              color="primary"
              size="large"
              fullWidth
              onClick={handleLogin}
              sx={{ py: 1.5 }}
            >
              Войти в систему
            </Button>
            <Box mt={3} textAlign="center">
              <Typography variant="caption" color="text.secondary">
                Тестовые пользователи: admin/admin, manager/manager, production/production, accountant/accountant
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </Container>
  )
}

export default LoginPage