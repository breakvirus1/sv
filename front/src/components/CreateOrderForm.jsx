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
  CircularProgress,
  Menu,
  Checkbox,
  FormControlLabel
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
  // Operations dialog state
  const [operationsDialog, setOperationsDialog] = useState({ open: false, itemIndex: null, selectedOps: [] });
  // Unified operation parameters dialog state
  const [operationParamsDialog, setOperationParamsDialog] = useState({
    open: false,
    itemIndex: null,
    pendingOps: [],
    params: {}
  });

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
      const response = await api.get('/api/v1/calculations/materials');
      return response.data;
    },
  });

  const { data: operationsData = [], error: operationsError } = useQuery({
    queryKey: ['operations'],
    queryFn: async () => {
      const response = await api.get('/api/v1/calculations/operations');
      return response.data || [];
    },
  });

  const { data: eyeletsData = [], error: eyeletsError } = useQuery({
    queryKey: ['eyelets'],
    queryFn: async () => {
      const response = await api.get('/api/v1/calculations/eyelets');
      return response.data || [];
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

  const handleUnitChange = (index, newUnit) => {
    setFormData(prev => {
      const item = prev.items[index];
      const convertUnit = (value, fromUnit, toUnit) => {
        if (fromUnit === toUnit) return value;
        const num = parseFloat(value) || 0;
        if (fromUnit === 'м' && toUnit === 'мм') return (num * 1000).toString();
        if (fromUnit === 'мм' && toUnit === 'м') return (num / 1000).toString();
        return value;
      };
      const oldUnit = item.unit || 'мм';
      return {
        ...prev,
        items: prev.items.map((it, i) =>
          i === index ? {
            ...it,
            unit: newUnit,
            qty1value: convertUnit(it.qty1value, oldUnit, newUnit),
            qty2value: convertUnit(it.qty2value, oldUnit, newUnit)
          } : it
        )
      };
    });
  };

  // Функции для позиций заказа (материалов)
  const addItem = () => {
    setFormData(prev => ({
      ...prev,
      items: [...prev.items, { materialId: '', qty1value: '', unit: 'мм', qty2value: '', readyDate: '', operations: [] }]
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

  const updateItemOperations = (index, operations) => {
    setFormData(prev => ({
      ...prev,
      items: prev.items.map((item, i) => i === index ? { ...item, operations } : item)
    }));
  };

  const handleOpenOperationsDialog = (itemIndex) => {
    const item = formData.items[itemIndex];
    setOperationsDialog({
      open: true,
      itemIndex,
      selectedOps: item.operations.map(op => op.id)
    });
  };

  const handleCloseOperationsDialog = () => {
    setOperationsDialog({ open: false, itemIndex: null, selectedOps: [] });
  };

  const handleCloseOperationParamsDialog = () => {
    setOperationParamsDialog(prev => ({
      ...prev,
      open: false,
      itemIndex: null,
      pendingOps: [],
      params: {}
    }));
  };

  const handleSaveOperationParams = () => {
    const { itemIndex, pendingOps, params } = operationParamsDialog;
    // Combine pendingOps with their configured params, converting types as needed
    const opsWithParams = pendingOps.map(op => {
      const baseOp = operationsData.find(o => o.id === op.id);
      const opParams = params[op.id] || {};
      // Convert eyeletId to number if present
      if (opParams.eyeletId !== undefined) {
        opParams.eyeletId = opParams.eyeletId ? parseInt(opParams.eyeletId, 10) : null;
      }
      return { ...baseOp, ...opParams };
    });

    // Get existing operations and update/add the special ones
    const currentOps = formData.items[itemIndex].operations || [];
    // Remove any existing special ops with same ids to avoid duplicates
    const filteredOps = currentOps.filter(cop => !pendingOps.some(pop => pop.id === cop.id));
    // Combine: existing non-special ops + new special ops with params
    const finalOps = [...filteredOps, ...opsWithParams];

    updateItemOperations(itemIndex, finalOps);
    handleCloseOperationParamsDialog();
  };

  const handleToggleOperation = (opId) => {
    setOperationsDialog(prev => ({
      ...prev,
      selectedOps: prev.selectedOps.includes(opId)
        ? prev.selectedOps.filter(id => id !== opId)
        : [...prev.selectedOps, opId]
    }));
  };

  const handleSaveOperations = () => {
    const { itemIndex, selectedOps } = operationsDialog;
    const selectedOpsData = operationsData.filter(op => selectedOps.includes(op.id));
    const currentOps = formData.items[itemIndex].operations || [];

    // Check for special operations (hem or eyelet) - case-insensitive
    const specialOps = selectedOpsData.filter(op =>
      op.name.toLowerCase().includes('подворот') || op.name.toLowerCase().includes('люверс')
    );

    if (specialOps.length > 0) {
      // Check if any special ops already have params in currentOps
      const allAlreadyConfigured = specialOps.every(sop => {
        const existing = currentOps.find(cop => cop.id === sop.id);
        if (sop.name.toLowerCase().includes('люверс')) {
          return existing && existing.eyeletId;
        }
        if (sop.name.toLowerCase().includes('подворот')) {
          return existing && existing.hemWidthMm;
        }
        return false;
      });

      if (allAlreadyConfigured) {
        // Preserve existing params for all special ops
        const ops = selectedOpsData.map(op => {
          const existing = currentOps.find(cop => cop.id === op.id);
          return existing ? { ...op, ...existing } : op;
        });
        updateItemOperations(itemIndex, ops);
        handleCloseOperationsDialog();
        return;
      }

      // Initialize params for all special ops, preserving existing if available
      const initialParams = {};
      const newPendingOps = [];

      specialOps.forEach(op => {
        const opId = op.id;
        newPendingOps.push(op);
        const existing = currentOps.find(cop => cop.id === opId);
        if (op.name.toLowerCase().includes('подворот')) {
          if (existing && existing.hemWidthMm) {
            initialParams[opId] = { 
              hemWidthMm: existing.hemWidthMm, 
              hemCount: existing.hemCount || 2,
              widthMm: existing.widthMm,
              heightMm: existing.heightMm
            };
          } else {
            const defaultWidth = op.hemWidthMm != null ? op.hemWidthMm : 20;
            const defaultCount = op.hemCount != null ? op.hemCount : 2;
            initialParams[opId] = { hemWidthMm: defaultWidth, hemCount: defaultCount, widthMm: null, heightMm: null };
          }
        } else if (op.name.toLowerCase().includes('люверс')) {
          if (existing && existing.eyeletId) {
            initialParams[opId] = { 
              eyeletId: existing.eyeletId, 
              eyeletStepCm: existing.eyeletStepCm || 40,
              widthMm: existing.widthMm,
              heightMm: existing.heightMm
            };
          } else {
            initialParams[opId] = { eyeletId: '', eyeletStepCm: 40, widthMm: null, heightMm: null };
          }
        }
      });

      setOperationParamsDialog(prev => ({
        ...prev,
        open: true,
        itemIndex,
        pendingOps: newPendingOps,
        params: initialParams
      }));

      handleCloseOperationsDialog();
    } else {
      // No special operations, save directly
      updateItemOperations(itemIndex, selectedOpsData);
      handleCloseOperationsDialog();
    }
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

  // State for calculated total
  const [totalOrderAmount, setTotalOrderAmount] = useState(0);

  // Recalculate total when items change
  useEffect(() => {
    const calculateTotal = async () => {
      let total = 0;
      for (const item of formData.items) {
        if (!item.materialId || !item.qty1value || !item.qty2value) {
          console.warn('Item missing required fields:', item);
          continue;
        }
        const material = materialsData.find(m => m.id === parseInt(item.materialId));
        if (!material) continue;

        const toMeters = (value, unit) => {
          const v = parseFloat(value) || 0;
          return unit === 'мм' ? v / 1000 : v;
        };

        const widthM = toMeters(item.qty1value, item.unit);
        const heightM = toMeters(item.qty2value, item.unit);

        if (isNaN(widthM) || isNaN(heightM) || widthM <= 0 || heightM <= 0) {
          console.warn('Invalid dimensions:', item.qty1value, item.unit, item.qty2value);
          continue;
        }

        // Prepare calculation request
        const requestData = {
          materialId: material.id,
          materialType: material.name.toLowerCase().includes('баннер') ? 'BANNER' : 'PLENKA',
          widthM,
          heightM,
          operationIds: item.operations.map(op => op.id),
        };

        // Add eyelet parameters if present
        const eyeletOp = item.operations.find(op => op.eyeletId);
        if (eyeletOp) {
          requestData.eyeletId = eyeletOp.eyeletId;
          requestData.eyeletStepCm = eyeletOp.eyeletStepCm;
        }

        // Add hem parameters if present (take first hem operation)
        const hemOp = item.operations.find(op => op.hemWidthMm && op.hemCount);
        if (hemOp) {
          requestData.podvorotMmHorizontal = hemOp.hemWidthMm;
          requestData.podvorotMmVertical = hemOp.hemWidthMm;
          requestData.podvorotCountPerSide = hemOp.hemCount;
        }

        try {
          const response = await api.post('/api/v1/calculations', requestData);
          total += response.data.totalPrice;
        } catch (error) {
          console.error('Error calculating item cost:', error);
          // Fallback to simple calculation
          let effectiveQty = 0;
          if (material.unit === 'м2') {
            effectiveQty = widthM * heightM;
          } else if (material.unit === 'м.п.') {
            effectiveQty = widthM;
          } else {
            effectiveQty = widthM;
          }
          total += material.price * effectiveQty * (material.wasteCoefficient || 1);
        }
      }
      setTotalOrderAmount(total);
    };

    if (formData.items.length > 0 && materialsData.length > 0) {
      calculateTotal();
    } else {
      setTotalOrderAmount(0);
    }
  }, [formData.items, materialsData]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!currentEmployee) {
      setNotification({ open: true, message: 'Менеджер не определен. Перелогинитесь.', severity: 'error' });
      return;
    }
    setIsSubmitting(true);
    try {
      const toMeters = (value, unit) => {
        const v = parseFloat(value) || 0;
        return unit === 'мм' ? v / 1000 : v;
      };

      const orderMaterials = formData.items.map(item => {
        const material = materialsData.find(m => m.id === parseInt(item.materialId));
        if (!material) throw new Error('Материал не выбран');
        const widthM = toMeters(item.qty1value, item.unit);
        const heightM = toMeters(item.qty2value, item.unit) || 0;

        // Find eyelet operation if any
        const eyeletOp = item.operations.find(op => op.eyeletId);
        const eyeletId = eyeletOp?.eyeletId || null;
        const eyeletStepCm = eyeletOp?.eyeletStepCm || null;

        // Find hem operation if any
        const hemOp = item.operations.find(op => op.hemWidthMm && op.hemCount);
        const podvorotMmHorizontal = hemOp?.hemWidthMm || null;
        const podvorotMmVertical = hemOp?.hemWidthMm || null;
        const podvorotCountPerSide = hemOp?.hemCount || null;

         return {
           materialId: parseInt(item.materialId),
           widthM,
           heightM,
           operations: item.operations.map(op => ({
             operationId: op.id,
             widthMm: op.widthMm != null ? op.widthMm : null,
             heightMm: op.heightMm != null ? op.heightMm : null
           })),
           readyDate: item.readyDate || null,
          ...(eyeletId !== null && { eyeletId }),
          ...(eyeletStepCm !== null && { eyeletStepCm }),
          ...(podvorotMmHorizontal !== null && { podvorotMmHorizontal }),
          ...(podvorotMmVertical !== null && { podvorotMmVertical }),
          ...(podvorotCountPerSide !== null && { podvorotCountPerSide })
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
        else navigate('/orders');
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

  if (clientsError || materialsError || operationsError || eyeletsError) {
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
                      <TableCell width={180}>Размер 1</TableCell>
                      <TableCell width={180}>Размер 2</TableCell>
                     <TableCell width={120}>Операции</TableCell>
                     <TableCell width={130}>Срок готовности</TableCell>
                     <TableCell width={50}>Действия</TableCell>
                   </TableRow>
                 </TableHead>
                <TableBody>
                  {formData.items.map((item, index) => {
                    const material = materialsData.find(m => m.id === parseInt(item.materialId));
                    const showSecond = material && material.unit === 'м2';
                    const isBanner = material && material.name.toLowerCase().includes('баннер');
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
                          <Box display="flex" gap={0.5} alignItems="center">
                            <TextField
                              size="small"
                              type="number"
                              value={item.qty1value}
                              onChange={(e) => updateItem(index, 'qty1value', e.target.value)}
                              inputProps={{ min: 0 }}
                              placeholder={material?.unit === 'м2' ? 'Ширина' : 'Длина'}
                              sx={{ width: 100 }}
                            />
                            <Select
                              size="small"
                              value={item.unit || 'мм'}
                              onChange={(e) => handleUnitChange(index, e.target.value)}
                              sx={{ width: 70 }}
                            >
                              <MenuItem value="мм">мм</MenuItem>
                              <MenuItem value="м">м</MenuItem>
                            </Select>
                          </Box>
                        </TableCell>
                        {showSecond ? (
                          <TableCell>
                            <TextField
                              size="small"
                              type="number"
                              value={item.qty2value}
                              onChange={(e) => updateItem(index, 'qty2value', e.target.value)}
                              inputProps={{ min: 0 }}
                              placeholder="Высота"
                              sx={{ width: 100 }}
                            />
                          </TableCell>
                        ) : (
                          <TableCell></TableCell>
                        )}
                        <TableCell>
                          <Button
                            variant="outlined"
                            size="small"
                            onClick={() => handleOpenOperationsDialog(index)}
                            disabled={!material}
                          >
                            {item.operations.length > 0 ? `${item.operations.length} оп.` : 'Выбрать'}
                          </Button>
                          {item.operations.length > 0 && (
                            <Box sx={{ mt: 0.5, fontSize: '0.75rem', color: 'text.secondary' }}>
                              {item.operations.map((op, opIdx) => (
                                <span key={op.id}>
                                  {opIdx > 0 && ', '}
                                  {op.name}
                                  {(op.widthMm != null || op.heightMm != null) && (
                                    <span>
                                      {' '}({op.widthMm || 0}×{op.heightMm || 0} мм)
                                    </span>
                                  )}
                                </span>
                              ))}
                            </Box>
                          )}
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

      {/* Operations dialog */}
      <Dialog open={operationsDialog.open} onClose={handleCloseOperationsDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Выбрать операции</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mt: 1, alignItems: 'flex-start' }}>
            {operationsData.map(op => (
              <FormControlLabel
                key={op.id}
                control={
                  <Checkbox
                    checked={operationsDialog.selectedOps.includes(op.id)}
                    onChange={() => handleToggleOperation(op.id)}
                  />
                }
                label={op.name}
              />
            ))}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseOperationsDialog}>Отмена</Button>
          <Button onClick={handleSaveOperations} variant="contained">Сохранить</Button>
        </DialogActions>
      </Dialog>

      {/* Unified operation parameters dialog */}
      <Dialog open={operationParamsDialog.open} onClose={handleCloseOperationParamsDialog} maxWidth="xs" fullWidth>
        <DialogTitle>Параметры операций</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            {operationParamsDialog.pendingOps.map(op => {
              const opName = op.name.toLowerCase();
              if (opName.includes('подворот')) {
                const params = operationParamsDialog.params[op.id] || { hemWidthMm: 20, hemCount: 2 };
                return (
                  <Box key={op.id} sx={{ border: '1px solid #e0e0e0', borderRadius: 1, p: 2 }}>
                    <Typography variant="subtitle2" gutterBottom color="primary">
                      {op.name}
                    </Typography>
                    <TextField
                      fullWidth
                      margin="dense"
                      label="Ширина подворота (мм)"
                      type="number"
                      value={params.hemWidthMm}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev,
                        params: {
                          ...prev.params,
                          [op.id]: {
                            ...prev.params[op.id],
                            hemWidthMm: parseInt(e.target.value) || 0
                          }
                        }
                      }))}
                      inputProps={{ min: 1 }}
                      required
                    />
                    <TextField
                      fullWidth
                      margin="dense"
                      label="Количество подворотов на сторону"
                      type="number"
                      value={params.hemCount}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev,
                        params: {
                          ...prev.params,
                          [op.id]: {
                            ...prev.params[op.id],
                            hemCount: parseInt(e.target.value) || 0
                          }
                        }
                      }))}
                      inputProps={{ min: 1 }}
                      required
                    />
                    <TextField
                      fullWidth margin="dense" label="Ширина (мм)" type="number"
                      value={params.widthMm || ''}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev, params: {
                          ...prev.params, [op.id]: {
                            ...prev.params[op.id], widthMm: e.target.value ? parseFloat(e.target.value) : null
                          }
                        }
                      }))}
                      inputProps={{ min: 0 }}
                    />
                    <TextField
                      fullWidth margin="dense" label="Высота (мм)" type="number"
                      value={params.heightMm || ''}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev, params: {
                          ...prev.params, [op.id]: {
                            ...prev.params[op.id], heightMm: e.target.value ? parseFloat(e.target.value) : null
                          }
                        }
                      }))}
                      inputProps={{ min: 0 }}
                    />
                  </Box>
                );
              } else if (opName.includes('люверс')) {
                const params = operationParamsDialog.params[op.id] || { eyeletId: '', eyeletStepCm: 40 };
                return (
                  <Box key={op.id} sx={{ border: '1px solid #e0e0e0', borderRadius: 1, p: 2 }}>
                    <Typography variant="subtitle2" gutterBottom color="primary">
                      {op.name}
                    </Typography>
                    <TextField
                      select
                      fullWidth
                      margin="dense"
                      label="Размер люверса"
                      value={params.eyeletId}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev,
                        params: {
                          ...prev.params,
                          [op.id]: {
                            ...prev.params[op.id],
                            eyeletId: e.target.value
                          }
                        }
                      }))}
                      required
                    >
                      <MenuItem value="">Выберите размер</MenuItem>
                      {eyeletsData.map(eyelet => (
                        <MenuItem key={eyelet.id} value={eyelet.id}>
                          {eyelet.name} — {eyelet.pricePerPiece} ₽/шт
                        </MenuItem>
                      ))}
                    </TextField>
                    <TextField
                      fullWidth
                      margin="dense"
                      label="Шаг установки (см)"
                      type="number"
                      value={params.eyeletStepCm}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev,
                        params: {
                          ...prev.params,
                          [op.id]: {
                            ...prev.params[op.id],
                            eyeletStepCm: parseInt(e.target.value) || 0
                          }
                        }
                      }))}
                      inputProps={{ min: 10 }}
                      required
                    />
                    <TextField
                      fullWidth margin="dense" label="Ширина (мм)" type="number"
                      value={params.widthMm || ''}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev, params: {
                          ...prev.params, [op.id]: {
                            ...prev.params[op.id], widthMm: e.target.value ? parseFloat(e.target.value) : null
                          }
                        }
                      }))}
                      inputProps={{ min: 0 }}
                    />
                    <TextField
                      fullWidth margin="dense" label="Высота (мм)" type="number"
                      value={params.heightMm || ''}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev, params: {
                          ...prev.params, [op.id]: {
                            ...prev.params[op.id], heightMm: e.target.value ? parseFloat(e.target.value) : null
                          }
                        }
                      }))}
                      inputProps={{ min: 0 }}
                    />
                  </Box>
                );
              }
              const params = operationParamsDialog.params[op.id] || {};
              return (
                <Box key={op.id} sx={{ border: '1px solid #e0e0e0', borderRadius: 1, p: 2 }}>
                  <Typography variant="subtitle2" gutterBottom color="primary">{op.name}</Typography>
                  <TextField
                    fullWidth margin="dense" label="Ширина (мм)" type="number"
                    value={params.widthMm || ''}
                    onChange={(e) => setOperationParamsDialog(prev => ({
                      ...prev, params: {
                        ...prev.params, [op.id]: {
                          ...prev.params[op.id], widthMm: e.target.value ? parseFloat(e.target.value) : null
                        }
                      }
                    }))}
                    inputProps={{ min: 0 }}
                  />
                  <TextField
                    fullWidth margin="dense" label="Высота (мм)" type="number"
                    value={params.heightMm || ''}
                    onChange={(e) => setOperationParamsDialog(prev => ({
                      ...prev, params: {
                        ...prev.params, [op.id]: {
                          ...prev.params[op.id], heightMm: e.target.value ? parseFloat(e.target.value) : null
                        }
                      }
                    }))}
                    inputProps={{ min: 0 }}
                  />
                </Box>
              );
            })}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseOperationParamsDialog}>Отмена</Button>
          <Button
            onClick={handleSaveOperationParams}
            variant="contained"
            disabled={
              !operationParamsDialog.pendingOps.every(op => {
                const opName = op.name.toLowerCase();
                if (opName.includes('подворот')) {
                  const p = operationParamsDialog.params[op.id];
                  return p && p.hemWidthMm > 0 && p.hemCount > 0;
                } else if (opName.includes('люверс')) {
                  const p = operationParamsDialog.params[op.id];
                  return p && p.eyeletId;
                }
                return true;
              })
            }
          >
            Сохранить
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CreateOrderForm;
