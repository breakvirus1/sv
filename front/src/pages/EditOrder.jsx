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
  FormControlLabel,
  InputAdornment
} from '@mui/material';
import { ArrowBack, Add, Delete, Save, Info } from '@mui/icons-material';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import ClientInfo from '../components/ClientInfo';

const EditOrder = ({ order, orderNumber, onSuccess, mode = 'edit' }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const identifier = orderNumber || order?.id;
  const isOrderNumber = !!orderNumber && !order;

  const { data: fetchedOrder, isLoading: isLoadingOrder, isError } = useQuery({
    queryKey: ['order', identifier],
    queryFn: async () => {
      if (order) return order;
      const endpoint = isOrderNumber && /^\d{17}$/.test(identifier)
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
  const [priceplus, setPriceplus] = useState(order?.priceplus || 0);
  const [calculatedItems, setCalculatedItems] = useState([]);
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
const [clientInfoDialog, setClientInfoDialog] = useState({ open: false, clientId: null });
// Диалог редактирования суммы заказа
const [orderAmountDialog, setOrderAmountDialog] = useState({ open: false, sumorder: 0 });
// State for calculated total
const [totalOrderAmount, setTotalOrderAmount] = useState(0);

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

// Initialize totalOrderAmount from order's saved total on mount
   useEffect(() => {
     if (orderData?.totalAmount != null) {
       setTotalOrderAmount(orderData.totalAmount);
     }
   }, [orderData?.id]);

  // Track if user has modified any item
  const [isModified, setIsModified] = useState(false);

  // Initialize form data when order is loaded
  useEffect(() => {
    if (orderData) {
      const formatDate = (dateStr) => {
        if (!dateStr) return '';
        return dateStr.split('T')[0];
      };
      const items = (orderData.materials ?? [])
        .filter((mat, idx, arr) => arr.findIndex(m => m.id === mat.id) === idx)
        .map((mat) => ({
          id: mat.id,
          materialId: String(mat.material?.id || ''),
          qty1value: mat.widthM != null ? mat.widthM.toString() : '',
          unit: 'м',
          qty2value: mat.heightM != null ? mat.heightM.toString() : '',
          readyDate: mat.readyDate || '',
          operations: (mat.operations ?? []).map((op) => ({ id: op.operationId, operationId: op.operationId, widthM: op.widthM, heightM: op.heightMm }))
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

  const isM2 = (mat) => {
    if (!mat || !mat.unit) return false;
    const u = mat.unit.trim().toLowerCase().replace(/[.]/g, '');
    return u === 'м2' || u === 'm2';
  };

// Track if items were modified to avoid recalculating on initial load
   const initialItemsRef = useRef(null);

   // Recalculate total when items change
   useEffect(() => {
     const calculateTotal = async () => {
       let total = 0;
       for (const item of formData.items) {
         if (!item.materialId || !item.qty1value || !item.qty2value) {
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

       // Apply client markup (priceplus)
       if (orderData?.client?.priceplus != null && orderData.client.priceplus > 0) {
         total = total * (1 + orderData.client.priceplus / 100);
       }

       setTotalOrderAmount(total);
     };

     // Skip calculation on initial load - use saved totalAmount from order
     if (initialItemsRef.current === null) {
       initialItemsRef.current = JSON.stringify(formData.items);
       return;
     }

     if (formData.items.length > 0 && materialsData.length > 0) {
       calculateTotal();
     } else {
       setTotalOrderAmount(0);
     }
   }, [formData.items, materialsData, orderData?.client?.priceplus]);

  // Fetch default dimensions for an existing item from backend (on mount and when orderData changes)
  useEffect(() => {
    if (!orderData || !materialsData.length) return;
    formData.items.forEach((item, index) => {
      if (!item.id || item.qty1value && item.qty2value) return;
      const mat = materialsData.find(m => m.id === parseInt(item.materialId));
      const needsFetch = mat && (mat.defaultWidthM != null || mat.defaultHeightM != null);
      if (!needsFetch) return;

      pendingDimRef.current.set(index, item.materialId);

      api.get(`/api/v1/orders/${orderData.id}/positions/${item.id}/default`)
        .then(r => {
          const d = r.data;
          const wM = d.widthM != null ? parseFloat(d.widthM) : 0;
          const hM = d.heightM != null ? parseFloat(d.heightM) : 0;
          setFormData(prev => {
            const curr = prev.items[index];
            if (!curr || curr.materialId !== item.materialId || (curr.qty1value && curr.qty2value)) {
              return prev;
            }
            return {
              ...prev,
              items: prev.items.map((it, i) =>
                i === index ? {
                  ...it,
                  qty1value: wM != null ? wM.toString() : '',
                  qty2value: hM != null ? hM.toString() : ''
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
    setIsModified(true);
    setFormData(prev => {
      const newIndex = prev.items.length;
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
        const nextItems = [...prev.items, { materialId: candidateId, qty1value: '', unit: 'м', qty2value: '', readyDate: '', operations: [] }];
        pendingDimRef.current.set(newIndex, candidateId);
        api.get(`/api/v1/materials/${candidateId}`)
          .then(r => {
            const m = r.data;
            const wM = m.defaultWidthM != null ? parseFloat(m.defaultWidthM) : null;
            const hM = m.defaultHeightM != null ? parseFloat(m.defaultHeightM) : null;
            setFormData(pd => {
              const curr = pd.items[newIndex];
              if (!curr || curr.materialId !== candidateId) return pd;
              return {
                ...pd,
                items: pd.items.map((it, i) =>
                  i === newIndex ? { ...it, qty1value: wM != null ? wM.toString() : '', qty2value: hM != null ? hM.toString() : '' } : it
                )
              };
            });
          })
          .catch(() => {})
          .finally(() => pendingDimRef.current.delete(newIndex));
        return { ...prev, items: nextItems };
      }
        return { ...prev, items: [...prev.items, { materialId: '', qty1value: '', unit: 'м', qty2value: '', readyDate: '', operations: [] }] };
    });
  };

  const removeItem = (index) => {
    setIsModified(true);
    setFormData(prev => ({
      ...prev,
      items: prev.items.filter((_, i) => i !== index)
    }));
  };

  const convertUnit = (value, fromUnit, toUnit) => {
    if (fromUnit === toUnit) return value;
    const num = parseFloat(value) || 0;
    if (fromUnit === 'м' && toUnit === 'мм') return (num * 1000).toString();
    if (fromUnit === 'мм' && toUnit === 'м') return (num / 1000).toString();
    return value;
  };

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
      const oldUnit = item.unit || 'м';
      setIsModified(true);
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

  const updateItem = (index, field, value) => {
    setFormData(prev => {
      if (field === 'materialId' && value) {
        setIsModified(true);
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

        const next = prev.items.map((item, i) =>
          i === index ? { ...item, materialId: value, qty1value: '', qty1unit: 'м', qty2value: '', qty2unit: 'м', readyDate: '', operations: [] } : item
        );

        pendingDimRef.current.set(index, value);

        api.get(`/api/v1/orders/${orderData?.id}/positions/${prev.items[index]?.id || ''}/default`)
          .then(r => {
            const d = r.data;
            const wM = d.widthM != null ? parseFloat(d.widthM) : null;
            const hM = d.heightM != null ? parseFloat(d.heightM) : null;
            const matResp = d.material || materialsData.find(m => m.id === parseInt(value));
            const dwM = wM != null ? wM : (matResp?.defaultWidthM != null ? parseFloat(matResp.defaultWidthM) : null);
            const dhM = hM != null ? hM : (matResp?.defaultHeightM != null ? parseFloat(matResp.defaultHeightM) : null);

            setFormData(pd => {
              const currMatId = pd.items[index]?.materialId;
              if (currMatId !== value) return pd;
              if (dwM == null && dhM == null) return pd;
              return {
                ...pd,
                items: pd.items.map((it, i) =>
                  i === index ? { ...it, qty1value: dwM != null ? dwM.toString() : '', qty2value: dhM != null ? dhM.toString() : '' } : it
                )
              };
            });
          })
          .catch(() => {})
          .finally(() => pendingDimRef.current.delete(index));

        return { ...prev, items: next };
      }

      setIsModified(true);
      return {
        ...prev,
        items: prev.items.map((item, i) => i === index ? { ...item, [field]: value } : item)
      };
    });
  };

  const updateItemOperations = (index, operations) => {
    setIsModified(true);
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
            initialParams[opId] = { hemWidthMm: existing.hemWidthMm, hemCount: existing.hemCount || 2, widthMm: existing.widthMm, heightMm: existing.heightMm };
          } else {
            const defaultWidth = op.hemWidthMm != null ? op.hemWidthMm : 20;
            const defaultCount = op.hemCount != null ? op.hemCount : 2;
            initialParams[opId] = { hemWidthMm: defaultWidth, hemCount: defaultCount, widthMm: existing?.widthMm || null, heightMm: existing?.heightMm || null };
          }
        } else if (op.name.toLowerCase().includes('люверс')) {
          if (existing && existing.eyeletId) {
            initialParams[opId] = { eyeletId: existing.eyeletId, eyeletStepCm: existing.eyeletStepCm || 40, widthMm: existing.widthMm, heightMm: existing.heightMm };
          } else {
            initialParams[opId] = { eyeletId: '', eyeletStepCm: 40, widthMm: existing?.widthMm || null, heightMm: existing?.heightMm || null };
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
      const opsWithDimensions = selectedOpsData.map(op => {
        const existing = currentOps.find(cop => cop.id === op.id);
        return existing ? { ...op, ...existing } : { ...op, widthMm: null, heightMm: null };
      });
      updateItemOperations(itemIndex, opsWithDimensions);
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
      const toMeters = (value, unit) => {
        const v = parseFloat(value) || 0;
        return unit === 'мм' ? v / 1000 : v;
      };

      const orderMaterials = formData.items.map(item => {
        const material = materialsData.find(m => m.id === parseInt(item.materialId));
        if (!material) throw new Error('Материал не выбран');
        const widthM = toMeters(item.qty1value, item.unit);
        const heightM = toMeters(item.qty2value, item.unit);

        return {
          ...(item.id ? { id: item.id } : {}),
          materialId: parseInt(item.materialId),
          widthM,
          heightM,
          readyDate: item.readyDate || null,
          operations: (item.operations || []).map(op => ({
            operationId: op.id,
            widthM: op.widthM != null ? op.widthM : null,
            heightM: op.heightM != null ? op.heightM : null
          }))
        };
      });

const orderDataPayload = {
         description: formData.description,
         orderDate: formData.orderDate,
         dueDate: formData.dueDate || null,
         managerId: formData.managerId ? parseInt(formData.managerId) : null,
         priceplus: priceplus,
         items: orderMaterials,
         totalAmount: totalOrderAmount
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

  if (isError) {
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
                  InputProps={{
                    endAdornment: orderData?.client?.id && (
                      <IconButton
                        size="small"
                        onClick={() => setClientInfoDialog({ open: true, clientId: orderData.client.id })}
                        sx={{ p: 0.5 }}
                      >
                        <Info fontSize="small" />
                      </IconButton>
                    )
                  }}
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
             <Grid item xs={12} md={6}>
               <TextField
                 fullWidth
                 label="Процент добавки (priceplus)"
                 type="number"
                 value={priceplus}
                 onChange={(e) => setPriceplus(parseFloat(e.target.value) || 0)}
                 margin="normal"
                 inputProps={{ min: -100, max: 100, step: 0.1 }}
               />
             </Grid>
             <Grid item xs={12} md={6}>
               <Button variant="contained" onClick={() => {
                 const calculated = formData.items.map(item => {
                   const material = materialsData.find(m => m.id === parseInt(item.materialId));
                   if (!material) return { ...item, calculatedCost: 0 };
                   const toMeters = (value, unit) => {
                     const v = parseFloat(value) || 0;
                     return unit === 'мм' ? v / 1000 : v;
                   };
                   const widthM = toMeters(item.qty1value, item.unit);
                   const heightM = material.unit === 'м2' ? toMeters(item.qty2value, item.unit) : 0;
                   let effectiveQty = material.unit === 'м2' ? widthM * heightM : widthM;
                   const baseCost = material.price * effectiveQty * (material.wasteCoefficient || 1);
                   const calculatedCost = baseCost * (1 + priceplus / 100);
                   return { ...item, calculatedCost };
                 });
                 setCalculatedItems(calculated);
                 const total = calculated.reduce((sum, item) => sum + (item.calculatedCost || 0), 0);
                 setTotalOrderAmount(total);
               }} sx={{ mt: 3 }}>
                 Рассчитать
               </Button>
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
                      const showSecond = isM2(material);
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
                            <Box display="flex" gap={0.5} alignItems="center">
                              <TextField
                                size="small"
                                type="number"
                                value={item.qty1value}
                                onChange={(e) => updateItem(index, 'qty1value', e.target.value)}
                                inputProps={{ min: 0, step: 0.001 }}
                                placeholder={material?.unit === 'м2' ? 'Ширина' : 'Длина'}
                                sx={{ width: 100 }}
                              />
                              <Select
                                size="small"
                                value={item.unit || 'м'}
                                onChange={(e) => handleUnitChange(index, e.target.value)}
                                sx={{ width: 70 }}
                              >
                                <MenuItem value="м">м</MenuItem>
                                <MenuItem value="мм">мм</MenuItem>
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
                                inputProps={{ min: 0, step: 0.001 }}
                                placeholder="Высота"
                                sx={{ width: 100 }}
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
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 'auto' }}>
                <Button
                  variant="outlined"
                  onClick={() => setOrderAmountDialog({ open: true, sumorder: totalOrderAmount })}
                  sx={{ fontSize: '1.25rem', fontWeight: 600 }}
                >
                  Сумма: {totalOrderAmount.toFixed(2)} ₽
                </Button>
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

      {/* Operation parameters dialog (podvorot, eyelets, dimensions) */}
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

      {/* Dialog for editing order amount */}
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
