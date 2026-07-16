import { useState, useEffect, useRef } from 'react';
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
  FormGroup,
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
  InputAdornment,
  Link,
  Tooltip
} from '@mui/material';
import { ArrowBack, Add, Delete, Save, Info, Download, Close, Upload } from '@mui/icons-material';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import ClientInfo from '../components/ClientInfo';
import { isM2 } from '../utils/orderUtils';
import { recalculateOrderLocally } from '../services/calculationService';


const EditOrder = ({ order, orderNumber, onSuccess, mode = 'edit' }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const identifier = orderNumber || order?.id;
  const isOrderNumber = !!orderNumber && !order;

    const { data: fetchedOrder, isLoading: isLoadingOrder, isError } = useQuery({
      queryKey: ['order', identifier],
      queryFn: async () => {
        if (order) return order;
        const endpoint = isOrderNumber
          ? `/api/v1/orders/number/${identifier}`
          : `/api/v1/orders/${identifier}`;
        const response = await api.get(endpoint);
        console.log('=== FETCH ORDER ===');
        console.log('Fetched orderId:', response.data.id, 'priceplus:', response.data.priceplus);
        return response.data;
      },
      staleTime: 0,
      enabled: mode !== 'create' && !!identifier && (!order || isOrderNumber)
    });

  const orderData = order || fetchedOrder;

  // Отслеживание изменений orderData
  useEffect(() => {
    console.log('=== orderData changed ===');
    console.log('orderData:', orderData?.id, orderData?.orderNumber);
    console.log('fetchedOrder:', fetchedOrder?.id);
    console.log('prop order:', order?.id);
    console.log('orderData.priceplus:', orderData?.priceplus);
    console.log('orderData.totalAmount:', orderData?.totalAmount);
  }, [orderData, fetchedOrder, order]);

  const [formData, setFormData] = useState({
    description: '',
    orderDate: '',
    dueDate: '',
    managerId: null,
    items: []
});
  const [priceplus, setPriceplus] = useState(order?.priceplus || 0);
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  // Отслеживание ожидающих запросов размеров по индексу позиции
  const pendingDimRef = useRef(new Map()); // index -> materialId
  const fileInputRef = useRef(null);
  const [uploadingIndex, setUploadingIndex] = useState(null);

  // ── Состояния: Диалоги ──
  const [operationsDialog, setOperationsDialog] = useState({ open: false, itemIndex: null, selectedOps: [] });
  const [operationParamsDialog, setOperationParamsDialog] = useState({
    open: false,
    itemIndex: null,
    pendingOps: [],
    params: {}
  });
  const [clientInfoDialog, setClientInfoDialog] = useState({ open: false, clientId: null });

  // ── Запросы: Сотрудники, Материалы, Операции ──
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

  const { data: eyeletsData = [] } = useQuery({
    queryKey: ['eyelets'],
    queryFn: async () => {
      const response = await api.get('/api/v1/calculations/eyelets');
      return response.data || [];
    }
  });

  // ── Запрос расчетных данных заказа с бекенда ──
  const { data: calculatedData } = useQuery({
    queryKey: ['order-calculated', orderData?.id],
    queryFn: async () => {
      if (!orderData?.id) return null;
      const response = await api.get(`/api/v1/orders/${orderData.id}/calculated`);
      console.log('=== ОТВЕТ С РАСЧЕТАМИ ЗАКАЗА ===');
      console.log('Order ID:', orderData.id);
      console.log('Calculated data:', response.data);
      if (response.data?.materials) {
        console.log('Material calculations:');
        response.data.materials.forEach((m, idx) => {
          console.log(`  [${idx}] Material "${m.materialName}" (id=${m.materialId}):`);
          console.log(`      cost: ${m.cost}, costPriceplus: ${m.costPriceplus}`);
          console.log(`      widthM: ${m.widthM}, heightM: ${m.heightM}`);
          console.log(`      operations: ${m.operations?.map(o => `${o.operationName}=${o.subtotal}`).join(', ') || 'none'}`);
        });
      }
      console.log('totalWithPriceplus:', response.data?.totalWithPriceplus);
      return response.data;
    },
    enabled: !!orderData?.id
  });

  // ── Эффект: Отслеживание изменений материалов ──
  useEffect(() => {
    console.log('=== materialsData changed ===');
    console.log('Count:', materialsData.length);
  }, [materialsData]);

  // ── Онлайн расчёт итоговой суммы при изменении позиций ──
  const [liveTotalWithPriceplus, setLiveTotalWithPriceplus] = useState(0);
  const [liveTotalWithoutPriceplus, setLiveTotalWithoutPriceplus] = useState(0);

  const calculateTotals = async () => {
    if (formData.items.length === 0) {
      setLiveTotalWithPriceplus(0);
      setLiveTotalWithoutPriceplus(0);
      return;
    }
    try {
      const result = await recalculateOrderLocally(formData.items, priceplus);
      setLiveTotalWithoutPriceplus(result.totalWithoutPriceplus);
      setLiveTotalWithPriceplus(result.totalWithPriceplus);
    } catch (err) {
      console.error('Calculation error:', err);
    }
  };

  useEffect(() => {
    calculateTotals();
  }, [formData.items, priceplus]);

  // Fallback на данные с бэкенда если онлайн расчёт ещё не выполнен
  const backendTotalWithoutPriceplus = calculatedData?.totalWithoutPriceplus ?? 0;
  const backendTotalWithPriceplus = calculatedData?.totalWithPriceplus ?? 0;
  const displayTotalWithoutPriceplus = liveTotalWithoutPriceplus || backendTotalWithoutPriceplus;
  const displayTotalWithPriceplus = liveTotalWithPriceplus || backendTotalWithPriceplus;

  console.log('=== ИТОГОВАЯ СУММА ЗАКАЗА ===');
  console.log('Live totalWithPriceplus:', liveTotalWithPriceplus);
  console.log('Live totalWithoutPriceplus:', liveTotalWithoutPriceplus);
  console.log('Backend totalWithPriceplus:', calculatedData?.totalWithPriceplus);
  console.log('Backend totalWithoutPriceplus:', calculatedData?.totalWithoutPriceplus);

  // ── Эффект: Инициализация формы при загрузке заказа ──
  useEffect(() => {
    if (orderData) {
      console.log('=== ЗАГРУЗКА ЗАКАЗА ===');
      console.log('Order ID:', orderData.id);
      console.log('Order number:', orderData.orderNumber);
      console.log('Raw materials count:', orderData.materials?.length);
      console.log('Raw materials:', orderData.materials?.map(m => ({
        id: m.id,
        materialId: m.material?.id,
        materialName: m.material?.name,
        cost: m.cost,
        widthM: m.widthM,
        heightM: m.heightM,
        operationsCount: m.operations?.length,
        operations: m.operations?.map(op => ({
          id: op.operationId,
          name: op.operationName,
          subtotal: op.subtotal
        }))
      })));
      console.log('Client priceplus:', orderData.client?.priceplus);

      const formatDate = (dateStr) => {
        if (!dateStr) return '';
        return dateStr.split('T')[0];
      };
      const rawItems = orderData.materials ?? [];
      const dedupedItems = rawItems.filter((mat, idx, arr) => arr.findIndex(m => m.id === mat.id) === idx);
      console.log('After dedup:', dedupedItems.length, 'items (was', rawItems.length, ')');

      const items = dedupedItems.map((mat) => {
        const orderItem = orderData.items?.find(i => i.id === mat.orderItemId);
        return {
          id: mat.id,
          orderItemId: mat.orderItemId,
          materialId: String(mat.material?.id || ''),
          qty1value: mat.widthM != null ? (mat.widthM * 1000).toString() : '',
          unit: 'мм',
          qty2value: mat.heightM != null ? (mat.heightM * 1000).toString() : '',
          readyDate: mat.readyDate || '',
          cost: mat.cost != null ? Number(mat.cost) : null,
          operations: (mat.operations ?? []).map((op) => ({
            id: op.operationId,
            operationId: op.operationId,
            name: op.operationName,
            subtotal: op.subtotal != null ? Number(op.subtotal) : null,
            widthM: op.widthM,
            heightM: op.heightM,
          })),
          fileId: orderItem?.fileId || null,
          fileUrl: orderItem?.fileUrl || null,
        };
      });

      console.log('Mapped items:', items.map(item => ({
        id: item.id,
        materialId: item.materialId,
        cost: item.cost,
        qty1: item.qty1value,
        qty2: item.qty2value,
        opsCount: item.operations.length,
        opsSubtotal: item.operations.reduce((s, o) => s + (o.subtotal || 0), 0)
      })));

      setFormData({
        description: orderData.description || '',
        orderDate: formatDate(orderData.orderDate),
        dueDate: formatDate(orderData.dueDate),
        managerId: orderData.manager?.id ? String(orderData.manager.id) : (orderData.managerId ? String(orderData.managerId) : ''),
        items
      });
      // Используем priceplus из заказа или клиента
      if (orderData.priceplus != null) {
        setPriceplus(orderData.priceplus);
      } else if (orderData.client?.priceplus != null) {
        setPriceplus(orderData.client.priceplus);
      } else {
        setPriceplus(0);
      }
      console.log('Set priceplus from order:', orderData.priceplus, 'or client:', orderData.client?.priceplus);
    }
}, [orderData, mode]);

  // ── Эффект: Загрузка значений по умолчанию для позиций ──
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
    console.log('=== addItem ===');
    console.log('Current items count:', formData.items.length);
    setFormData(prev => {
      const newIndex = prev.items.length;
      const anyMaterial = prev.items.find(item => item.materialId);
      if (anyMaterial && materialsData.length > 0) {
        const candidateId = String(materialsData[0].id);
        const existingCandidate = prev.items.find(item => item.materialId === candidateId);
        if (existingCandidate) {
          const matName = materialsData.find(m => m.id === parseInt(candidateId))?.name;
          const idMsg = existingCandidate.id ? ` (позиция #${existingCandidate.id})` : '';
          setNotification({
            open: true,
            message: `Материал "${matName}" уже выбран${idMsg}`,
            severity: 'warning'
          });
          return prev;
        }
        const nextItems = [...prev.items, { materialId: candidateId, qty1value: '', unit: 'мм', qty2value: '', readyDate: '', operations: [] }];
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
      return { ...prev, items: [...prev.items, { materialId: '', qty1value: '', unit: 'мм', qty2value: '', readyDate: '', operations: [] }] };
    });
  };

const removeItem = (index) => {
    console.log('=== removeItem ===');
    console.log('Removing item at index:', index, 'item:', formData.items[index]);
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
    console.log('=== updateItem ===');
    console.log('index:', index, 'field:', field, 'value:', value);
setFormData(prev => {
       if (field === 'materialId' && value) {
         const currentItem = prev.items[index];
        console.log('Changing materialId for item:', currentItem?.id, 'from:', currentItem?.materialId, 'to:', value);
        const dup = prev.items.find((item, i) => i !== index && item.materialId === value);
        if (dup) {
          const mat = materialsData.find(m => m.id === parseInt(value));
          const dupIdMsg = dup.id ? ` (позиция #${dup.id})` : '';
          setNotification({
            open: true,
            message: `Материал "${mat?.name || value}" уже выбран в другой позиции${dupIdMsg}`,
            severity: 'warning'
          });
          return prev;
        }

        const next = prev.items.map((item, i) =>
          i === index ? { ...item, materialId: value, qty1value: '', qty1unit: 'м', qty2value: '', qty2unit: 'м', readyDate: '', operations: [] } : item
        );

        pendingDimRef.current.set(index, value);

        api.get(`/api/v1/orders/${orderData?.id}/positions/${currentItem?.id || ''}/default`)
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

      return {
        ...prev,
        items: prev.items.map((item, i) => i === index ? { ...item, [field]: value } : item)
      };
    });
  };

  const updateItemOperations = (index, operations) => {
    console.log('=== updateItemOperations ===');
    console.log('index:', index);
    console.log('New operations:', operations.map(op => ({ id: op.id, name: op.name, subtotal: op.subtotal })));
    console.log('Old operations:', formData.items[index]?.operations?.map(op => ({ id: op.id, name: op.name, subtotal: op.subtotal })));
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

  const handleFileSelect = (index) => {
    setUploadingIndex(index);
    fileInputRef.current?.click();
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file || uploadingIndex === null) return;
    const item = formData.items[uploadingIndex];
    if (!item?.orderItemId) {
      setNotification({ open: true, message: 'Сначала сохраните заказ, чтобы привязать файл к позиции', severity: 'warning' });
      e.target.value = '';
      return;
    }
    try {
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
      const fd = new FormData();
      fd.append('file', file);
      fd.append('orderItemId', item.orderItemId);
      fd.append('orderId', orderData.id);
      fd.append('orderNumber', orderData.orderNumber || '');
      fd.append('managerName', orderData.manager?.fullName || '');
      fd.append('clientName', orderData.client?.name || '');
      fd.append('materialName', materialName);
      fd.append('operationNames', operationNames);
      fd.append('operationParams', operationParamsList);
      const response = await api.post('/api/files/upload', fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      const saved = response.data;
      setFormData(prev => ({
        ...prev,
        items: prev.items.map((it, i) => i === uploadingIndex ? {
          ...it,
          fileId: saved.id,
          fileUrl: saved.fileUrl,
        } : it)
      }));
      queryClient.invalidateQueries({ queryKey: ['order', orderData?.id] });
      queryClient.invalidateQueries({ queryKey: ['order-calculated', orderData?.id] });
      setNotification({ open: true, message: `Файл "${saved.fileName}" загружен`, severity: 'success' });
    } catch (err) {
      setNotification({ open: true, message: `Ошибка загрузки: ${err.response?.data?.message || err.message}`, severity: 'error' });
    } finally {
      setUploadingIndex(null);
      e.target.value = '';
    }
  };

  const handleDeleteFile = async (index) => {
    const item = formData.items[index];
    if (!item?.fileId) return;
    if (!window.confirm('Удалить файл?')) return;
    try {
      await api.delete(`/api/files/${item.fileId}`);
      setFormData(prev => ({
        ...prev,
        items: prev.items.map((it, i) => i === index ? { ...it, fileId: null, fileUrl: null } : it)
      }));
      queryClient.invalidateQueries({ queryKey: ['order', orderData?.id] });
      queryClient.invalidateQueries({ queryKey: ['order-calculated', orderData?.id] });
      setNotification({ open: true, message: 'Файл удалён', severity: 'success' });
    } catch (err) {
      setNotification({ open: true, message: `Ошибка удаления: ${err.response?.data?.message || err.message}`, severity: 'error' });
    }
  };

const handleSubmit = async (e) => {
    e.preventDefault();
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
        items: orderMaterials
      };

      console.log('=== EDIT ORDER SUBMIT ===');
      console.log('Order ID:', orderData?.id);
      console.log('Items count:', formData.items.length);
      console.log('Priceplus:', priceplus);
      console.log('Payload:', JSON.stringify(orderDataPayload, null, 2));

      const response = await api.put(`/api/v1/orders/${orderData.id}`, orderDataPayload);

      console.log('=== EDIT ORDER RESPONSE ===');
      console.log('Saved priceplus:', response.data.priceplus);
      console.log('Saved totalAmount:', response.data.totalAmount);
      console.log('Saved totalWithPriceplus:', response.data.totalWithPriceplus);

      setNotification({ open: true, message: 'Заказ успешно обновлен', severity: 'success' });

      // Немедленное обновление кэша
      queryClient.setQueryData(['order', orderData.id], response.data);
      queryClient.invalidateQueries({ queryKey: ['orders'] });

      if (onSuccess) onSuccess();
      navigate(`/orders/${orderData.id}`);
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
                <Box display="flex" gap={1} alignItems="flex-end">
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
                  <TextField
                    size="small"
                    label="Наценка %"
                    type="number"
value={priceplus}
                     onChange={(e) => {
                       const newValue = parseFloat(e.target.value) || 0;
                       console.log('=== priceplus changed ===');
                       console.log('Old:', priceplus, 'New:', newValue);
                       setPriceplus(newValue);
                     }}
                     margin="normal"
                    inputProps={{ min: -100, max: 100, step: 0.1 }}
                    sx={{ width: 120 }}
                  />
                </Box>
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
              <Typography variant="h6" mb={2}>Позиции заказа</Typography>

              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Материал</TableCell>
                      <TableCell width={180}>Размер 1</TableCell>
                      <TableCell width={180}>Размер 2</TableCell>
                      <TableCell width={120}>Операции</TableCell>
                      <TableCell width={200}>Файл</TableCell>
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
                                 placeholder={isM2(material) ? 'Ширина' : 'Длина'}
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
                            {item.fileUrl ? (
                              <Box display="flex" alignItems="center" gap={0.5} flexWrap="wrap">
                                <Link
                                  href={`${import.meta.env.VITE_API_URL || ''}${item.fileUrl}`}
                                  target="_blank"
                                  rel="noopener"
                                  sx={{ fontSize: '0.8rem', wordBreak: 'break-all' }}
                                >
                                  <Download fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.3 }} />
                                  {item.fileUrl.split('/').pop()}
                                </Link>
                                <IconButton size="small" onClick={() => handleDeleteFile(index)} color="error" sx={{ p: 0.3 }}>
                                  <Delete fontSize="small" />
                                </IconButton>
                              </Box>
                            ) : (
                              <Button size="small" variant="text" startIcon={<Upload />} onClick={() => handleFileSelect(index)} sx={{ fontSize: '0.75rem' }}>
                                Загрузить
                              </Button>
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
                  Сумма: {displayTotalWithPriceplus.toFixed(2)} ₽
                  <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                    (без наценки: {displayTotalWithoutPriceplus.toFixed(2)})
                  </Typography>
                </Box>
                <Box display="flex" gap={2}>
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
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mt: 1 }}>
            {Object.values(dialogGroupedData).length === 0 && (
              <Typography color="text.secondary">Нет операций для выбранного материала</Typography>
            )}
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
        onClose={() => setNotification({ ...notification, open: false })}>
        <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })}>
          {notification.message}
        </Alert>
      </Snackbar>

      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileUpload}
        style={{ display: 'none' }}
      />
    </Container>
  );
};

export default EditOrder;
