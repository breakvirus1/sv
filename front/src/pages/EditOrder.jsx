import { useState, useEffect, useMemo, useRef } from 'react';
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
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Checkbox,
  FormControlLabel
} from '@mui/material';
import { ArrowBack, Add, Delete, Save } from '@mui/icons-material';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';

const EditOrder = ({ order, orderNumber, onSuccess, mode = 'edit' }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const identifier = orderNumber || order?.id;
  const isOrderNumber = !!orderNumber && !order;

  const { data: fetchedOrder, isLoading: isLoadingOrder } = useQuery({
    queryKey: ['order', identifier],
    queryFn: async () => {
      if (order) return order;
      const endpoint = isOrderNumber && /^\d{14}$/.test(identifier)
        ? `/api/v1/orders/number/${identifier}`
        : `/api/v1/orders/${identifier}`;
      const response = await api.get(endpoint);
      return response.data;
    },
    enabled: mode !== 'create' && !!identifier && (!order || isOrderNumber)
  });

  const orderData = order || fetchedOrder;

  const [formData, setFormData] = useState({
    description: '',
    orderDate: '',
    dueDate: '',
    managerId: null,
    items: []
  });
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Track pending dimension-fetch requests per row index so late responses are ignored
  const pendingDimRef = useRef(new Map()); // index -> materialId

  const [operationsDialog, setOperationsDialog] = useState({ open: false, itemIndex: null, selectedOps: [] });
  const [operationParamsDialog, setOperationParamsDialog] = useState({
    open: false,
    itemIndex: null,
    pendingOps: [],
    params: {}
  });

  const { data: employeesData = [], isLoading: isLoadingEmployees } = useQuery({
    queryKey: ['employees'],
    queryFn: async () => {
      const response = await api.get('/api/v1/employees?size=100');
      return response.data.content || [];
    }
  });

  const { data: materialsData = [], isLoading: isLoadingMaterials } = useQuery({
    queryKey: ['materials'],
    queryFn: async () => {
      const response = await api.get('/api/v1/materials?size=100');
      return response.data.content || [];
    }
  });

  const { data: operationsData = [] } = useQuery({
    queryKey: ['operations'],
    queryFn: async () => {
      const response = await api.get('/api/v1/calculations/operations');
      return response.data || [];
    }
  });

  // Load order total from backend
  const { data: backendTotal } = useQuery({
    queryKey: ['order-total', orderData?.id],
    queryFn: async () => {
      if (!orderData?.id) return 0;
      const r = await api.get(`/api/v1/orders/${orderData.id}/total-open`);
      return r.data || 0;
    },
    enabled: !!orderData?.id
  });

  // Initialize form data when order is loaded
  useEffect(() => {
    if (orderData) {
      const formatDate = (dateStr) => {
        if (!dateStr) return '';
        return dateStr.split('T')[0];
      };
      const items = (orderData.materials ?? []).map((mat) => ({
        id: mat.id,
        materialId: String(mat.material?.id || ''),
        qty1: mat.widthMm != null ? (mat.widthMm / 1000).toString() : '',
        qty2: mat.heightMm != null ? (mat.heightMm / 1000).toString() : '',
        readyDate: mat.readyDate || '',
        operations: (mat.operations ?? []).map((op) => ({ id: op.operationId, operationId: op.operationId }))
      }));
      setFormData({
        description: orderData.description || '',
        orderDate: formatDate(orderData.orderDate),
        dueDate: formatDate(orderData.dueDate),
        managerId: orderData.manager?.id ? String(orderData.manager.id) : (orderData.managerId ? String(orderData.managerId) : ''),
        items
      });
    }
  }, [orderData, mode]);

  // Calculate total using backend value when available, otherwise fallback to local
  const totalOrderAmount = useMemo(() => {
    if (backendTotal != null) return backendTotal;
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
  }, [formData.items, materialsData, backendTotal]);

  // Fetch default dimensions for an existing item from backend (on mount and when orderData changes)
  useEffect(() => {
    if (!orderData || !materialsData.length) return;
    formData.items.forEach((item, index) => {
      if (!item.id || item.qty1 && item.qty2) return; // already have dimensions or it's a new item
      const mat = materialsData.find(m => m.id === parseInt(item.materialId));
      const needsFetch = mat && (mat.defaultWidthMm != null || mat.defaultHeightMm != null);
      if (!needsFetch) return;

      pendingDimRef.current.set(index, item.materialId);

      api.get(`/api/v1/orders/${orderData.id}/positions/${item.id}/default`)
        .then(r => {
          const d = r.data;
          const matResp = d.material;
          const wMm = d.widthMm != null ? parseFloat(d.widthMm) : 0;
          const hMm = d.heightMm != null ? parseFloat(d.heightMm) : 0;
          setFormData(prev => {
            const curr = prev.items[index];
            if (!curr || curr.materialId !== item.materialId || (curr.qty1 && curr.qty2)) {
              return prev; // skipped or already updated
            }
            return {
              ...prev,
              items: prev.items.map((it, i) =>
                i === index ? {
                  ...it,
                  qty1: wMm != null ? (wMm / 1000).toString() : '',
                  qty2: hMm != null ? (hMm / 1000).toString() : ''
                } : it
              )
            };
          });
        })
        .catch(() => {})
        .finally(() => pendingDimRef.current.delete(index));
    });
  }, [orderData, materialsData]);

  // ── CRUD ──

  const addItem = () => {
    setFormData(prev => {
      const newIndex = prev.items.length;
      // Check if this new row introduces a duplicate
      const anyMaterial = prev.items.find(item => item.materialId);
      if (anyMaterial && materialsData.length > 0) {
        const candidateId = String(materialsData[0].id);
        const existingCandidate = prev.items.find(item => item.materialId === candidateId);
        if (existingCandidate) {
          setNotification({
            open: true,
            message: `Материал "${materialsData.find(m => m.id === parseInt(candidateId))?.name}" уже выбран`,
            severity: 'warning'
          });
          return prev;
        }
        // Auto-select candidate and fetch defaults from service
        const nextItems = [...prev.items, { materialId: candidateId, qty1: '', qty2: '', readyDate: '', operations: [] }];
        pendingDimRef.current.set(newIndex, candidateId);
        api.get(`/api/v1/materials/${candidateId}`)
          .then(r => {
            const m = r.data;
            const wMm = m.defaultWidthMm != null ? parseFloat(m.defaultWidthMm) : null;
            const hMm = m.defaultHeightMm != null ? parseFloat(m.defaultHeightMm) : null;
            setFormData(pd => {
              const curr = pd.items[newIndex];
              if (!curr || curr.materialId !== candidateId) return pd;
              return {
                ...pd,
                items: pd.items.map((it, i) =>
                  i === newIndex ? { ...it, qty1: wMm != null ? (wMm / 1000).toString() : '', qty2: hMm != null ? (hMm / 1000).toString() : '' } : it
                )
              };
            });
          })
          .catch(() => {})
          .finally(() => pendingDimRef.current.delete(newIndex));
        return { ...prev, items: nextItems };
      }
      return { ...prev, items: [...prev.items, { materialId: '', qty1: '', qty2: '', readyDate: '', operations: [] }] };
    });
  };

  const removeItem = (index) => {
    setFormData(prev => ({
      ...prev,
      items: prev.items.filter((_, i) => i !== index)
    }));
  };

  const updateItem = (index, field, value) => {
    setFormData(prev => {
      // When changing material: block duplicates, fetch defaults from backend
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

        // Immediately clear qty/readyDate/operations when material changes
        const next = prev.items.map((item, i) =>
          i === index ? { ...item, materialId: value, qty1: '', qty2: '', readyDate: '', operations: [] } : item
        );

        // Fetch defaults from backend; apply only if row hasn't changed yet
        pendingDimRef.current.set(index, value);

        api.get(`/api/v1/orders/${orderData?.id}/positions/${prev.items[index]?.id || ''}/default`)
          .then(r => {
            const d = r.data;
            const wMm = d.widthMm != null ? parseFloat(d.widthMm) : null;
            const hMm = d.heightMm != null ? parseFloat(d.heightMm) : null;
            // Get defaults from material response if position record had no stored sizes
            const matResp = d.material || materialsData.find(m => m.id === parseInt(value));
            const dwMm = wMm != null ? wMm : (matResp?.defaultWidthMm != null ? parseFloat(matResp.defaultWidthMm) : null);
            const dhMm = hMm != null ? hMm : (matResp?.defaultHeightMm != null ? parseFloat(matResp.defaultHeightMm) : null);

            setFormData(pd => {
              const currMatId = pd.items[index]?.materialId;
              if (currMatId !== value) return pd; // user changed material again
              if (dwMm == null && dhMm == null) return pd;      // no defaults available
              return {
                ...pd,
                items: pd.items.map((it, i) =>
                  i === index ? { ...it, qty1: dwMm != null ? (dwMm / 1000).toString() : '', qty2: dhMm != null ? (dhMm / 1000).toString() : '' } : it
                )
              };
            });
          })
          .catch(() => {})
          .finally(() => pendingDimRef.current.delete(index));

        return { ...prev, items: next };
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

  // ── Operations dialogs ──

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
    const opsWithParams = pendingOps.map(op => {
      const baseOp = operationsData.find(o => o.id === op.id);
      const opParams = params[op.id] || {};
      if (opParams.eyeletId !== undefined) {
        opParams.eyeletId = opParams.eyeletId ? parseInt(opParams.eyeletId, 10) : null;
      }
      return { ...baseOp, ...opParams };
    });

    const currentOps = formData.items[itemIndex].operations || [];
    const filteredOps = currentOps.filter(cop => !pendingOps.some(pop => pop.id === cop.id));
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

    const specialOps = selectedOpsData.filter(op =>
      op.name.toLowerCase().includes('подворот') || op.name.toLowerCase().includes('люверс')
    );

    if (specialOps.length > 0) {
      const allAlreadyConfigured = specialOps.every(sop => {
        const existing = currentOps.find(cop => cop.id === sop.id);
        if (sop.name.toLowerCase().includes('люверс')) return existing && existing.eyeletId;
        if (sop.name.toLowerCase().includes('подворот')) return existing && existing.hemWidthMm;
        return false;
      });

      if (allAlreadyConfigured) {
        const ops = selectedOpsData.map(op => {
          const existing = currentOps.find(cop => cop.id === op.id);
          return existing ? { ...op, ...existing } : op;
        });
        updateItemOperations(itemIndex, ops);
        handleCloseOperationsDialog();
        return;
      }

      const initialParams = {};
      const newPendingOps = [];

      specialOps.forEach(op => {
        const opId = op.id;
        newPendingOps.push(op);
        const existing = currentOps.find(cop => cop.id === opId);
        if (op.name.toLowerCase().includes('подворот')) {
          if (existing && existing.hemWidthMm) {
            initialParams[opId] = { hemWidthMm: existing.hemWidthMm, hemCount: existing.hemCount || 2 };
          } else {
            const defaultWidth = op.hemWidthMm != null ? op.hemWidthMm : 20;
            const defaultCount = op.hemCount != null ? op.hemCount : 2;
            initialParams[opId] = { hemWidthMm: defaultWidth, hemCount: defaultCount };
          }
        } else if (op.name.toLowerCase().includes('люверс')) {
          if (existing && existing.eyeletId) {
            initialParams[opId] = { eyeletId: existing.eyeletId, eyeletStepCm: existing.eyeletStepCm || 40 };
          } else {
            initialParams[opId] = { eyeletId: '', eyeletStepCm: 40 };
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

        return {
          ...(item.id ? { id: item.id } : {}),
          materialId: parseInt(item.materialId),
          widthM,
          heightM,
          readyDate: item.readyDate || null
        };
      });

      const orderDataPayload = {
        description: formData.description,
        orderDate: formData.orderDate,
        dueDate: formData.dueDate || null,
        managerId: formData.managerId ? parseInt(formData.managerId) : null,
        items: orderMaterials
      };

      await api.put(`/api/v1/orders/${orderData.id}`, orderDataPayload);
      setNotification({ open: true, message: 'Заказ успешно обновлен', severity: 'success' });

      await queryClient.invalidateQueries({ queryKey: ['order', orderData.id] });
      await queryClient.invalidateQueries({ queryKey: ['orders'] });
      await queryClient.invalidateQueries({ queryKey: ['order-total', orderData.id] });

      if (onSuccess) onSuccess();
      setTimeout(() => navigate(`/orders/${orderData.id}`), 1500);
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

  const isLoading = isLoadingMaterials || isLoadingEmployees || isLoadingOrder;

  if (!isLoading && !orderData) {
    return (
      <Container maxWidth="xl" sx={{ mt: 4, px: 2.5 }}>
        <Alert severity="error">Заказ не найден</Alert>
      </Container>
    );
  }

  if (isLoading || !orderData) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ mt: 4, px: 2.5 }}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate(`/orders/${orderData.id}`)}>
          Назад
        </Button>
        <Typography variant="h4">Редактировать заказ #{orderData.orderNumber}</Typography>
      </Box>

      <Paper sx={{ p: 4 }}>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
             <Grid item xs={12} md={6}>
               <TextField
                 fullWidth
                 label="Клиент"
                 value={orderData?.client?.name || orderData?.clientName || ''}
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
                      <TableCell width={120}>Операции</TableCell>
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
                            <Button
                              variant="outlined"
                              size="small"
                              onClick={() => handleOpenOperationsDialog(index)}
                              disabled={!material}
                            >
                              {item.operations.length > 0 ? `${item.operations.length} оп.` : 'Выбрать'}
                            </Button>
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
                  <Button onClick={() => navigate(`/orders/${orderData.id}`)}>Отмена</Button>
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

      {/* Operations selection dialog */}
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

      {/* Operation parameters dialog (podvorot, eyelets) */}
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
                    <Typography variant="subtitle2" gutterBottom color="primary">{op.name}</Typography>
                    <TextField
                      fullWidth margin="dense" label="Ширина подворота (мм)" type="number"
                      value={params.hemWidthMm}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev, params: {
                          ...prev.params, [op.id]: {
                            ...prev.params[op.id], hemWidthMm: parseInt(e.target.value) || 0
                          }
                        }
                      }))}
                      inputProps={{ min: 1 }} required
                    />
                    <TextField
                      fullWidth margin="dense" label="Количество подворотов на сторону" type="number"
                      value={params.hemCount}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev, params: {
                          ...prev.params, [op.id]: {
                            ...prev.params[op.id], hemCount: parseInt(e.target.value) || 0
                          }
                        }
                      }))}
                      inputProps={{ min: 1 }} required
                    />
                  </Box>
                );
              }
              if (opName.includes('люверс')) {
                const params = operationParamsDialog.params[op.id] || { eyeletId: '', eyeletStepCm: 40 };
                return (
                  <Box key={op.id} sx={{ border: '1px solid #e0e0e0', borderRadius: 1, p: 2 }}>
                    <Typography variant="subtitle2" gutterBottom color="primary">{op.name}</Typography>
                    <TextField
                      select fullWidth margin="dense" label="Размер люверса"
                      value={params.eyeletId}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev, params: {
                          ...prev.params, [op.id]: { ...prev.params[op.id], eyeletId: e.target.value }
                        }
                      }))}
                      required
                    >
                      <MenuItem value="">Выберите размер</MenuItem>
                      {operationsData
                        .filter(o => o.name.toLowerCase().includes('люверс'))
                        .map(eyelet => (
                          <MenuItem key={eyelet.id} value={eyelet.id}>
                            {eyelet.name} — {eyelet.pricePerPiece} ₽/шт
                          </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                      fullWidth margin="dense" label="Шаг установки (см)" type="number"
                      value={params.eyeletStepCm}
                      onChange={(e) => setOperationParamsDialog(prev => ({
                        ...prev, params: {
                          ...prev.params, [op.id]: { ...prev.params[op.id], eyeletStepCm: parseInt(e.target.value) || 0 }
                        }
                      }))}
                      inputProps={{ min: 10 }} required
                    />
                  </Box>
                );
              }
              return null;
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
