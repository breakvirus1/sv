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
  Grid,
  TextField,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Select,
  FormControl,
  InputLabel,
  Snackbar
} from '@mui/material';
import { ArrowBack, Add, Payment, Delete } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const OrderDetail = ({ mode = 'view' }) => {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const username = user?.username;

  // ==================== Common State ====================
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });

  // ==================== Create Mode State ====================
  const [formData, setFormData] = useState({
    clientId: '',
    description: '',
    orderDate: new Date().toISOString().split('T')[0],
    dueDate: '',
    items: []
  });
  const [clientDialogOpen, setClientDialogOpen] = useState(false);
  const [newClientForm, setNewClientForm] = useState({
    name: '',
    type: 'PRIVATE',
    contactPerson: '',
    phone: '',
    email: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  // ==================== View Mode State ====================
  const [activeTab, setActiveTab] = useState(0);
  const [paymentDialogOpen, setPaymentDialogOpen] = useState(false);
  const [statusDialogOpen, setStatusDialogOpen] = useState(false);
  const [newStatus, setNewStatus] = useState('');

  // ==================== Queries ====================
  // Clients (fetch when create mode)
  const { data: clientsData = [] } = useQuery({
    queryKey: ['clients'],
    queryFn: async () => {
      const response = await api.get('/api/v1/clients?size=100');
      return response.data.content || [];
    },
    enabled: mode === 'create'
  });

  // Materials (create mode)
  const { data: materialsData = [] } = useQuery({
    queryKey: ['materials'],
    queryFn: async () => {
      const response = await api.get('/api/v1/materials?size=100');
      return response.data.content || [];
    },
    enabled: mode === 'create'
  });

  // Current employee (manager) from Keycloak
  const { data: currentEmployee, refetch: refetchEmployee } = useQuery({
    queryKey: ['currentEmployee', username],
    queryFn: async () => {
      if (!username) return null;
      const response = await api.get(`/api/v1/employees?size=1&q=${username}`);
      const data = response.data.content || [];
      return data.length > 0 ? data[0] : null;
    },
    enabled: mode === 'create' && !!username
  });

  // Order data (view mode)
  const { data: order, isLoading, error } = useQuery({
    queryKey: ['order', id],
    queryFn: async () => {
      const response = await api.get(`/api/v1/orders/${id}`);
      return response.data;
    },
    enabled: mode !== 'create'
  });

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

  // Used in view mode (historical). Not used in create mode but defined.
  const createOrderMutation = useMutation({
    mutationFn: (data) => api.post('/api/v1/orders', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
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
  // Ensure current employee is synced from Keycloak when in create mode
  useEffect(() => {
    if (mode === 'create' && username && !currentEmployee) {
      api.post('/api/v1/employees/sync')
        .then(() => refetchEmployee())
        .catch(err => console.error('Employee sync failed:', err));
    }
  }, [mode, username, currentEmployee, refetchEmployee]);

  // ==================== Create Mode Handlers ====================
  const addItem = () => {
    setFormData(prev => ({
      ...prev,
      items: [...prev.items, { materialId: '', qty1: '', qty2: '', readyDate: '' }]
    }));
  };

  const removeItem = (index) => {
    setFormData(prev => ({
      ...prev,
      items: prev.items.filter((_, i) => i !== index)
    }));
  };

  const updateItem = (index, field, value) => {
    setFormData(prev => ({
      ...prev,
      items: prev.items.map((item, i) => i === index ? { ...item, [field]: value } : item)
    }));
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

  // Total calculation
  const totalOrderAmount = useMemo(() => {
    return formData.items.reduce((sum, item) => {
      const material = materialsData.find(m => m.id === parseInt(item.materialId));
      if (!material) return sum;
      const q1 = parseFloat(item.qty1) || 0;
      const q2 = parseFloat(item.qty2) || 0;
      let effectiveQty = 0;
      if (material.unit === 'м2') {
        effectiveQty = (q1 / 1000) * (q2 / 1000);
      } else if (material.unit === 'м.п.') {
        effectiveQty = q1 / 1000;
      } else {
        effectiveQty = q1;
      }
      const cost = material.price * effectiveQty * (material.wasteCoefficient || 1);
      return sum + cost;
    }, 0);
  }, [formData.items, materialsData]);

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    if (!currentEmployee) {
      setNotification({ open: true, message: 'Менеджер не определен. Перелогинитесь.', severity: 'error' });
      return;
    }
    setIsSubmitting(true);
    try {
      const orderMaterials = formData.items.map(item => {
        const material = materialsData.find(m => m.id === parseInt(item.materialId));
        if (!material) throw new Error('Материал не выбран');
        let qty;
        const q1 = parseFloat(item.qty1) || 0;
        const q2 = parseFloat(item.qty2) || 0;
        if (material.unit === 'м2') {
          qty = (q1 / 1000) * (q2 / 1000);
        } else if (material.unit === 'м.п.') {
          qty = q1 / 1000;
        } else {
          qty = q1;
        }
        return {
          materialId: parseInt(item.materialId),
          quantity: qty,
          readyDate: item.readyDate || null
        };
      });

      const orderData = {
        clientId: parseInt(formData.clientId),
        description: formData.description,
        orderDate: formData.orderDate,
        dueDate: formData.dueDate || null,
        managerId: currentEmployee.id,
        items: orderMaterials
      };

      await api.post('/api/v1/orders', orderData);
      setNotification({ open: true, message: 'Заказ успешно создан', severity: 'success' });

      await queryClient.invalidateQueries({ queryKey: ['orders'] });
      await queryClient.invalidateQueries({ queryKey: ['recentOrders'] });

      setTimeout(() => navigate('/orders'), 1500);
    } catch (err) {
      setNotification({
        open: true,
        message: `Ошибка: ${err.response?.data?.message || err.message}`,
        severity: 'error'
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // ==================== View Mode Handlers ====================
  // They are directly used in JSX (no separate functions except existing)
  // Status change handler is inline in dialog? Actually in original view they used updateStatusMutation directly; we can keep as is.

  // ==================== Conditional Render ====================
  if (mode === 'create') {
    return (
      <Container maxWidth="xl" sx={{ mt: 4 }}>
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
                  <Select
                    name="clientId"
                    value={formData.clientId}
                    onChange={handleClientChange}
                  >
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
                <TextField
                  fullWidth
                  label="Дата заказа"
                  name="orderDate"
                  type="date"
                  value={formData.orderDate}
                  onChange={(e) => setFormData(prev => ({ ...prev, orderDate: e.target.value }))}
                  required
                  margin="normal"
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Срок сдачи"
                  name="dueDate"
                  type="date"
                  value={formData.dueDate}
                  onChange={(e) => setFormData(prev => ({ ...prev, dueDate: e.target.value }))}
                  margin="normal"
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Описание"
                  name="description"
                  multiline
                  rows={3}
                  value={formData.description}
                  onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                  margin="normal"
                />
              </Grid>

              {/* Позиции заказа */}
              <Grid item xs={12}>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Typography variant="h6">Позиции заказа</Typography>
                  <Button startIcon={<Add />} onClick={addItem} variant="outlined">
                    Добавить позицию
                  </Button>
                </Box>

                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Материал</TableCell>
                        <TableCell width={100}>Размер 1 (мм)</TableCell>
                        <TableCell width={100}>Размер 2 (мм)</TableCell>
                        <TableCell width={130}>Срок готовности</TableCell>
                        <TableCell width={50}>Действия</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {formData.items.map((item, index) => {
                        const material = materialsData.find(m => m.id === parseInt(item.materialId));
                        const showSecond = material && material.unit === 'м2';
                        return (
                          <TableRow key={index}>
                            <TableCell>
                              <FormControl fullWidth size="small">
                                <Select
                                  value={item.materialId}
                                  onChange={(e) => updateItem(index, 'materialId', e.target.value)}
                                >
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
                              <TextField
                                fullWidth
                                size="small"
                                type="number"
                                value={item.qty1}
                                onChange={(e) => updateItem(index, 'qty1', e.target.value)}
                                inputProps={{ min: 0 }}
                                placeholder={material?.unit === 'м2' ? 'Ширина' : 'Длина'}
                              />
                            </TableCell>
                            {showSecond ? (
                              <TableCell>
                                <TextField
                                  fullWidth
                                  size="small"
                                  type="number"
                                  value={item.qty2}
                                  onChange={(e) => updateItem(index, 'qty2', e.target.value)}
                                  inputProps={{ min: 0 }}
                                  placeholder="Высота"
                                />
                              </TableCell>
                            ) : (
                              <TableCell />
                            )}
                            <TableCell>
                              <TextField
                                fullWidth
                                size="small"
                                type="date"
                                value={item.readyDate}
                                onChange={(e) => updateItem(index, 'readyDate', e.target.value)}
                                InputLabelProps={{ shrink: true }}
                              />
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
                  <Alert severity="info" sx={{ mt: 2 }}>
                    Добавьте хотя бы одну позицию в заказ
                  </Alert>
                )}
              </Grid>

              <Grid item xs={12}>
                <Box display="flex" justifyContent="space-between" alignItems="center" mt={2}>
                  <Typography variant="h6">
                    Сумма: {totalOrderAmount.toFixed(2)} ₽
                  </Typography>
                  <Box display="flex" gap={2}>
                    <Button onClick={() => navigate('/orders')}>Отмена</Button>
                    <Button
                      type="submit"
                      variant="contained"
                      disabled={!formData.clientId || formData.items.length === 0 || isSubmitting || !currentEmployee}
                    >
                      {isSubmitting ? 'Создание...' : 'Создать заказ'}
                    </Button>
                  </Box>
                </Box>
              </Grid>
            </Grid>
          </form>
        </Paper>

        {/* Диалог создания нового клиента */}
        <Dialog open={clientDialogOpen} onClose={() => setClientDialogOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Новый клиент</DialogTitle>
          <DialogContent>
            <TextField
              autoFocus
              fullWidth
              margin="dense"
              label="Название"
              value={newClientForm.name}
              onChange={handleNewClientChange('name')}
            />
            <FormControl fullWidth margin="dense">
              <InputLabel>Тип</InputLabel>
              <Select
                value={newClientForm.type}
                label="Тип"
                onChange={handleNewClientChange('type')}
              >
                <MenuItem value="PRIVATE">Частник</MenuItem>
                <MenuItem value="COMPANY">Компания</MenuItem>
              </Select>
            </FormControl>
            <TextField
              fullWidth
              margin="dense"
              label="Контактное лицо"
              value={newClientForm.contactPerson}
              onChange={handleNewClientChange('contactPerson')}
            />
            <TextField
              fullWidth
              margin="dense"
              label="Телефон"
              value={newClientForm.phone}
              onChange={handleNewClientChange('phone')}
            />
            <TextField
              fullWidth
              margin="dense"
              label="Email"
              value={newClientForm.email}
              onChange={handleNewClientChange('email')}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setClientDialogOpen(false)}>Отмена</Button>
            <Button onClick={handleClientSubmit} variant="contained" disabled={createClientMutation.isLoading}>
              {createClientMutation.isLoading ? 'Сохранение...' : 'Сохранить'}
            </Button>
          </DialogActions>
        </Dialog>

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
  }

  // ==================== View Mode JSX ====================
  const statusOptions = ['WAITING', 'LAUNCHED', 'IN_PROGRESS', 'READY', 'ACCEPTED', 'CLOSED'];

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

          {/* Tabs */}
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

      {/* Dialog for Status Change */}
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

      {/* Dialog for Payment */}
      <Dialog open={paymentDialogOpen} onClose={() => setPaymentDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Добавить оплату</DialogTitle>
        <DialogContent>
          <PaymentForm onSubmit={(data) => addPaymentMutation.mutate(data)} />
        </DialogContent>
      </Dialog>

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

const PositionsTab = ({ materials = [], items = [] }) => {
  const displayList = (materials && materials.length > 0) ? materials : items;

  if (!displayList?.length) {
    return <Typography>Нет позиций в заказе</Typography>;
  }

  const isMaterial = displayList[0]?.material !== undefined;

  return (
    <Box>
      {displayList.map((entry) => {
        const name = isMaterial ? entry.material?.name : entry.name;
        const price = isMaterial ? entry.material?.price : entry.price;
        const quantity = entry.quantity;
        const cost = entry.cost;
        const readyDate = entry.readyDate;
        const unit = isMaterial ? entry.material?.unit : '';

        const qtyDisplay = isMaterial 
          ? parseFloat(quantity).toFixed(3) 
          : quantity;

        return (
          <Paper key={entry.id} sx={{ p: 2, mb: 2 }} variant="outlined">
            <Box display="flex" justifyContent="space-between" alignItems="center">
              <Typography variant="subtitle1">{name}</Typography>
              <Typography variant="h6">{cost?.toFixed(2)} ₽</Typography>
            </Box>
            <Box display="flex" gap={4} mt={1}>
              <Typography variant="body2" color="text.secondary">
                Цена за ед.: {price?.toFixed(2)} ₽
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Кол-во: {qtyDisplay} {unit}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Срок: {readyDate || '—'}
              </Typography>
            </Box>
          </Paper>
        );
      })}
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
