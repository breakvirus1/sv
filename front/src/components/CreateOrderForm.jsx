import { useState, useEffect, useMemo } from 'react';
import {
  Box,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Paper,
  Typography,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Alert,
  Snackbar,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress
} from '@mui/material';
import { Add, Delete, Save } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const CreateOrderForm = ({ windowId, closeWindow }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const username = user?.username;

  // Form state (без номера заказа и менеджера)
  const [formData, setFormData] = useState({
    clientId: '',
    description: '',
    orderDate: new Date().toISOString().split('T')[0],
    dueDate: '',
    items: []
  });
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Клиентский диалог создания нового клиента
  const [clientDialogOpen, setClientDialogOpen] = useState(false);
  const [newClientForm, setNewClientForm] = useState({
    name: '',
    type: 'PRIVATE',
    contactPerson: '',
    phone: '',
    email: ''
  });

  // Загрузка данных
  const { data: clientsData = [], error: clientsError } = useQuery({
    queryKey: ['clients'],
    queryFn: async () => {
      const response = await api.get('/api/v1/clients?size=100');
      return response.data.content || [];
    },
  });

  const { data: materialsData = [], error: materialsError } = useQuery({
    queryKey: ['materials'],
    queryFn: async () => {
      const response = await api.get('/api/v1/materials?size=100');
      return response.data.content || [];
    },
  });

  // Получение текущего менеджера по username из Keycloak
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

  // Синхронизируем менеджера из Keycloak, если не найден
  useEffect(() => {
    if (username && !currentEmployee) {
      api.post('/api/v1/employees/sync')
        .then(() => refetchEmployee())
        .catch(err => console.error('Employee sync failed:', err));
    }
  }, [username, currentEmployee, refetchEmployee]);

  // Мутация для создания клиента
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

  // Функции для позиций заказа (материалов)
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

  const handleChange = (field) => (e) => {
    setFormData(prev => ({ ...prev, [field]: e.target.value }));
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

  const handleClose = () => {
    if (closeWindow) closeWindow();
    else navigate('/orders');
  };

  // Расчет общей суммы заказа
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

  const handleSubmit = async (e) => {
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

      // Invalidate queries
      await queryClient.invalidateQueries({ queryKey: ['orders'] });
      await queryClient.invalidateQueries({ queryKey: ['recentOrders'] });

      setTimeout(() => {
        if (closeWindow) closeWindow();
      }, 1500);
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

  if (clientsError || materialsError) {
    return (
      <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
        <Alert severity="error" sx={{ mb: 2 }}>
          Не удалось загрузить данные. Пожалуйста, проверьте подключение к серверу.
        </Alert>
        <Button onClick={handleClose} variant="outlined">
          Закрыть
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      <Typography variant="h6" gutterBottom sx={{ color: '#0055ea', fontWeight: 600 }}>
        Создание нового заказа
      </Typography>
      <Divider sx={{ mb: 2 }} />

      <Box component="form" onSubmit={handleSubmit} sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'auto' }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <FormControl fullWidth size="small">
            <InputLabel>Клиент</InputLabel>
            <Select
              name="clientId"
              value={formData.clientId}
              onChange={handleClientChange}
              required
            >
              <MenuItem value="create_new">Создать нового клиента</MenuItem>
              {clientsData?.map((client) => (
                <MenuItem key={client.id} value={client.id}>
                  {client.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField
              fullWidth
              label="Дата заказа"
              name="orderDate"
              type="date"
              value={formData.orderDate}
              onChange={handleChange('orderDate')}
              required
              size="small"
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              fullWidth
              label="Срок сдачи"
              name="dueDate"
              type="date"
              value={formData.dueDate}
              onChange={handleChange('dueDate')}
              size="small"
              InputLabelProps={{ shrink: true }}
            />
          </Box>

          <TextField
            fullWidth
            label="Описание"
            name="description"
            value={formData.description}
            onChange={handleChange('description')}
            multiline
            rows={2}
            size="small"
          />
        </Box>

        <Divider sx={{ my: 1 }}>Позиции заказа</Divider>

        <Box sx={{ flex: 1, overflow: 'auto', mb: 2 }}>
          {formData.items.length === 0 ? (
            <Alert severity="info" sx={{ mt: 1 }}>
              Добавьте хотя бы одну позицию в заказ
            </Alert>
          ) : (
            <TableContainer component={Paper} variant="outlined" sx={{ maxHeight: 300 }}>
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
                          <TableCell></TableCell>
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
                            <Delete fontSize="small" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Box>

        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 'auto' }}>
          <Typography variant="h6">
            Сумма: {totalOrderAmount.toFixed(2)} ₽
          </Typography>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button onClick={handleClose} variant="outlined">
              Отмена
            </Button>
            <Button
              onClick={addItem}
              variant="outlined"
              startIcon={<Add />}
              disabled={formData.items.length >= 20}
            >
              Добавить позицию
            </Button>
            <Button
              type="submit"
              variant="contained"
              startIcon={<Save />}
              disabled={!formData.clientId || formData.items.length === 0 || isSubmitting || !currentEmployee}
            >
              {isSubmitting ? 'Создание...' : 'Создать заказ'}
            </Button>
          </Box>
        </Box>
      </Box>

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
            {createClientMutation.isLoading ? <CircularProgress size={24} /> : 'Сохранить'}
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
    </Box>
  );
};

export default CreateOrderForm;
