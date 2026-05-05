import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Container, Typography, Paper, Tabs, Tab, Button, CircularProgress, Alert, Snackbar, Divider
} from '@mui/material';
import { ArrowBack } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import EstimateCalculator from '../features/orders/components/EstimateCalculator';

const OrderItemDetail = () => {
  const { orderId, itemId } = useParams();
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState(0);
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });

  // Fetch order to get item details
  const { data: order, isLoading: orderLoading, error: orderError } = useQuery({
    queryKey: ['order', orderId],
    queryFn: async () => {
      const response = await api.get(`/api/v1/orders/${orderId}`);
      return response.data;
    },
    enabled: !!orderId
  });

  // Find the specific order item
  const orderItem = order?.items?.find(item => item.id === parseInt(itemId));

  // Fetch current estimate
  const { data: estimateData, isLoading: estimateLoading, refetch: refetchEstimate } = useQuery({
    queryKey: ['estimate', itemId],
    queryFn: async () => {
      const response = await api.get(`/api/v1/orders/items/${itemId}/estimate`);
      return response.data;
    },
    enabled: !!itemId
  });

  const handleSaveEstimate = async (data) => {
    try {
      await api.post(`/api/v1/orders/items/${itemId}/estimate`, data);
      setNotification({ open: true, message: 'Смета сохранена', severity: 'success' });
      refetchEstimate();
    } catch (err) {
      setNotification({ open: true, message: `Ошибка: ${err.response?.data?.message || err.message}`, severity: 'error' });
    }
  };

  if (orderLoading) {
    return (
      <Container maxWidth="xl" sx={{ mt: 4 }}>
        <CircularProgress />
      </Container>
    );
  }

  if (orderError || !orderItem) {
    return (
      <Container maxWidth="xl" sx={{ mt: 4 }}>
        <Alert severity="error">Позиция заказа не найдена</Alert>
        <Button startIcon={<ArrowBack />} onClick={() => navigate(-1)} sx={{ mt: 2 }}>
          Назад
        </Button>
      </Container>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ mt: 4 }}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate(`/orders/${orderId}`)}>
          Назад к заказу
        </Button>
        <Typography variant="h4">Позиция: {orderItem.name}</Typography>
      </Box>

      <Paper sx={{ p: 3 }}>
        <Box display="flex" justifyContent="space-between" mb={2}>
          <Typography variant="h6">Детали позиции</Typography>
          <Box>
            <Typography variant="body2" color="text.secondary">
              Кол-во: {orderItem.quantity} шт.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Цена за ед.: {Number(orderItem.price).toFixed(2)} ₽
            </Typography>
            <Typography variant="h6">
              Итого: {Number(orderItem.cost).toFixed(2)} ₽
            </Typography>
          </Box>
        </Box>
        <Divider sx={{ my: 2 }} />
        {orderItem.productId && (
          <Typography variant="body2" color="text.secondary">
            Шаблон продукта: {orderItem.product?.name || '—'}
          </Typography>
        )}
      </Paper>

      <Paper sx={{ mt: 3 }}>
        <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)} sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tab label="Смета" />
        </Tabs>
        <Box sx={{ p: 3 }}>
          {activeTab === 0 && (
            estimateLoading ? (
              <CircularProgress />
            ) : (
              <EstimateCalculator
                orderItemId={parseInt(itemId)}
                productId={orderItem.productId || (estimateData?.productId) || null}
                initialData={estimateData}
                onSave={handleSaveEstimate}
              />
            )
          )}
        </Box>
      </Paper>

      <Snackbar
        open={notification.open}
        autoHideDuration={6000}
        onClose={() => setNotification({ ...notification, open: false })}
      >
        <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })}>
          {notification.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default OrderItemDetail;
