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
  Divider,
  Snackbar,
  Grid,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow
} from '@mui/material';
import { ArrowBack, Edit, Payment, Delete, Add, Info } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getStatusColor, getStatusLabel, isM2, isLinearMeter } from '../utils/orderUtils';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

import OrderInfoCard from '../components/OrderInfoCard';
import PositionsTab from '../components/PositionsTab';
import StagesTab from '../components/StagesTab';
import PaymentsTab from '../components/PaymentsTab';
import CommentsTab from '../components/CommentsTab';
import StatusChangeDialog from '../components/StatusChangeDialog';
import PaymentDialog from '../components/PaymentDialog';
import NewClientDialog from '../components/NewClientDialog';
import EditOrder from './EditOrder';

const ManagerOrderDetail = ({ mode = 'view' }) => {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const username = user?.username;
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');
  const isManager = user?.roles?.includes('ROLE_MANAGER');

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

  const { data: order, isLoading, error } = useQuery({
    queryKey: ['order', id],
    queryFn: async () => {
      const response = await api.get(`/api/v1/orders/${id}`);
      return response.data;
    },
    enabled: mode === 'view' && !!id
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

  const [formData, setFormData] = useState({
    clientId: '',
    description: '',
    orderDate: new Date().toISOString().split('T')[0],
    dueDate: '',
    items: []
  });
  const [priceplus, setPriceplus] = useState(0);
  const [calculatedData, setCalculatedData] = useState({ total: 0, cashFromPriceplus: 0 });

  const totalOrderAmount = useMemo(() => {
    if (mode !== 'create') return 0;
    let total = 0;
    formData.items.forEach(item => {
      const material = materialsData.find(m => m.id === parseInt(item.materialId));
      if (material) {
        const toMeters = (v, u) => u === 'мм' ? (parseFloat(v) || 0) / 1000 : (parseFloat(v) || 0);
        const widthM = toMeters(item.qty1value, item.qty1unit);
        const heightM = isM2(material) ? toMeters(item.qty2value, item.qty2unit) : 0;
        const wasteCoeff = material.wasteCoefficient || 1;
        total += widthM * (heightM || 1) * material.price * wasteCoeff;
      }
    });
    return total * (1 + priceplus / 100);
  }, [formData.items, materialsData, priceplus, mode]);

  const canEdit = !!(currentEmployee && order?.manager && (isAdmin || currentEmployee.id === order.manager.id));
  const isManagerNotOwner = isManager && !canEdit && !isAdmin;

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
      const toMeters = (value, unit) => {
        const v = parseFloat(value) || 0;
        return unit === 'мм' ? v / 1000 : v;
      };

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

      const response = await api.post('/api/v1/orders', orderData);
      setCalculatedData({
        total: frontendTotalWithPriceplus,
        cashFromPriceplus: currentEmployee.managerCashPercent ? frontendTotal * currentEmployee.managerCashPercent / 100 : 0
      });
      return response;
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

  useEffect(() => {
    if (mode === 'view' && order) {
      const total = order.items?.reduce((sum, item) => sum + (item.totalPrice || 0), 0) || 0;
      setCalculatedData({ total, cashFromPriceplus: order.cashFromPriceplus || 0 });
    }
  }, [mode, order]);

  useEffect(() => {
    if (mode === 'create' && username && !currentEmployee) {
      api.post('/api/v1/employees/sync')
        .then(() => refetchEmployee())
        .catch(err => console.error('Employee sync failed:', err));
    }
  }, [mode, username, currentEmployee, refetchEmployee]);

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

  if (mode === 'create') {
    return (
      <Container maxWidth="xl" sx={{ mt: 4, px: 2.5 }}>
        <Box display="flex" alignItems="center" gap={2} mb={3}>
          <Button startIcon={<ArrowBack />} onClick={() => navigate('/orders')}>
            Назад
          </Button>
          <Typography variant="h4">Новый заказ</Typography>
        </Box>

        <Paper sx={{ p: 4 }}>
          <form onSubmit={handleCreateSubmit}>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <FormControl fullWidth margin="normal" required>
                  <InputLabel>Клиент</InputLabel>
                  <Select name="clientId" value={formData.clientId} onChange={handleClientChange}>
                    <MenuItem value="create_new">Создать нового клиента</MenuItem>
                    {clientsData?.map((client) => (
                      <MenuItem key={client.id} value={client.id}>
                        {client.name}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} md={6}></Grid>
              <Grid item xs={12} md={6}>
                <TextField fullWidth label="Процент добавки (priceplus)" type="number" value={priceplus} onChange={(e) => setPriceplus(parseFloat(e.target.value) || 0)} margin="normal" inputProps={{ min: -100, max: 100, step: 0.1 }} />
              </Grid>
              <Grid item xs={12} md={6}></Grid>
              <Grid item xs={12} md={6}>
                <TextField fullWidth label="Дата заказа" name="orderDate" type="date" value={formData.orderDate} onChange={(e) => setFormData(prev => ({ ...prev, orderDate: e.target.value }))} required margin="normal" InputLabelProps={{ shrink: true }} />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField fullWidth label="Срок сдачи" name="dueDate" type="date" value={formData.dueDate} onChange={(e) => setFormData(prev => ({ ...prev, dueDate: e.target.value }))} margin="normal" InputLabelProps={{ shrink: true }} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth label="Описание" name="description" multiline rows={3} value={formData.description} onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))} margin="normal" />
              </Grid>

              <Grid item xs={12}>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Typography variant="h6">Позиции заказа</Typography>
                  <Button startIcon={<Add />} onClick={addItem} variant="outlined">Добавить позицию</Button>
                </Box>
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Материал</TableCell>
                        <TableCell width={180}>Размер 1</TableCell>
                        <TableCell width={180}>Размер 2</TableCell>
                        <TableCell width={130}>Срок готовности</TableCell>
                        <TableCell width={50}>Действия</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {formData.items.map((item, index) => {
                        const material = materialsData.find(m => m.id === parseInt(item.materialId));
                        const showSecond = isM2(material);
                        return (
                          <TableRow key={index}>
                            <TableCell>
                              <FormControl fullWidth size="small">
                                <Select value={item.materialId} onChange={(e) => updateItem(index, 'materialId', e.target.value)}>
                                  <MenuItem value="">Выберите материал</MenuItem>
                                  {materialsData.map((mat) => (
                                    <MenuItem key={mat.id} value={mat.id}>
                                      {mat.name} ({mat.unit})
                                    </MenuItem>
                                  ))}
                                </Select>
                              </FormControl>
                            </TableCell>
                            <TableCell>
                              <Box display="flex" gap={0.5} alignItems="center">
                                <TextField size="small" type="number" value={item.qty1value} onChange={(e) => updateItem(index, 'qty1value', e.target.value)} inputProps={{ min: 0, step: 0.001 }} placeholder={isM2(material) ? 'Ширина' : 'Длина'} sx={{ width: 100 }} />
                                <Select size="small" value={item.qty1unit || 'м'} onChange={(e) => updateItem(index, 'qty1unit', e.target.value)} sx={{ width: 70 }}>
                                  <MenuItem value="м">м</MenuItem>
                                  <MenuItem value="мм">мм</MenuItem>
                                </Select>
                              </Box>
                            </TableCell>
                            {showSecond ? (
                              <TableCell>
                                <Box display="flex" gap={0.5} alignItems="center">
                                  <TextField size="small" type="number" value={item.qty2value} onChange={(e) => updateItem(index, 'qty2value', e.target.value)} inputProps={{ min: 0, step: 0.001 }} placeholder="Высота" sx={{ width: 100 }} />
                                  <Select size="small" value={item.qty2unit || 'м'} onChange={(e) => updateItem(index, 'qty2unit', e.target.value)} sx={{ width: 70 }}>
                                    <MenuItem value="м">м</MenuItem>
                                    <MenuItem value="мм">мм</MenuItem>
                                  </Select>
                                </Box>
                              </TableCell>
                            ) : (
                              <TableCell />
                            )}
                            <TableCell>
                              <TextField fullWidth size="small" type="date" value={item.readyDate} onChange={(e) => updateItem(index, 'readyDate', e.target.value)} InputLabelProps={{ shrink: true }} />
                            </TableCell>
                            <TableCell>
                              <IconButton onClick={() => removeItem(index)} color="error" size="small">
                                <Delete />
                              </IconButton>
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
                {formData.items.length === 0 && (
                  <Alert severity="info" sx={{ mt: 2 }}>Добавьте хотя бы одну позицию в заказ</Alert>
                )}
              </Grid>

              <Grid item xs={12}>
                <Box display="flex" justifyContent="space-between" alignItems="center" mt={2}>
                  <Typography variant="h6">Сумма: {totalOrderAmount.toFixed(2)} ₽</Typography>
                  <Box display="flex" gap={2}>
                    <Button onClick={() => navigate('/orders')}>Отмена</Button>
                    <Button type="submit" variant="contained" disabled={!formData.clientId || formData.items.length === 0 || createOrderMutation.isLoading || !currentEmployee}>
                      {createOrderMutation.isLoading ? 'Создание...' : 'Создать заказ'}
                    </Button>
                  </Box>
                </Box>
              </Grid>
            </Grid>
          </form>
        </Paper>

        <NewClientDialog
          open={clientDialogOpen}
          onClose={() => setClientDialogOpen(false)}
          onSubmit={handleClientSubmit}
          isLoading={createClientMutation.isLoading}
        />

        <Snackbar open={notification.open} autoHideDuration={6000} onClose={() => setNotification({ ...notification, open: false })}>
          <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })}>
            {notification.message}
          </Alert>
        </Snackbar>
      </Container>
    );
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
          <Button startIcon={<ArrowBack />} onClick={() => navigate('/manager')}>
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
          {!isManagerNotOwner && (
            <Button variant="outlined" onClick={() => { setNewStatus(order?.status); setStatusDialogOpen(true); }}>
              Изменить статус
            </Button>
          )}
          {!isManagerNotOwner && (
            <Button variant="contained" startIcon={<Payment />} onClick={() => setPaymentDialogOpen(true)}>
              Добавить оплату
            </Button>
          )}
        </Box>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <OrderInfoCard order={order} calculatedData={calculatedData} onClientInfoClick={(clientId) => setClientInfoDialog({ open: true, clientId })} />
          <Paper sx={{ mt: 3 }}>
            <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)}>
              <Tab label="Позиции" />
              <Tab label="Этапы" />
              <Tab label="Оплаты" />
              <Tab label="Комментарии" />
            </Tabs>
            <Divider />
            <Box sx={{ p: 3 }}>
              {activeTab === 0 && <PositionsTab materials={order?.materials || []} items={order?.items || []} orderId={order?.id} calculatedData={calculatedData} />}
              {activeTab === 1 && <StagesTab stages={order?.stages || []} />}
              {activeTab === 2 && <PaymentsTab payments={order?.payments || []} />}
              {activeTab === 3 && <CommentsTab comments={order?.comments || []} />}
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          {order?.cashFromPriceplus != null && (
            <Paper sx={{ p: 3, mt: 2, bgcolor: '#e8f5e9' }}>
              <Typography variant="h6" gutterBottom color="success.dark">Твой заработок</Typography>
              <Typography variant="h4" fontWeight={700} color="success.dark">
                {Number(order.cashFromPriceplus).toFixed(2)} ₽
              </Typography>
            </Paper>
          )}
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

      <Snackbar open={notification.open} autoHideDuration={6000} onClose={() => setNotification({ ...notification, open: false })}>
        <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })}>
          {notification.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default ManagerOrderDetail;