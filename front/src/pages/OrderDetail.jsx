import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Container,
  Typography,
  Paper,
  Tabs,
  Tab,
  Button,
  CircularProgress,
  Alert,
  Chip,
  Divider,
  Grid,
  TextField,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions
} from '@mui/material';
import { ArrowBack, Add, Payment } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';

const OrderDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState(0);
  const [paymentDialogOpen, setPaymentDialogOpen] = useState(false);
  const [statusDialogOpen, setStatusDialogOpen] = useState(false);
  const [newStatus, setNewStatus] = useState('');

  const { data: order, isLoading, error } = useQuery({
    queryKey: ['order', id],
    queryFn: async () => {
      const response = await api.get(`/api/v1/orders/${id}`);
      return response.data;
    },
  });

  const updateStatusMutation = useMutation({
    mutationFn: (status) => api.put(`/api/v1/orders/${id}/status?status=${status}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      setStatusDialogOpen(false);
    }
  });

  const addPaymentMutation = useMutation({
    mutationFn: (payment) => api.post(`/api/v1/orders/${id}/payments`, payment),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      setPaymentDialogOpen(false);
    }
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
      <Container maxWidth="xl" sx={{ mt: 4 }}>
        <Alert severity="error">Ошибка загрузки заказа: {error.message}</Alert>
      </Container>
    );
  }

  const statusOptions = ['WAITING', 'LAUNCHED', 'IN_PROGRESS', 'READY', 'ACCEPTED', 'CLOSED'];

  return (
    <Container maxWidth="xl" sx={{ mt: 4 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Button startIcon={<ArrowBack />} onClick={() => navigate('/orders')}>
            Назад
          </Button>
          <Typography variant="h4">
            Заказ #{order?.orderNumber}
          </Typography>
          <Chip
            label={order?.status}
            color={getStatusColor(order?.status)}
            size="medium"
          />
        </Box>
        <Box display="flex" gap={1}>
          <Button
            variant="outlined"
            onClick={() => {
              setNewStatus(order?.status);
              setStatusDialogOpen(true);
            }}
          >
            Изменить статус
          </Button>
          <Button
            variant="contained"
            startIcon={<Payment />}
            onClick={() => setPaymentDialogOpen(true)}
          >
            Добавить оплату
          </Button>
        </Box>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>Информация о заказе</Typography>
            <Box display="flex" flexDirection="column" gap={2}>
              <Box>
                <Typography variant="body2" color="text.secondary">Клиент</Typography>
                <Typography variant="body1">{order?.client?.name}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Описание</Typography>
                <Typography variant="body1">{order?.description || '—'}</Typography>
              </Box>
              <Divider />
              <Box>
                <Typography variant="body2" color="text.secondary">Сумма</Typography>
                <Typography variant="h6">{order?.totalAmount?.toFixed(2)} ₽</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Оплачено</Typography>
                <Typography variant="body1" color="success.main">{order?.paidAmount?.toFixed(2)} ₽</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Долг</Typography>
                <Typography variant="body1" color="error.main">{order?.debtAmount?.toFixed(2)} ₽</Typography>
              </Box>
            </Box>
          </Paper>

          {/* Табы для деталей заказа */}
          <Paper sx={{ mt: 3 }}>
            <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)}>
              <Tab label="Позиции" />
              <Tab label="Этапы" />
              <Tab label="Оплаты" />
              <Tab label="Комментарии" />
            </Tabs>
            <Divider />
            <Box sx={{ p: 3 }}>
              {activeTab === 0 && (
                <PositionsTab items={order?.items || []} />
              )}
              {activeTab === 1 && (
                <StagesTab stages={order?.stages || []} />
              )}
              {activeTab === 2 && (
                <PaymentsTab payments={order?.payments || []} />
              )}
              {activeTab === 3 && (
                <CommentsTab comments={order?.comments || []} />
              )}
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>Детали</Typography>
            <Box display="flex" flexDirection="column" gap={2}>
              <div>
                <Typography variant="body2" color="text.secondary">Дата заказа</Typography>
                <Typography variant="body1">{order?.orderDate || '—'}</Typography>
              </div>
              <div>
                <Typography variant="body2" color="text.secondary">Срок сдачи</Typography>
                <Typography variant="body1">{order?.dueDate || '—'}</Typography>
              </div>
              <div>
                <Typography variant="body2" color="text.secondary">Менеджер</Typography>
                <Typography variant="body1">{order?.manager?.fullName || '—'}</Typography>
              </div>
              <Divider />
              <div>
                <Typography variant="body2" color="text.secondary">Изменен</Typography>
                <Typography variant="body1">
                  {new Date(order?.updatedAt).toLocaleString()}
                </Typography>
              </div>
            </Box>
          </Paper>
        </Grid>
      </Grid>

      {/* Диалог изменения статуса */}
      <Dialog open={statusDialogOpen} onClose={() => setStatusDialogOpen(false)}>
        <DialogTitle>Изменить статус заказа</DialogTitle>
        <DialogContent>
          <TextField
            select
            fullWidth
            margin="dense"
            label="Статус"
            value={newStatus}
            onChange={(e) => setNewStatus(e.target.value)}
          >
            {statusOptions.map((status) => (
              <MenuItem key={status} value={status}>
                {status}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setStatusDialogOpen(false)}>Отмена</Button>
          <Button
            onClick={() => updateStatusMutation.mutate(newStatus)}
            variant="contained"
          >
            Сохранить
          </Button>
        </DialogActions>
      </Dialog>

      {/* Диалог добавления оплаты */}
      <Dialog open={paymentDialogOpen} onClose={() => setPaymentDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Добавить оплату</DialogTitle>
        <DialogContent>
          <PaymentForm onSubmit={(data) => addPaymentMutation.mutate(data)} />
        </DialogContent>
      </Dialog>
    </Container>
  );
};

const getStatusColor = (status) => {
  const colors = {
    WAITING: 'warning',
    LAUNCHED: 'info',
    IN_PROGRESS: 'primary',
    READY: 'success',
    ACCEPTED: 'success',
    CLOSED: 'default'
  };
  return colors[status] || 'default';
};

const PositionsTab = ({ items }) => {
  if (!items.length) {
    return <Typography>Нет позиций в заказе</Typography>;
  }

  return (
    <Box>
      {items.map((item, index) => (
        <Paper key={item.id} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="h6">{item.name}</Typography>
            <Typography variant="h6">{item.cost?.toFixed(2)} ₽</Typography>
          </Box>
          <Box display="flex" gap={4} mt={1}>
            <Typography variant="body2" color="text.secondary">
              Цена: {item.price?.toFixed(2)} ₽
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Кол-во: {item.quantity}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Срок: {item.readyDate || '—'}
            </Typography>
          </Box>
        </Paper>
      ))}
    </Box>
  );
};

const StagesTab = ({ stages }) => {
  if (!stages.length) {
    return <Typography>Этапы производства не заданы</Typography>;
  }

  return (
    <Box>
      {stages.map((stage) => (
        <Paper key={stage.id} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="subtitle1">{stage.workshop?.name}</Typography>
            <Chip
              label={stage.status}
              color={stage.status === 'DONE' ? 'success' : stage.status === 'IN_PROGRESS' ? 'primary' : 'default'}
              size="small"
            />
          </Box>
          <Box mt={1}>
            <Typography variant="body2" color="text.secondary">
              Срок: {stage.dueDate || '—'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Ждать предыдущие: {stage.waitPrevious ? 'Да' : 'Нет'}
            </Typography>
            {stage.note && (
              <Typography variant="body2" color="text.secondary">
                Примечание: {stage.note}
              </Typography>
            )}
          </Box>
        </Paper>
      ))}
    </Box>
  );
};

const PaymentsTab = ({ payments }) => {
  if (!payments.length) {
    return <Typography>Нет оплат</Typography>;
  }

  return (
    <Box>
      {payments.map((payment) => (
        <Paper key={payment.id} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Box>
              <Typography variant="subtitle1">{payment.paymentType}</Typography>
              <Typography variant="body2" color="text.secondary">
                {payment.paymentDate}
              </Typography>
            </Box>
            <Typography variant="h6">{payment.amount?.toFixed(2)} ₽</Typography>
          </Box>
          {payment.details && (
            <Typography variant="body2" color="text.secondary" mt={1}>
              {payment.details}
            </Typography>
          )}
        </Paper>
      ))}
    </Box>
  );
};

const CommentsTab = ({ comments }) => {
  if (!comments.length) {
    return <Typography>Нет комментариев</Typography>;
  }

  return (
    <Box>
      {comments.map((comment) => (
        <Paper key={comment.id} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
            <Typography variant="subtitle1">{comment.author?.fullName || 'Система'}</Typography>
            <Typography variant="caption" color="text.secondary">
              {new Date(comment.timestamp).toLocaleString()}
            </Typography>
          </Box>
          <Typography variant="body1">{comment.message}</Typography>
          {comment.isInternal && (
            <Chip label="Внутренний" size="small" sx={{ mt: 1 }} />
          )}
        </Paper>
      ))}
    </Box>
  );
};

const PaymentForm = ({ onSubmit }) => {
  const [formData, setFormData] = useState({
    amount: '',
    paymentDate: new Date().toISOString().split('T')[0],
    paymentType: 'Безнал',
    details: '',
    isPartial: false
  });

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      ...formData,
      amount: parseFloat(formData.amount)
    });
  };

  return (
    <form onSubmit={handleSubmit}>
      <TextField
        fullWidth
        margin="dense"
        label="Сумма"
        name="amount"
        type="number"
        value={formData.amount}
        onChange={handleChange}
        required
      />
      <TextField
        fullWidth
        margin="dense"
        label="Дата оплаты"
        name="paymentDate"
        type="date"
        value={formData.paymentDate}
        onChange={handleChange}
        required
      />
      <TextField
        fullWidth
        margin="dense"
        label="Вид оплаты"
        name="paymentType"
        select
        value={formData.paymentType}
        onChange={handleChange}
      >
        <MenuItem value="Безнал">Безнал</MenuItem>
        <MenuItem value="Нал">Нал</MenuItem>
        <MenuItem value="Карта">Карта</MenuItem>
      </TextField>
      <TextField
        fullWidth
        margin="dense"
        label="Примечание"
        name="details"
        multiline
        rows={2}
        value={formData.details}
        onChange={handleChange}
      />
      <DialogActions sx={{ mt: 2 }}>
        <Button type="button" onClick={() => {}}>Отмена</Button>
        <Button type="submit" variant="contained">Добавить</Button>
      </DialogActions>
    </form>
  );
};

export default OrderDetail;
