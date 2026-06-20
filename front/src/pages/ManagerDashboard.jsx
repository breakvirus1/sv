import { useNavigate } from 'react-router-dom';
import {
  Box, Container, Typography, Paper, Grid, Chip, Button,
  CircularProgress, Alert, Card, CardContent, Divider
} from '@mui/material';
import { ArrowBack, CheckCircle, HourglassEmpty, PendingActions } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const ManagerDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const username = user?.username;

  const { data: currentEmployee } = useQuery({
    queryKey: ['currentEmployee', username],
    queryFn: async () => {
      if (!username) return null;
      const response = await api.get(`/api/v1/employees?size=1&q=${username}`);
      const data = response.data.content || [];
      return data.length > 0 ? data[0] : null;
    },
    enabled: !!username,
  });

  const { data: earnings, isLoading, error } = useQuery({
    queryKey: ['managerEarnings', currentEmployee?.id],
    queryFn: async () => {
      const response = await api.get(`/api/v1/orders/manager-earnings/${currentEmployee.id}`);
      return response.data;
    },
    enabled: !!currentEmployee?.id,
    refetchInterval: 30000,
  });

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Alert severity="error">Ошибка загрузки данных: {error.message}</Alert>
      </Container>
    );
  }

  const readyEarnings = earnings?.readyEarnings ?? 0;
  const inProgressEarnings = earnings?.inProgressEarnings ?? 0;
  const approvalEarnings = earnings?.approvalEarnings ?? 0;
  const readyOrdersCount = earnings?.readyOrdersCount ?? 0;
  const inProgressOrdersCount = earnings?.inProgressOrdersCount ?? 0;
  const approvalOrdersCount = earnings?.approvalOrdersCount ?? 0;
  const managerCashPercent = earnings?.managerCashPercent ?? 0;

  return (
    <Container maxWidth="lg" sx={{ mt: 4, px: 2.5 }}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/manager')}>
          Назад
        </Button>
        <Typography variant="h4">Личный кабинет</Typography>
      </Box>

      <Typography variant="h6" color="text.secondary" gutterBottom>
        {earnings?.managerName || user?.name}
        {managerCashPercent > 0 && (
          <Chip label={`${managerCashPercent}% с прибыли`} size="small" sx={{ ml: 1 }} color="primary" variant="outlined" />
        )}
      </Typography>

      <Divider sx={{ mb: 3 }} />

      <Grid container spacing={3}>
        {/* Заработок с готовых заказов */}
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%', bgcolor: '#e8f5e9' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1} mb={2}>
                <CheckCircle sx={{ color: '#2e7d32' }} />
                <Typography variant="h6" color="success.dark">Готовые заказы</Typography>
              </Box>
              <Typography variant="h3" fontWeight={700} color="success.dark">
                {Number(readyEarnings).toFixed(2)} ₽
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                {readyOrdersCount} {readyOrdersCount === 1 ? 'заказ' : 'заказов'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Потенциальный заработок с заказов в работе */}
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%', bgcolor: '#fff3e0' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1} mb={2}>
                <HourglassEmpty sx={{ color: '#e65100' }} />
                <Typography variant="h6" color="warning.dark">В работе</Typography>
              </Box>
              <Typography variant="h3" fontWeight={700} color="warning.dark">
                {Number(inProgressEarnings).toFixed(2)} ₽
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                {inProgressOrdersCount} {inProgressOrdersCount === 1 ? 'заказ' : 'заказов'} (потенциально)
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Потенциальный заработок с заказов на согласовании */}
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%', bgcolor: '#fce4ec' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1} mb={2}>
                <PendingActions sx={{ color: '#c62828' }} />
                <Typography variant="h6" color="error.dark">На согласовании</Typography>
              </Box>
              <Typography variant="h3" fontWeight={700} color="error.dark">
                {Number(approvalEarnings).toFixed(2)} ₽
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                {approvalOrdersCount} {approvalOrdersCount === 1 ? 'заказ' : 'заказов'} (потенциально)
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Paper sx={{ mt: 3, p: 3 }}>
        <Typography variant="h6" gutterBottom>Информация</Typography>
        <Typography variant="body2" color="text.secondary">
          Заработок рассчитывается как процент от суммы прибыли (priceplus) с каждого заказа.
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          <strong>Готовые заказы</strong> — заказы со статусом "Готов", заработок по которым уже начислен.
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          <strong>В работе</strong> — заказы со статусом "В работе", показывают потенциальный заработок после завершения.
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          <strong>На согласовании</strong> — заказы со статусом "Согласование", показывают потенциальный заработок после утверждения.
        </Typography>
      </Paper>
    </Container>
  );
};

export default ManagerDashboard;
