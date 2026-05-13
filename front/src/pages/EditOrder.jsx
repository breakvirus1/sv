import { useState, useEffect, useMemo } from 'react';
import {
  Box,
  Container,
  Typography,
  Paper,
  Button,
  Grid,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  CircularProgress,
  Snackbar,
  Alert
} from '@mui/material';
import { ArrowBack, Add, Delete, Save } from '@mui/icons-material';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';

const EditOrder = ({ order, onSuccess }) => {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [formData, setFormData] = useState({
    description: '',
    orderDate: '',
    dueDate: '',
    managerId: null,
    items: []
  });
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Fetch employees for manager selection
  const { data: employeesData = [] } = useQuery({
    queryKey: ['employees'],
    queryFn: async () => {
      const response = await api.get('/api/v1/employees?size=100');
      return response.data.content || [];
    }
  });

  // Fetch materials for items
  const { data: materialsData = [] } = useQuery({
    queryKey: ['materials'],
    queryFn: async () => {
      const response = await api.get('/api/v1/materials?size=100');
      return response.data.content || [];
    }
  });

  // Initialize form data when order is loaded
  useEffect(() => {
    if (order) {
      const formatDate = (dateStr) => {
        if (!dateStr) return '';
        return dateStr.split('T')[0];
      };
      const items = order.items?.map(item => ({
        id: item.id,
        materialId: item.material?.id ? String(item.material.id) : '',
        qty1: item.widthM ? (item.widthM * 1000).toString() : '',
        qty2: item.heightM ? (item.heightM * 1000).toString() : '',
        readyDate: item.readyDate || '',
        operations: item.operations?.map(op => ({ id: op.id, operationId: op.id })) || []
      })) || [];
      setFormData({
        description: order.description || '',
        orderDate: formatDate(order.orderDate),
        dueDate: formatDate(order.dueDate),
        managerId: order.manager?.id ? String(order.manager.id) : '',
        items
      });
    }
  }, [order]);

  // Calculate total
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

  // CRUD Handlers for items
  const addItem = () => {
    setFormData(prev => ({
      ...prev,
      items: [...prev.items, { materialId: '', qty1: '', qty2: '', readyDate: '', operations: [] }]
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

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const orderMaterials = formData.items.map(item => {
        const material = materialsData.find(m => m.id === parseInt(item.materialId));
        if (!material) throw new Error('Материал не выбран');
        const widthM = parseFloat(item.qty1) / 1000;
        const heightM = parseFloat(item.qty2) / 1000 || 0;

        // TODO: handle operations if needed in edit mode
        return {
          ...(item.id ? { id: item.id } : {}),
          materialId: parseInt(item.materialId),
          widthM,
          heightM,
          readyDate: item.readyDate || null
        };
      });

      const orderData = {
        description: formData.description,
        orderDate: formData.orderDate,
        dueDate: formData.dueDate || null,
        managerId: formData.managerId ? parseInt(formData.managerId) : null,
        items: orderMaterials
      };

      await api.put(`/api/v1/orders/${id}`, orderData);
      setNotification({ open: true, message: 'Заказ успешно обновлен', severity: 'success' });

      await queryClient.invalidateQueries({ queryKey: ['order', id] });
      await queryClient.invalidateQueries({ queryKey: ['orders'] });

      if (onSuccess) onSuccess();
      setTimeout(() => navigate(`/orders/${id}`), 1500);
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

  return (
    <Container maxWidth="xl" sx={{ mt: 4, px: 2.5 }}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate(`/orders/${id}`)}>
          Назад
        </Button>
        <Typography variant="h4">
          Редактировать заказ #{order?.orderNumber}
        </Typography>
      </Box>

      <Paper sx={{ p: 4 }}>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Клиент"
                value={order?.client?.name || ''}
                margin="normal"
                disabled
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Дата заказа"
                name="orderDate"
                type="date"
                value={formData.orderDate}
                onChange={handleChange}
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
                onChange={handleChange}
                margin="normal"
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth margin="normal">
                <InputLabel>Менеджер</InputLabel>
                <Select
                  name="managerId"
                  value={formData.managerId || ''}
                  onChange={handleChange}
                >
                  <MenuItem value="">Не назначен</MenuItem>
                  {employeesData?.map((emp) => (
                    <MenuItem key={emp.id} value={String(emp.id)}>
                      {emp.fullName}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Описание"
                name="description"
                multiline
                rows={3}
                value={formData.description}
                onChange={handleChange}
                margin="normal"
              />
            </Grid>

            {/* Positions CRUD Section */}
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
                        <TableRow key={item.id || index}>
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
                  <Button onClick={() => navigate(`/orders/${id}`)}>Отмена</Button>
                  <Button
                    type="submit"
                    variant="contained"
                    disabled={isSubmitting}
                    startIcon={<Save />}
                  >
                    {isSubmitting ? 'Сохранение...' : 'Сохранить'}
                  </Button>
                </Box>
              </Box>
            </Grid>
          </Grid>
        </form>
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

export default EditOrder;
