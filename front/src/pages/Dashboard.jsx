import { useQuery } from '@tanstack/react-query';
import {
  Grid,
  Paper,
  Typography,
  Box,
  Button,
  Card,
  CardContent,
  Chip
} from '@mui/material';
import {
  Inventory,
  Payments,
  Add
} from '@mui/icons-material';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { getStatusColor, getStatusLabel } from '../utils/orderUtils';

const Dashboard = () => {
  const navigate = useNavigate();

  const { data: orders = [] } = useQuery({
    queryKey: ['recentOrders'],
    queryFn: async () => {
      const response = await api.get('/api/v1/orders?size=50');
      return response.data.content || [];
    },
  });

  const navigate = useNavigate();

  const handleCreateOrder = () => {
    navigate('/orders/new');
  };

  const totalRevenue = orders.reduce((sum, o) => sum + (o.paidAmount || 0), 0);

  return (
    <Box sx={{ mt: 4, width: '1900px', mx: 'auto', overflowX: 'auto' }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h4">
          Панель управления
        </Typography>
        <Button variant="contained" startIcon={<Add />} onClick={handleCreateOrder}>
          Создать заказ
        </Button>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" justifyContent="space-between">
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Всего заказов
                  </Typography>
                  <Typography variant="h5" sx={{ mt: 1 }}>
                    {orders.length}
                  </Typography>
                </Box>
                <Box sx={{ color: '#1976d2' }}>
                  <Inventory />
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" justifyContent="space-between">
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Оплачено
                  </Typography>
                  <Typography variant="h5" sx={{ mt: 1 }}>
                    {totalRevenue.toFixed(2)} ₽
                  </Typography>
                </Box>
                <Box sx={{ color: '#4caf50' }}>
                  <Payments />
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Box mt={4}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h6">Последние заказы</Typography>
          <Button component={Link} to="/orders" variant="outlined" size="small">
            Все заказы
          </Button>
        </Box>
        <Paper sx={{ p: 2 }}>
          {orders.slice(0, 5).map(order => (
            <Box
              key={order.id}
              display="flex"
              justifyContent="space-between"
              alignItems="center"
              p={2}
              borderBottom="1px solid"
              borderColor="divider"
              component={Link}
              to={`/orders/${order.id}`}
              sx={{
                textDecoration: 'none',
                color: 'inherit',
                '&:hover': { backgroundColor: 'action.hover' }
              }}
            >
              <Box>
                <Typography variant="subtitle1">#{order.orderNumber}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {order.client?.name}
                </Typography>
              </Box>
              <Box textAlign="right">
                <Typography variant="body1">{order.totalAmount?.toFixed(2)} ₽</Typography>
                <Chip
                  label={getStatusLabel(order.status)}
                  color={getStatusColor(order.status)}
                  size="small"
                />
              </Box>
            </Box>
          ))}
          {orders.length === 0 && (
            <Typography p={2} textAlign="center" color="text.secondary">
              Нет заказов
            </Typography>
          )}
        </Paper>
      </Box>
    </Box>
  );
};

export default Dashboard;
