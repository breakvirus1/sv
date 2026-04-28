import { useQuery } from '@tanstack/react-query';
import { Grid, Paper, Typography, Box, Button, Card, CardContent } from '@mui/material';
import { People, Inventory, Payments, Assessment } from '@mui/icons-material';
import { Link } from 'react-router-dom';
import api from '../services/api';

const Dashboard = () => {
  const { data: orders = [] } = useQuery({
    queryKey: ['recentOrders'],
    queryFn: async () => {
      const response = await api.get('/api/v1/orders?size=5');
      return response.data.content || [];
    },
  });

  const stats = {
    totalOrders: orders.length,
    pendingOrders: orders.filter(o => o.status === 'WAITING').length,
    inProgressOrders: orders.filter(o => o.status === 'IN_PROGRESS').length,
    totalRevenue: orders.reduce((sum, o) => sum + (o.paidAmount || 0), 0),
  };

  const statCards = [
    { title: 'Всего заказов', value: stats.totalOrders, icon: <Inventory />, color: '#1976d2' },
    { title: 'Ожидают', value: stats.pendingOrders, icon: <Assessment />, color: '#ff9800' },
    { title: 'В работе', value: stats.inProgressOrders, icon: <Inventory />, color: '#9c27b0' },
    { title: 'Оплачено, ₽', value: stats.totalRevenue.toFixed(2), icon: <Payments />, color: '#4caf50' },
  ];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Панель управления
      </Typography>

      <Grid container spacing={3}>
        {statCards.map((stat, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" justifyContent="space-between">
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      {stat.title}
                    </Typography>
                    <Typography variant="h5" sx={{ mt: 1 }}>
                      {stat.value}
                    </Typography>
                  </Box>
                  <Box sx={{ color: stat.color }}>
                    {stat.icon}
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
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
                <Typography variant="caption" color="text.secondary">
                  {order.status}
                </Typography>
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
