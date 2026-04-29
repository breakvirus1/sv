import { useState } from 'react';
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
  Chip,
  Alert,
  Snackbar
} from '@mui/material';
import { Add, Delete, Save } from '@mui/icons-material';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';

const CreateOrderForm = ({ windowId, closeWindow }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [formData, setFormData] = useState({
    orderNumber: '',
    clientId: '',
    description: '',
    orderDate: new Date().toISOString().split('T')[0],
    dueDate: '',
    managerId: '',
    items: []
  });
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Fetch clients for dropdown
  const { data: clientsData = [], error: clientsError } = useQuery({
    queryKey: ['clients'],
    queryFn: async () => {
      const response = await api.get('/api/v1/clients?size=100');
      return response.data.content || [];
    },
  });

  // Fetch employees (managers) for dropdown
  const { data: employeesData = [], error: employeesError } = useQuery({
    queryKey: ['employees'],
    queryFn: async () => {
      const response = await api.get('/api/v1/employees?size=100');
      return response.data.content || [];
    },
  });

  const addItem = () => {
    setFormData(prev => ({
      ...prev,
      items: [...prev.items, { name: '', price: '', quantity: '1', readyDate: '' }]
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

  const handleClose = () => {
    if (closeWindow) closeWindow();
    else navigate('/orders');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const orderData = {
        orderNumber: formData.orderNumber,
        clientId: parseInt(formData.clientId),
        description: formData.description,
        orderDate: formData.orderDate,
        dueDate: formData.dueDate || null,
        managerId: parseInt(formData.managerId),
        items: formData.items.map(item => ({
          name: item.name,
          price: parseFloat(item.price),
          quantity: parseInt(item.quantity),
          readyDate: item.readyDate || null
        }))
      };

      await api.post('/api/v1/orders', orderData);
      setNotification({ open: true, message: 'Заказ успешно создан', severity: 'success' });

      // Invalidate queries to refresh data
      await queryClient.invalidateQueries({ queryKey: ['orders'] });
      await queryClient.invalidateQueries({ queryKey: ['recentOrders'] });

      // Close window after short delay
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

  // Show error state if data failed to load
  if (clientsError || employeesError) {
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
          <TextField
            fullWidth
            label="Номер заказа"
            name="orderNumber"
            value={formData.orderNumber}
            onChange={handleChange('orderNumber')}
            required
            size="small"
          />

          <Box sx={{ display: 'flex', gap: 2 }}>
            <FormControl fullWidth size="small">
              <InputLabel>Клиент</InputLabel>
              <Select
                name="clientId"
                value={formData.clientId}
                onChange={handleChange('clientId')}
                required
              >
                {clientsData?.map((client) => (
                  <MenuItem key={client.id} value={client.id}>
                    {client.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl fullWidth size="small">
              <InputLabel>Менеджер</InputLabel>
              <Select
                name="managerId"
                value={formData.managerId}
                onChange={handleChange('managerId')}
                required
              >
                {employeesData?.map((emp) => (
                  <MenuItem key={emp.id} value={emp.id}>
                    {emp.fullName}
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
                    <TableCell>Наименование</TableCell>
                    <TableCell width={120}>Цена, ₽</TableCell>
                    <TableCell width={80}>Кол-во</TableCell>
                    <TableCell width={130}>Срок готовности</TableCell>
                    <TableCell width={50}>Действия</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {formData.items.map((item, index) => (
                    <TableRow key={index}>
                      <TableCell>
                        <TextField
                          fullWidth
                          size="small"
                          value={item.name}
                          onChange={(e) => updateItem(index, 'name', e.target.value)}
                          placeholder="Наименование изделия"
                          required
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          fullWidth
                          size="small"
                          type="number"
                          value={item.price}
                          onChange={(e) => updateItem(index, 'price', e.target.value)}
                          required
                          inputProps={{ step: 0.01, min: 0 }}
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          fullWidth
                          size="small"
                          type="number"
                          value={item.quantity}
                          onChange={(e) => updateItem(index, 'quantity', e.target.value)}
                          required
                          inputProps={{ min: 1 }}
                        />
                      </TableCell>
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
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Box>

        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2, mt: 'auto' }}>
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
            disabled={!formData.orderNumber || !formData.clientId || !formData.managerId || formData.items.length === 0 || isSubmitting}
          >
            {isSubmitting ? 'Создание...' : 'Создать заказ'}
          </Button>
        </Box>
      </Box>

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
