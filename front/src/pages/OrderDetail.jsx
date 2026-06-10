import { useState, useEffect, useMemo } from 'react';
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
  TextField,
  Divider,
  Snackbar,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow
} from '@mui/material';
import { ArrowBack, Payment, Edit } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getStatusColor, getStatusLabel, isM2, isLinearMeter } from '../utils/orderUtils';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import CreateOrderForm from '../components/CreateOrderForm';

const OrderDetail = ({ mode = 'view' }) => {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const username = user?.username;
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');

  // ── Create mode state ──
  const [formData, setFormData] = useState({
    clientId: '',
    description: '',
    orderDate: new Date().toISOString().split('T')[0],
    dueDate: '',
    items: []
  });
  const [priceplus, setPriceplus] = useState(0);
  const [totalAmount, setTotalAmount] = useState(0);

  // ── Helper to determine endpoint based on identifier ──
  const getOrderEndpoint = (identifier) => {
    if (/^\d{14}$/.test(identifier)) {
      return `/api/v1/orders/number/${identifier}`;
    }
    return `/api/v1/orders/${identifier}`;
  };

  // ── Queries: Order, Calculated Data, Clients, Materials, Employees ──
  const { data: order, isLoading, error } = useQuery({
    queryKey: ['order', id],
    queryFn: async () => {
      const endpoint = getOrderEndpoint(id);
      const response = await api.get(endpoint);
      return response.data;
    },
    enabled: mode !== 'create'
  });

  const { data: calculatedData } = useQuery({
    queryKey: ['order-calculated', id],
    queryFn: async () => {
      const response = await api.get(`/api/v1/orders/${id}/calculated`);
      return response.data;
    },
    enabled: mode !== 'create' && !!order
  });

  const { data: clientsData = [] } = useQuery({
    queryKey: ['clients'],
    queryFn: async () => {
      const response = await api.get('/api/v1/clients?size=100');
      return response.data.content || [];
    },
    enabled: mode === 'create'
  });

  const { data: materialsData = [] } = useQuery({
    queryKey: ['materials'],
    queryFn: async () => {
      const response = await api.get('/api/v1/materials?size=100');
      return response.data.content || [];
    },
    enabled: mode === 'create'
  });

  const { data: currentEmployee, refetch: refetchEmployee } = useQuery({
    queryKey: ['currentEmployee', username],
    queryFn: async () => {
      if (!username) return null;
      const response = await api.get(`/api/v1/employees?size=1&q=${username}`);
      const data = response.data.content || [];
      return data.length > 0 ? data[0] : null;
    },
    enabled: !!username
  });

const { data: employeesData = [] } = useQuery({
    queryKey: ['employees'],
    queryFn: async () => {
      const response = await api.get('/api/v1/employees?size=100');
      return response.data.content || [];
    },
    enabled: mode === 'edit'
  });

// Получаем итоговую сумму из заказа (totalAmount from orders table)
    const totalOrderAmount = order?.totalAmount ?? 0;

    // Проверка прав на редактирование: автор заказа или ADMIN
    const canEdit = !!(currentEmployee && order?.manager && (isAdmin || currentEmployee.id === order.manager.id));

    // ==================== State ====================
    const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
    const [activeTab, setActiveTab] = useState(0);
    const [paymentDialogOpen, setPaymentDialogOpen] = useState(false);
    const [statusDialogOpen, setStatusDialogOpen] = useState(false);
    const [newStatus, setNewStatus] = useState('');
    const [clientDialogOpen, setClientDialogOpen] = useState(false);
    const [newClientForm, setNewClientForm] = useState({
      name: '',
      type: 'PRIVATE',
      contactPerson: '',
      phone: '',
      email: ''
    });
    const [clientInfoDialog, setClientInfoDialog] = useState({ open: false, clientId: null });

   // ==================== Mutations ====================
  const createClientMutation = useMutation({
    mutationFn: (client) => api.post('/api/v1/clients', client),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['clients'] });
      setClientDialogOpen(false);
      setNewClientForm({ name: '', type: 'PRIVATE', contactPerson: '', phone: '', email: '' });
      setFormData(prev => ({ ...prev, clientId: response.data.id.toString() }));
},
    onError: (err) => setNotification({ open: true, message: 'Ошибка создания клиента: ' + err.message, severity: 'error' })
  });

  const createOrderMutation = useMutation({
    mutationFn: async () => {
      if (!currentEmployee) {
        throw new Error('Менеджер не определен. Перелогинитесь.');
      }
      // Конвертация значения в метры в зависимости от единицы измерения
      const toMeters = (value, unit) => {
        const v = parseFloat(value) || 0;
        return unit === 'мм' ? v / 1000 : v;
      };

      // Формирование массива материалов заказа из формы
      const orderMaterials = formData.items.map(item => {
        const material = materialsData.find(m => m.id === parseInt(item.materialId));
        if (!material) throw new Error('Материал не выбран');

        const widthM = toMeters(item.qty1value, item.qty1unit);
        const heightM = isM2(material) ? toMeters(item.qty2value, item.qty2unit) : null;

        return {
          materialId: parseInt(item.materialId),
          widthM,
          heightM,
          readyDate: item.readyDate || null
        };
      });

      // Расчет итогов для логирования
      let frontendTotal = 0;
      formData.items.forEach((item, idx) => {
        const material = materialsData.find(m => m.id === parseInt(item.materialId));
        if (material) {
          const widthM = toMeters(item.qty1value, item.qty1unit);
          const heightM = isM2(material) ? toMeters(item.qty2value, item.qty2unit) : 0;
          const wasteCoeff = material.wasteCoefficient || 1;
          const materialCost = widthM * (heightM || 1) * material.price * wasteCoeff;
          const opsCost = (item.operations || []).reduce((sum, op) => sum + (op.subtotal || 0), 0);
          frontendTotal += materialCost + opsCost;
          console.log(`[CreateOrder] Item ${idx}: material=${material.name}, w=${widthM}, h=${heightM}, wasteCoeff=${wasteCoeff}, matCost=${materialCost.toFixed(2)}, opsCost=${opsCost.toFixed(2)}`);
        }
      });
      const frontendTotalWithPriceplus = frontendTotal * (1 + priceplus / 100);

      const orderData = {
        clientId: parseInt(formData.clientId),
        description: formData.description,
        orderDate: formData.orderDate,
        dueDate: formData.dueDate || null,
        managerId: currentEmployee.id,
        priceplus: priceplus,
        items: orderMaterials
      };

      console.log('=== CREATE ORDER SUBMIT ===');
      console.log('Client ID:', formData.clientId);
      console.log('Items count:', formData.items.length);
      console.log('Priceplus:', priceplus);
      console.log('Frontend total (without priceplus):', frontendTotal.toFixed(2));
      console.log('Frontend total (with priceplus):', frontendTotalWithPriceplus.toFixed(2));
      console.log('Payload:', JSON.stringify(orderData, null, 2));

      const response = await api.post('/api/v1/orders', orderData);

      console.log('=== CREATE ORDER RESPONSE ===');
      console.log('Created order ID:', response.data.id);
      console.log('Created order number:', response.data.orderNumber);
      console.log('Saved priceplus:', response.data.priceplus);
      console.log('Saved totalAmount (without priceplus):', response.data.totalAmount);
      console.log('Saved totalWithPriceplus:', response.data.totalWithPriceplus);
      console.log('Comparison - Frontend vs Backend totalWithPriceplus:');
      console.log('  Frontend:', frontendTotalWithPriceplus.toFixed(2));
      console.log('  Backend:', response.data.totalWithPriceplus?.toFixed(2));
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['orders'] });
      await queryClient.invalidateQueries({ queryKey: ['recentOrders'] });
      setNotification({ open: true, message: 'Заказ успешно создан', severity: 'success' });
      setTimeout(() => navigate('/orders'), 1500);
    },
    onError: (err) => {
      setNotification({ open: true, message: `Ошибка: ${err.response?.data?.message || err.message}`, severity: 'error' });
    }
  });

  const updateStatusMutation = useMutation({
    mutationFn: (status) => api.put(`/api/v1/orders/${id}/status?status=${status}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      setStatusDialogOpen(false);
      setNotification({ open: true, message: 'Статус обновлен', severity: 'success' });
    }
  });

  const addPaymentMutation = useMutation({
    mutationFn: (payment) => api.post(`/api/v1/orders/${id}/payments`, payment),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      setPaymentDialogOpen(false);
      setNotification({ open: true, message: 'Оплата добавлена', severity: 'success' });
    }
  });

  // ==================== Effects ====================
  useEffect(() => {
    if (mode === 'create' && username && !currentEmployee) {
      api.post('/api/v1/employees/sync')
        .then(() => refetchEmployee())
        .catch(err => console.error('Employee sync failed:', err));
    }
  }, [mode, username, currentEmployee, refetchEmployee]);

  // ==================== Handlers ====================
  const addItem = () => {
    setFormData(prev => ({
      ...prev,
      items: [...prev.items, { materialId: '', qty1value: '', qty1unit: 'м', qty2value: '', qty2unit: 'м', readyDate: '', operations: [] }]
    }));
  };

  const removeItem = (index) => {
    setFormData(prev => ({
      ...prev,
      items: prev.items.filter((_, i) => i !== index)
    }));
  };

  const updateItem = (index, field, value) => {
    setFormData(prev => {
      if (field === 'materialId' && value) {
        const dup = prev.items.find((item, i) => i !== index && item.materialId === value);
        if (dup) {
          const mat = materialsData.find(m => m.id === parseInt(value));
          setNotification({
            open: true,
            message: `Материал "${mat?.name || value}" уже выбран в другой позиции`,
            severity: 'warning'
          });
          return prev;
        }
      }
      return {
        ...prev,
        items: prev.items.map((item, i) => i === index ? { ...item, [field]: value } : item)
      };
    });
  };

  const handleClientChange = (e) => {
    const value = e.target.value;
    if (value === 'create_new') {
      setClientDialogOpen(true);
    } else {
      setFormData(prev => ({ ...prev, clientId: value }));
    }
  };

  const handleNewClientChange = (field) => (e) => {
    setNewClientForm(prev => ({ ...prev, [field]: e.target.value }));
  };

  const handleClientSubmit = () => {
    if (!newClientForm.name) {
      setNotification({ open: true, message: 'Введите название клиента', severity: 'error' });
      return;
    }
    createClientMutation.mutate(newClientForm);
  };

  const handleCreateSubmit = (e) => {
    e.preventDefault();
    createOrderMutation.mutate();
  };

  // ==================== Render ====================
  if (mode === 'create') {
    return <CreateOrderForm />;
  }

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    const isNotFound = error.response?.status === 404;
    return (
      <Container maxWidth="xl" sx={{ mt: 4, px: 2.5 }}>
        <Alert severity="error">{isNotFound ? 'Заказ не найден' : `Ошибка загрузки заказа: ${error.message}`}</Alert>
      </Container>
    );
  }

  if (mode === 'edit') {
    return <EditOrder orderNumber={order?.orderNumber} onSuccess={() => navigate(`/orders/${order?.id}`)} />;
  }

  return (
    <Container maxWidth="xl" sx={{ mt: 4, px: 2.5 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Button startIcon={<ArrowBack />} onClick={() => navigate('/orders')}>
            Назад
          </Button>
          <Typography variant="h4">
            Заказ #{order?.orderNumber}
          </Typography>
          <Chip label={getStatusLabel(order?.status)} color={getStatusColor(order?.status)} size="medium" />
        </Box>
        <Box display="flex" gap={1}>
          {canEdit && (
            <Button variant="outlined" startIcon={<Edit />} onClick={() => navigate(`/orders/${id}/edit`)}>
              Редактировать
            </Button>
          )}
          <Button variant="outlined" onClick={() => { setNewStatus(order?.status); setStatusDialogOpen(true); }}>
            Изменить статус
          </Button>
          <Button variant="contained" startIcon={<Payment />} onClick={() => setPaymentDialogOpen(true)}>
            Добавить оплату
          </Button>
        </Box>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <OrderInfoCard order={order} onClientInfoClick={(clientId) => setClientInfoDialog({ open: true, clientId })} />
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
                <PositionsTab 
                  materials={order?.materials || []} 
                  items={order?.items || []}
                  orderId={order?.id}
                />
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
          <OrderDetailsCard order={order} calculatedData={calculatedData} />
        </Grid>
      </Grid>

      <StatusChangeDialog
        open={statusDialogOpen}
        onClose={() => setStatusDialogOpen(false)}
        onSave={(status) => updateStatusMutation.mutate(status)}
        currentStatus={newStatus}
      />

      <PaymentDialog
        open={paymentDialogOpen}
        onClose={() => setPaymentDialogOpen(false)}
        onSubmit={(data) => addPaymentMutation.mutate(data)}
      />

      <Dialog open={clientInfoDialog.open} onClose={() => setClientInfoDialog({ open: false, clientId: null })} maxWidth="sm" fullWidth>
        <DialogTitle>Информация о клиенте</DialogTitle>
        <DialogContent>
          <ClientInfo clientId={clientInfoDialog.clientId} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setClientInfoDialog({ open: false, clientId: null })}>
            Закрыть
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={notification.open} autoHideDuration={6000} onClose={() => setNotification({ ...notification, open: false })}>
        <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })}>
          {notification.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

// ==================== Helper Components ====================
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

const PositionsTab = ({ materials = [], items = [], orderId }) => {
  if (!items?.length && !materials?.length) {
    return <Typography>Нет позиций в заказе</Typography>;
  }

  const navigate = useNavigate();

  return (
    <Box>
      {items.map((item) => (
        <Paper key={item.id} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="subtitle1">{item.name}</Typography>
            <Typography variant="h6">{Number(item.cost).toFixed(2)} ₽</Typography>
          </Box>
          <Box display="flex" gap={4} mt={1}>
            <Typography variant="body2" color="text.secondary">
              Цена за ед.: {Number(item.price).toFixed(2)} ₽
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Кол-во: {item.quantity} шт.
            </Typography>
          </Box>
          <Box mt={1}>
            <Button
              variant="outlined"
              size="small"
              startIcon={<Edit />}
              onClick={() => navigate(`/orders/${orderId}/items/${item.id}`)}
            >
              Смета
            </Button>
          </Box>
          {/* Operations list */}
          {item.operations && item.operations.length > 0 && (
            <Box mt={2}>
              <Typography variant="subtitle2" gutterBottom>Операции:</Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Название</TableCell>
                      <TableCell align="right">Цена</TableCell>
                      <TableCell align="right">Кол-во</TableCell>
                      <TableCell align="right">Сумма</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {item.operations.map((op) => (
                      <TableRow key={op.id}>
                        <TableCell>{op.name}</TableCell>
                        <TableCell align="right">{Number(op.pricePerUnit).toFixed(2)} ₽</TableCell>
                        <TableCell align="right">{Number(op.quantity).toFixed(2)}</TableCell>
                        <TableCell align="right">{Number(op.cost).toFixed(2)} ₽</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </Paper>
      ))}
      {materials.map((mat) => (
        <Paper key={mat.id} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="subtitle1">
              {mat.materialName}
            </Typography>
            <Typography variant="h6">{Number(mat.cost).toFixed(2)} ₽</Typography>
          </Box>
          <Box display="flex" gap={4} mt={1}>
            <Typography variant="body2" color="text.secondary">
              Цена за ед.: {Number(mat.pricePerUnit).toFixed(2)} ₽
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Кол-во: {parseFloat(mat.quantity).toFixed(3)} {mat.unit}
            </Typography>
          </Box>
          {/* Operations for material */}
          {mat.operations && mat.operations.length > 0 && (
            <Box mt={2}>
              <Typography variant="subtitle2" gutterBottom>Операции:</Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Название</TableCell>
                      <TableCell align="right">Цена</TableCell>
                      <TableCell align="right">Кол-во</TableCell>
                      <TableCell align="right">Сумма</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {mat.operations.map((op) => (
                      <TableRow key={op.id}>
                        <TableCell>{op.name}</TableCell>
                        <TableCell align="right">{Number(op.pricePerUnit).toFixed(2)} ₽</TableCell>
                        <TableCell align="right">{Number(op.quantity).toFixed(2)}</TableCell>
                        <TableCell align="right">{Number(op.cost).toFixed(2)} ₽</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </Paper>
      ))}
    </Box>
  );
};

const StagesTab = ({ stages }) => {
  if (!stages?.length) {
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
  if (!payments?.length) {
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
  if (!comments?.length) {
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
    details: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
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
        inputProps={{ step: 0.01 }}
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
        InputLabelProps={{ shrink: true }}
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
