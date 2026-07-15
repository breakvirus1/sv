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
  CircularProgress, LinearProgress,
  Menu,
  Checkbox,
  FormControlLabel,
  FormGroup,
  InputAdornment
} from '@mui/material';
import { Add, Delete, Save, AttachFile } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';
import { computeWorkshopTags } from '../utils/workshopTags';
import { isM2, isLinearMeter } from '../utils/orderUtils';
import { recalculateOrderLocally, applyPriceplus } from '../services/calculationService';
import { useAuth } from '../context/AuthContext';

const CreateOrderForm = ({ windowId, closeWindow }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const username = user?.username;

  // ── State: Form Data ──
  const [formData, setFormData] = useState({
    clientId: '',
    description: '',
    orderDate: new Date().toISOString().split('T')[0],
    dueDate: '',
    items: []
  });
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
  const [uploadProgresses, setUploadProgresses] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [priceplus, setPriceplus] = useState(0);

  // ── Queries ──
  const { data: workshopsData = [] } = useQuery({
    queryKey: ['workshops'],
    queryFn: async () => {
      const response = await api.get('/api/v1/workshops?size=100');
      return response.data.content || [];
    },
  });

  // ── Workshop tags for display ──
  const workshopTags = useMemo(
    () => computeWorkshopTags(workshopsData, formData.items),
    [workshopsData, formData.items]
  );

  // ── State: Dialogs ──
  const [operationsDialog, setOperationsDialog] = useState({ open: false, itemIndex: null, selectedOps: [] });
  const [operationParamsDialog, setOperationParamsDialog] = useState({
    open: false,
    itemIndex: null,
    pendingOps: [],
    params: {}
  });
  const [clientDialogOpen, setClientDialogOpen] = useState(false);
  const [orderAmountDialog, setOrderAmountDialog] = useState({ open: false, sumorder: 0 });

  // ── State: New Client Form ──
  const [newClientForm, setNewClientForm] = useState({
    name: '',
    type: 'PRIVATE',
    contactPerson: '',
    phone: '',
    email: '',
    priceplus: null
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

  const { data: currentEmployee } = useQuery({
    queryKey: ['currentEmployee', username],
    queryFn: async () => {
      if (!username) return null;
      const response = await api.get('/api/v1/employees?size=1&q=' + username);
      const data = response.data.content || [];
      return data.length > 0 ? data[0] : null;
    },
    enabled: !!username,
  });

  const workshopName = currentEmployee?.workshopId
    ? (workshopsData.find(w => w.id === currentEmployee.workshopId)?.name || '#' + currentEmployee.workshopId)
    : null;

  const { data: operationsData = [], error: operationsError } = useQuery({
    queryKey: ['operations'],
    queryFn: async () => {
      const response = await api.get('/api/v1/calculations/operations');
      return response.data || [];
    },
  });

  const { data: groupedOperationsData = {} } = useQuery({
    queryKey: ['grouped-operations'],
    queryFn: async () => {
      const response = await api.get('/api/v1/calculations/operations/grouped');
      const groups = response.data?.groups || [];
      return Object.fromEntries(groups.map(g => [g.id, g]));
    },
  });

  const dialogMaterialId = operationsDialog.open
    ? formData.items[operationsDialog.itemIndex]?.materialId
    : null;

  const { data: materialGroupedOperations = {} } = useQuery({
    queryKey: ['material-grouped-operations', dialogMaterialId],
    queryFn: async () => {
      if (!dialogMaterialId) return {};
      const response = await api.get(`/api/v1/calculations/operations/grouped?materialId=${dialogMaterialId}`);
      const groups = response.data?.groups || [];
      return Object.fromEntries(groups.map(g => [g.id, g]));
    },
    enabled: operationsDialog.open && dialogMaterialId != null,
  });

  const dialogGroupedData = dialogMaterialId != null ? materialGroupedOperations : groupedOperationsData;

  const allGroupedOpsFlat = useMemo(
    () => Object.values(dialogGroupedData).flatMap(g => (g.operations || [])),
    [dialogGroupedData]
  );

  const { data: eyeletsData = [], error: eyeletsError } = useQuery({
    queryKey: ['eyelets'],
    queryFn: async () => {
      const response = await api.get('/api/v1/calculations/eyelets');
      console.log('[DEBUG] eyeletsData received:', response.data);
      return response.data || [];
    },
  });

  // Получение данных выбранного клиента для получения priceplus
  const { data: selectedClient } = useQuery({
    queryKey: ['client', formData.clientId],
    queryFn: async () => {
      if (!formData.clientId) return null;
      const response = await api.get(`/api/v1/clients/${formData.clientId}`);
      return response.data;
    },
    enabled: !!formData.clientId
  });

  // Синхронизируем priceplus с выбранным клиентом
  useEffect(() => {
    if (selectedClient?.priceplus != null) {
      setPriceplus(selectedClient.priceplus);
    } else {
      setPriceplus(0);
    }
  }, [selectedClient?.priceplus]);

  // Мутация для создания клиента
  const createClientMutation = useMutation({
    mutationFn: (client) => api.post('/api/v1/clients', client),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['clients'] });
      setClientDialogOpen(false);
      setNewClientForm({ name: '', type: 'PRIVATE', contactPerson: '', phone: '', email: '', priceplus: null });
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
      items: [...prev.items, { materialId: '', qty1value: '', unit: 'мм', qty2value: '', readyDate: '', operations: [], file: null, fileName: '' }]
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

  const updateItemOperations = (index, operations) => {
    setFormData(prev => ({
      ...prev,
      items: prev.items.map((item, i) => i === index ? { ...item, operations } : item)
    }));
  };

  const handleOpenOperationsDialog = (itemIndex) => {
    const item = formData.items[itemIndex];
    const currentOpIds = item.operations.map(op => op.id);
    setOperationsDialog({
      open: true,
      itemIndex,
      selectedOps: [...currentOpIds]
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
    const opsWithParams = pendingOps.map(op => {
      const baseOp = operationsData.find(o => o.id === op.id);
      const opParams = params[op.id] || {};
      if (opParams.eyeletId !== undefined) {
        opParams.eyeletId = opParams.eyeletId ? parseInt(opParams.eyeletId, 10) : null;
      }
      return { ...baseOp, ...opParams };
    }).filter(Boolean);

    const currentOps = formData.items[itemIndex].operations || [];
    const filteredOps = currentOps.filter(cop => !pendingOps.some(pop => pop.id === cop.id));
    const finalOps = [...filteredOps, ...opsWithParams];

    updateItemOperations(itemIndex, finalOps);
    handleCloseOperationParamsDialog();
  };

  const handleToggleOperation = (opId) => {
    const item = operationsDialog.itemIndex != null ? formData.items[operationsDialog.itemIndex] : null;
    const isHemLocked = item && item.operations.some(iop => String(iop.id) === String(opId) && iop.name && iop.name.toLowerCase().includes('подворот'));
    if (isHemLocked) return;
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

    const specialOps = selectedOpsData.filter(op =>
      op.name.toLowerCase().includes('подворот') || op.name.toLowerCase().includes('люверс')
    );

    const hasSpecialOps = specialOps.length > 0;

    if (hasSpecialOps) {
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
        const ops = selectedOpsData.map(op => {
          const existing = currentOps.find(cop => cop.id === op.id);
          return existing ? { ...op, ...existing } : op;
        });
        const lockedHemOps = currentOps.filter(op => op.name && op.name.toLowerCase().includes('подворот'));
        const mergedOps = [...ops.filter(o => !o.name || !o.name.toLowerCase().includes('подворот')), ...lockedHemOps];
        updateItemOperations(itemIndex, mergedOps);
        handleCloseOperationsDialog();
        return;
      }

      const newPendingOps = [...specialOps];
      const initialParams = {};

      specialOps.forEach(op => {
        const opId = op.id;
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
    const payload = {
      ...newClientForm,
      priceplus: newClientForm.priceplus !== null && newClientForm.priceplus !== '' ? parseFloat(newClientForm.priceplus) : null
    };
    createClientMutation.mutate(payload);
  };

const handleClose = () => {
    if (closeWindow) closeWindow();
    else navigate('/orders');
  };

  const [totalOrderAmount, setTotalOrderAmount] = useState(0);
  const [cashFromPriceplus, setCashFromPriceplus] = useState(0);

  const calculateTotals = async () => {
    if (formData.items.length === 0) return;
    const hasValidItems = formData.items.every(item => item.materialId && parseFloat(item.qty1value) > 0);
    if (!hasValidItems) return;
    try {
      const result = await recalculateOrderLocally(formData.items, priceplus);
      setTotalOrderAmount(result.totalWithPriceplus);
      if (currentEmployee) {
        const totalWithout = result.totalWithoutPriceplus || 0;
        const totalWith = result.totalWithPriceplus || 0;
        const priceplusAmount = totalWith - totalWithout;
        const mgrPercent = currentEmployee.managerCashPercent != null ? Number(currentEmployee.managerCashPercent) : 0;
        console.log('[DEBUG] calculateTotals:', { totalWithout, totalWith, priceplusAmount, mgrPercent, cash: priceplusAmount * mgrPercent / 100 });
        setCashFromPriceplus(priceplusAmount * mgrPercent / 100);
      }
    } catch (err) {
      console.error('Calculation error:', err);
    }
  };

  useEffect(() => {
    calculateTotals();
  }, [formData.items, priceplus, currentEmployee]);

const handleSubmit = async (e) => {
    e.preventDefault();
    if (!currentEmployee) {
      setNotification({ open: true, message: 'Менеджер не определен. Перелогинитесь.', severity: 'error' });
      return;
    }
    setIsSubmitting(true);
    try {
      // Конвертация значения в метры в зависимости от единицы измерения
      const toMeters = (value, unit) => {
        const v = parseFloat(value) || 0;
        return unit === 'мм' ? v / 1000 : v;
      };

      // Формирование массива материалов заказа из формы
      const orderMaterials = formData.items.map(item => {
        const material = materialsData.find(m => m.id === parseInt(item.materialId));
        if (!material) throw new Error('Материал не выбран');

        const widthM = toMeters(item.qty1value, item.unit);
        const heightM = isM2(material) ? toMeters(item.qty2value, item.unit) : null;

        // Поиск операции люверс если есть
        const eyeletOp = item.operations.find(op => op.eyeletId);
        const eyeletId = eyeletOp?.eyeletId || null;
        const eyeletStepCm = eyeletOp?.eyeletStepCm || null;

        // Поиск операции подворот если есть
        const hemOp = item.operations.find(op => op.hemWidthMm != null && op.hemCount != null);
        const podvorotMmHorizontal = hemOp?.hemWidthMm || null;
        const podvorotMmVertical = hemOp?.hemWidthMm || null;
        const podvorotCountPerSide = hemOp?.hemCount || null;

        return {
          materialId: parseInt(item.materialId),
          widthM,
          heightM: isM2(material) ? heightM : null,
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
        priceplus: priceplus,
        items: orderMaterials,
        clientTotalWithPriceplus: Number(totalOrderAmount.toFixed(2))
      };

      const response = await api.post('/api/v1/orders', orderData);
      const createdOrderId = response.data.id;
      const createdOrderItems = response.data.items || [];

      const createdOrderNumber = response.data.orderNumber || '';
      const createdManagerName = response.data.manager?.fullName || currentEmployee?.fullName || '';
      const createdClientName = clientsData.find(c => String(c.id) === String(formData.clientId))?.name || '';

      for (let i = 0; i < formData.items.length; i++) {
        const item = formData.items[i];
        if (item.file) {
          const materialName = materialsData.find(m => String(m.id) === String(item.materialId))?.name || '';
          const operationNames = (item.operations || []).map(op => op.name).filter(Boolean).join('-');
          const operationParamsList = (item.operations || []).map(op => {
            const params = [];
            if (op.hemWidthMm != null) params.push(`podvorot${op.hemWidthMm}mm`);
            if (op.hemCount != null) params.push(`x${op.hemCount}`);
            if (op.eyeletId) {
              const eyelet = eyeletsData.find(e => String(e.id) === String(op.eyeletId));
              const diameter = eyelet?.diameterMm || op.eyeletId;
              params.push(`d${diameter}mm`);
            }
            if (op.eyeletStepCm != null) params.push(`shag${op.eyeletStepCm}sm`);
            if (op.widthMm != null) params.push(`w${op.widthMm}mm`);
            if (op.heightMm != null) params.push(`h${op.heightMm}mm`);
            return params.join('_');
          }).filter(Boolean).join('-');
          const fileFormData = new FormData();
          fileFormData.append('file', item.file);
          fileFormData.append('orderId', createdOrderId);
          const orderItemId = createdOrderItems[i]?.id;
          if (orderItemId) {
            fileFormData.append('orderItemId', orderItemId);
          }
          fileFormData.append('orderNumber', createdOrderNumber);
          fileFormData.append('managerName', createdManagerName);
          fileFormData.append('clientName', createdClientName);
          fileFormData.append('materialName', materialName);
          fileFormData.append('operationNames', operationNames);
          fileFormData.append('operationParams', operationParamsList);
          setUploadProgresses(prev => ({ ...prev, [i]: 0 }));
          try {
            await api.post('/api/files/upload', fileFormData, {
              headers: { 'Content-Type': 'multipart/form-data' },
              onUploadProgress: (event) => {
                const percent = Math.round((event.loaded * 100) / event.total);
                setUploadProgresses(prev => ({ ...prev, [i]: percent }));
              },
            });
            setUploadProgresses(prev => ({ ...prev, [i]: null }));
          } catch (fileErr) {
            setUploadProgresses(prev => ({ ...prev, [i]: null }));
            console.error('File upload error for item', i, fileErr);
          }
        }
      }

      setNotification({ open: true, message: 'Заказ успешно создан', severity: 'success' });

      // Инвалидация запросов
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
        {workshopName ? workshopName + ' — ' : ''}Создание нового заказа
      </Typography>
      <Divider sx={{ mb: 2 }} />

      <Box component="form" onSubmit={handleSubmit} sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'auto' }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-end' }}>
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
          <TextField
            size="small"
            label="Наценка %"
            type="number"
            value={priceplus}
            onChange={(e) => setPriceplus(parseFloat(e.target.value) || 0)}
            inputProps={{ min: -100, max: 100, step: 0.1 }}
            sx={{ width: 120 }}
          />
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
                     <TableCell width={140}>Файл</TableCell>
                     <TableCell width={50}>Действия</TableCell>
                   </TableRow>
                 </TableHead>
                <TableBody>
                  {formData.items.map((item, index) => {
                    const material = materialsData.find(m => m.id === parseInt(item.materialId));
                    const showSecond = isM2(material);
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
                              placeholder={isM2(material) ? 'Ширина' : 'Длина'}
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
                           <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                             <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                               <Button
                                 component="label"
                                 variant="outlined"
                                 size="small"
                                 startIcon={<AttachFile />}
                                 sx={{ fontSize: '0.75rem', px: 1 }}
                               >
                                 {item.fileName || 'Файл'}
                                 <input
                                   type="file"
                                   hidden
                                   onChange={(e) => {
                                     if (e.target.files && e.target.files[0]) {
                                       const file = e.target.files[0];
                                       setFormData(prev => ({
                                         ...prev,
                                         items: prev.items.map((it, i) =>
                                           i === index ? { ...it, file, fileName: file.name } : it
                                         )
                                       }));
                                     }
                                   }}
                                 />
                               </Button>
                               {item.fileName && (
                                 <IconButton
                                   size="small"
                                   color="error"
                                   onClick={() => setFormData(prev => ({
                                     ...prev,
                                     items: prev.items.map((it, i) =>
                                       i === index ? { ...it, file: null, fileName: '' } : it
                                     )
                                   }))}
                                   sx={{ ml: 0.5 }}
                                 >
                                   <Delete fontSize="small" />
                                 </IconButton>
                               )}
                             </Box>
                             {uploadProgresses[index] != null && uploadProgresses[index] >= 0 && (
                               <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, width: '100%' }}>
                                 <LinearProgress variant="determinate" value={uploadProgresses[index] || 0} sx={{ height: 6, flex: 1 }} />
                                 <Typography variant="caption" sx={{ fontSize: '0.65rem', minWidth: 30, textAlign: 'right' }}>
                                   {uploadProgresses[index]}%
                                 </Typography>
                               </Box>
                             )}
                           </Box>
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
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Box
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                px: 2,
                py: 1,
                fontSize: '1.25rem',
                fontWeight: 600,
                display: 'flex',
                alignItems: 'center',
                gap: 1,
              }}
            >
              Сумма: {totalOrderAmount.toFixed(2)} ₽{workshopTags ? '   ' + workshopTags : ''}
            </Box>
            {currentEmployee && cashFromPriceplus > 0 && (
              <Box
                sx={{
                  border: '1px solid',
                  borderColor: 'success.main',
                  borderRadius: 1,
                  px: 2,
                  py: 1,
                  fontSize: '1rem',
                  fontWeight: 600,
                  color: 'success.dark',
                  bgcolor: '#e8f5e9',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1,
                }}
              >
                Твой заработок с заказа: {cashFromPriceplus.toFixed(2)} ₽
              </Box>
            )}
          </Box>
        </Box>

        <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
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

      <Dialog open={orderAmountDialog.open} onClose={() => setOrderAmountDialog({ open: false, sumorder: totalOrderAmount })} maxWidth="xs" fullWidth>
        <DialogTitle>Итоговая сумма заказа</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            margin="dense"
            label="Сумма с учетом наценки (sumorder)"
            type="number"
            value={orderAmountDialog.sumorder}
            onChange={(e) => {
              const newSumorder = parseFloat(e.target.value) || 0;
              setOrderAmountDialog(prev => ({ ...prev, sumorder: newSumorder }));
              setTotalOrderAmount(newSumorder);
            }}
            InputProps={{
              endAdornment: <InputAdornment position="end">₽</InputAdornment>
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOrderAmountDialog({ open: false, sumorder: totalOrderAmount })}>
            Отмена
          </Button>
          <Button onClick={() => setOrderAmountDialog({ open: false })} variant="contained">
            ОК
          </Button>
        </DialogActions>
      </Dialog>

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
          <TextField
            fullWidth
            margin="dense"
            label="Процент добавки (priceplus)"
            type="number"
            value={newClientForm.priceplus || ''}
            onChange={handleNewClientChange('priceplus')}
            inputProps={{ min: 0, step: 0.01 }}
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
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mt: 1, alignItems: 'stretch' }}>
            {(() => {
              const currentItem = operationsDialog.itemIndex != null ? formData.items[operationsDialog.itemIndex] : null;
              const lockedHem = currentItem?.operations?.find(op => op.name && op.name.toLowerCase().includes('подворот'));
              if (lockedHem) {
                return (
                  <FormControlLabel
                    key={lockedHem.id}
                    control={
                      <Checkbox
                        checked
                        disabled
                      />
                    }
                    label={lockedHem.name + ' (обязательная)'}
                  />
                );
              }
              return null;
            })()}
            {Object.values(dialogGroupedData)
              .map(group => ({
                ...group,
                operations: (group.operations || []).filter(op => op.name)
              }))
              .filter(group => group.operations.length > 0)
              .map(group => (
                <Box key={group.id} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.5, width: '100%' }}>
                  <Typography variant="subtitle2" color="primary" gutterBottom>
                    {group.name}
                  </Typography>
                  <FormGroup>
                    {group.operations.map(op => (
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
                  </FormGroup>
                </Box>
              ))}
            {(() => {
              const groupedOpIds = new Set(
                Object.values(dialogGroupedData).flatMap(g => (g.operations || []).map(op => op.id))
              );
              const ungroupedOps = allGroupedOpsFlat.filter(
                op => !groupedOpIds.has(op.id) && op.name && !op.name.toLowerCase().includes('подворот')
              );
              if (ungroupedOps.length === 0) return null;
              return (
                <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.5, width: '100%' }}>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Без группы
                  </Typography>
                  <FormGroup>
                    {ungroupedOps.map(op => (
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
                  </FormGroup>
                </Box>
              );
            })()}
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
