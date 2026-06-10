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
  List,
  ListItem,
  ListItemText
} from '@mui/material';
import { Add, Delete, Save, AttachFile } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';
import { computeWorkshopTags } from '../utils/workshopTags';
import { isM2, isLinearMeter } from '../utils/orderUtils';
import { recalculateOrderLocally, applyPriceplus } from '../services/calculationService';
import { useAuth } from '../context/AuthContext';
import OperationForm from './OperationForm';

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
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [priceplus, setPriceplus] = useState(0);

   // Operation dialog state
   const [opDialogOpen, setOpDialogOpen] = useState(false);
   const [activeItemIdx, setActiveItemIdx] = useState(null);
   const [materialOpsCache, setMaterialOpsCache] = useState({});
   const [selectedOpTemplate, setSelectedOpTemplate] = useState(null);

   // Клиентский диалог создания нового клиента
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

  // Load material operations when dialog opens
  const currentMaterialId = formData.items[activeItemIdx]?.materialId;
  useEffect(() => {
    if (opDialogOpen && currentMaterialId && !materialOpsCache[currentMaterialId]) {
      api.get(`/api/v1/materials/${currentMaterialId}/operations`).then(res => {
        setMaterialOpsCache(prev => ({ ...prev, [currentMaterialId]: res.data || [] }));
      }).catch(err => {
        console.error('Failed to load operations', err);
      });
    }
  }, [opDialogOpen, currentMaterialId, materialOpsCache]);

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
      items: [...prev.items, { materialId: '', qty1: '', qty2: '', readyDate: '', itemCount: 1, operations: [] }]
    }));
  };

  const removeItem = (index) => {
    setFormData(prev => ({
      ...prev,
      items: prev.items.filter((_, i) => i !== index)
    }));
  };

   const updateItem = (index, field, value) => {
     if (field === 'materialId') {
       // If material changes, clear associated operations
       setFormData(prev => {
         const items = [...prev.items];
         items[index] = { ...items[index], [field]: value, operations: [] };
         return { ...prev, items };
       });
     } else {
       setFormData(prev => ({
         ...prev,
         items: prev.items.map((item, i) => i === index ? { ...item, [field]: value } : item)
       }));
     }
   };

   const openOperationDialog = (index) => {
     setActiveItemIdx(index);
     setOpDialogOpen(true);
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
    // Объединяем pendingOps с их параметрами, преобразуя типы при необходимости
    const opsWithParams = pendingOps.map(op => {
      const baseOp = operationsData.find(o => o.id === op.id);
      const opParams = params[op.id] || {};
      // Преобразуем eyeletId в число если присутствует
      if (opParams.eyeletId !== undefined) {
        opParams.eyeletId = opParams.eyeletId ? parseInt(opParams.eyeletId, 10) : null;
      }
      return { ...baseOp, ...opParams };
    });

    // Получаем существующие операции и обновляем/добавляем специальные
    const currentOps = formData.items[itemIndex].operations || [];
    // Удаляем существующие специальные операции с теми же id чтобы избежать дубликатов
    const filteredOps = currentOps.filter(cop => !pendingOps.some(pop => pop.id === cop.id));
    // Объединяем: существующие не-специальные операции + новые специальные с параметрами
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

    // Проверяем специальные операции (подворот или люверс) - нечувствительно к регистру
    const specialOps = selectedOpsData.filter(op =>
      op.name.toLowerCase().includes('подворот') || op.name.toLowerCase().includes('люверс')
    );

    if (specialOps.length > 0) {
      // Проверяем, есть ли у специальных операций уже параметры в currentOps
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
        // Сохраняем существующие параметры для всех специальных операций
        const ops = selectedOpsData.map(op => {
          const existing = currentOps.find(cop => cop.id === op.id);
          return existing ? { ...op, ...existing } : op;
        });
        updateItemOperations(itemIndex, ops);
        handleCloseOperationsDialog();
        return;
      }

      // Инициализируем параметры для всех специальных операций, сохраняя существующие если есть
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
      // Нет специальных операций, сохраняем сразу
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

  // Отладочное логирование расчёта по позициям в консоль браузера
  useEffect(() => {
    // Логируем только в development режиме
    if (process.env.NODE_ENV !== 'development') return;

    const items = formData.items;
    if (!items || items.length === 0) return;

    console.group('📊 Расчет по позициям заказа');
    let grandTotal = 0;

    items.forEach((item, idx) => {
      const material = materialsData.find(m => m.id === parseInt(item.materialId));
      if (!material) return;

      const q1 = parseFloat(item.qty1) || 0;
      const q2 = parseFloat(item.qty2) || 0;

      // Эффективное количество материала с учётом единиц измерения
      let effectiveQty = 0;
      if (material.unit === 'м2') {
        effectiveQty = (q1 / 1000) * (q2 / 1000);
      } else if (material.unit === 'м.п.') {
        effectiveQty = q1 / 1000;
      } else {
        effectiveQty = q1;
      }

      const matWasteCoef = material.wasteCoefficient || 1;
      const matCost = material.price * effectiveQty * matWasteCoef;

      // Обработка операций
      let opsCost = 0;
      let pieceCount = 0; // общее количество изделий в штуках по операциям
      const opsDetails = (item.operations || []).map(op => {
        const opQty = op.quantity || 0;
        const opUnit = op.unit || 'шт';
        const opPrice = op.basePrice || 0;
        const opWasteCoef = op.wasteCoefficient || 1;
        const opCost = opPrice * opQty * opWasteCoef;
        opsCost += opCost;

        // Считаем штучные операции как количество изделий
        if (opUnit === 'шт') {
          pieceCount += opQty;
        }

        return {
          name: op.name,
          qty: opQty,
          unit: opUnit,
          price: opPrice,
          cost: opCost
        };
      });

      const itemTotal = matCost + opsCost;
      grandTotal += itemTotal;

      console.log(`Позиция ${idx + 1}: ${material.name}`);
      console.log(`  Исходные размеры: ${q1} × ${q2} мм`);
      console.log(`  Материал: ${effectiveQty.toFixed(4)} ${material.unit} × ${material.price} ₽ (коэф.отх. ${matWasteCoef}) = ${matCost.toFixed(2)} ₽`);

      if (opsDetails.length > 0) {
        console.log(`  Операции:`);
        opsDetails.forEach(d => {
          console.log(`    • ${d.name}: ${d.qty} ${d.unit} × ${d.price} ₽ = ${d.cost.toFixed(2)} ₽`);
        });
      }

      console.log(`  >>> Итого по позиции: ${itemTotal.toFixed(2)} ₽`);

      // Если есть штучные операции, указываем количество изделий
      if (pieceCount > 0) {
        console.log(`  Количество изделий (по операциям в штуках): ${pieceCount} шт.`);
      }

      console.log('');
    });

    console.log(`Общая сумма заказа: ${grandTotal.toFixed(2)} ₽`);
    console.groupEnd();

  }, [formData.items, materialsData]); // Зависимость от изменений в позициях и материалах

  // Вспомогательная функция: расчёт количества материала с учётом припусков от операции раскроя
  const calculateMaterialBaseQty = (item, material) => {
    const q1 = parseFloat(item.qty1) || 0;
    const q2 = parseFloat(item.qty2) || 0;
    let baseQty = 0;

    if (material.unit === 'м2') {
      // Базовая площадь в мм²
      let areaMm2 = q1 * q2;

      // Ищем операцию раскроя (CUTTING) среди операций позиции
      const cuttingOp = (item.operations || []).find(op => {
        const p = op.parameters || {};
        // Операция раскроя имеет параметры marginWidth, marginHeight, sides
        return p.marginWidth !== undefined && p.marginHeight !== undefined && p.sides !== undefined;
      });

      if (cuttingOp) {
        const marginWidth = parseFloat(cuttingOp.parameters.marginWidth) || 50;
        const marginHeight = parseFloat(cuttingOp.parameters.marginHeight) || 50;
        const sides = parseFloat(cuttingOp.parameters.sides) || 1;
        // Добавляем площадь припусков по формуле: marginWidth * width * sides + marginHeight * height * sides
        const extraAreaMm2 = marginWidth * q1 * sides + marginHeight * q2 * sides;
        areaMm2 += extraAreaMm2;
      }

      baseQty = areaMm2 / 1_000_000;
    } else if (material.unit === 'м.п.') {
      baseQty = q1 / 1000;
    } else {
      baseQty = q1;
    }

    return baseQty;
  };

  // Расчет общей суммы заказа
  const totalOrderAmount = useMemo(() => {
    let total = 0;
    formData.items.forEach(item => {
      const material = materialsData.find(m => m.id === parseInt(item.materialId));
      if (material) {
        const baseQty = calculateMaterialBaseQty(item, material);
        total += material.price * baseQty * (material.wasteCoefficient || 1);
      }
      // Operations cost
      if (item.operations && Array.isArray(item.operations)) {
        item.operations.forEach(op => {
          total += op.basePrice * op.quantity * (op.wasteCoefficient || 1);
          // Additional materials cost
          if (op.additionalMaterials) {
            Object.entries(op.additionalMaterials).forEach(([matId, qty]) => {
              const addMat = materialsData.find(m => m.id === parseInt(matId));
              if (addMat) {
                total += addMat.price * parseFloat(qty);
              }
            });
          }
        });
      }
    });
    return total;
  }, [formData.items, materialsData]);

  // Детальное логирование при создании заказа
  const logOrderCreation = () => {
    const items = formData.items;
    if (!items || items.length === 0) {
      console.warn('⚠️ Попытка создать заказ без позиций');
      return;
    }

    console.group('🚀 Создание заказа — детальное логирование');
    console.log('📋 Заказ:', {
      clientId: formData.clientId,
      orderDate: formData.orderDate,
      dueDate: formData.dueDate || 'не указан',
      description: formData.description || 'отсутствует',
      itemsCount: items.length
    });

    let grandTotal = 0;
    let totalMaterialQty = 0;
    let totalOpsCost = 0;

    items.forEach((item, idx) => {
      const material = materialsData.find(m => m.id === parseInt(item.materialId));
      if (!material) {
        console.warn(`⚠️ Позиция ${idx + 1}: материал не найден (ID: ${item.materialId})`);
        return;
      }

      const baseQty = calculateMaterialBaseQty(item, material);
      const matWasteCoef = material.wasteCoefficient || 1;
      const effectiveQty = baseQty * matWasteCoef;
      const matCost = material.price * effectiveQty;
      totalMaterialQty += baseQty;

      console.log(`\n📦 Позиция ${idx + 1}: ${material.name} (ID: ${material.id})`);
      console.log(`   ├─ Размеры: ${item.qty1} × ${item.qty2} мм`);

      // Check for cutting operation to show margin info
      const cuttingOp = (item.operations || []).find(op => {
        const p = op.parameters || {};
        return p.marginWidth !== undefined && p.marginHeight !== undefined && p.sides !== undefined;
      });
      if (cuttingOp) {
        const mW = parseFloat(cuttingOp.parameters.marginWidth) || 50;
        const mH = parseFloat(cuttingOp.parameters.marginHeight) || 50;
        const sides = parseFloat(cuttingOp.parameters.sides) || 1;
        console.log(`   ├─ Припуски: ±${mW} мм (ш), ±${mH} мм (в), сторон: ${sides}`);
      }

      console.log(`   ├─ Материал: ${baseQty.toFixed(6)} ${material.unit} × ${material.price} ₽/${material.unit} (отходы: ×${matWasteCoef})`);
      console.log(`   └─ Стоимость материала: ${matCost.toFixed(2)} ₽`);

      // Детальное логирование операций
      let itemOpsCost = 0;
      if (item.operations && item.operations.length > 0) {
        console.log(`   🔧 Операции (${item.operations.length}):`);
        item.operations.forEach((op, opIdx) => {
          const opQty = op.quantity || 0;
          const opUnit = op.unit || 'шт';
          const opPrice = op.basePrice || 0;
          const opWasteCoef = op.wasteCoefficient || 1;
          const opCost = opPrice * opQty * opWasteCoef;
          itemOpsCost += opCost;
          totalOpsCost += opCost;

          console.log(`      ${opIdx + 1}. ${op.name}`);
          console.log(`         ├─ Количество: ${opQty} ${opUnit}`);
          console.log(`         ├─ Цена: ${opPrice} ₽ за ${opUnit}`);
          console.log(`         ├─ Отходы: ×${opWasteCoef}`);
          console.log(`         └─ Итого: ${opCost.toFixed(2)} ₽`);

          // Логируем параметры операции
          if (op.parameters && Object.keys(op.parameters).length > 0) {
            console.log(`         📐 Параметры:`, op.parameters);
          }

          // Логируем дополнительные материалы
          if (op.additionalMaterials && Object.keys(op.additionalMaterials).length > 0) {
            console.log(`         📦 Доп. материалы:`, op.additionalMaterials);
          }
        });
      } else {
        console.log(`   🔧 Операции: отсутствуют`);
      }

      const itemTotal = matCost + itemOpsCost;
      grandTotal += itemTotal;

      console.log(`   ✅ Итого по позиции: ${itemTotal.toFixed(2)} ₽`);
    });

    console.log('\n📊 СВОДКА ЗАКАЗА');
    console.log(`   Материалы: ${totalMaterialQty.toFixed(6)} ед.`);
    console.log(`   Стоимость материалов: ${(grandTotal - totalOpsCost).toFixed(2)} ₽`);
    console.log(`   Стоимость операций: ${totalOpsCost.toFixed(2)} ₽`);
    console.log(`   🏁 ОБЩАЯ СУММА: ${grandTotal.toFixed(2)} ₽`);
    console.groupEnd();
  };

   const handleSubmit = async (e) => {
     e.preventDefault();
     if (!currentEmployee) {
       setNotification({ open: true, message: 'Менеджер не определен. Перелогинитесь.', severity: 'error' });
       return;
     }

     // Логируем детали заказа перед отправкой
     logOrderCreation();

     setIsSubmitting(true);
     try {
      const orderMaterials = formData.items.map(item => {
        const material = materialsData.find(m => m.id === parseInt(item.materialId));
        if (!material) throw new Error('Материал не выбран');
        // Рассчитываем количество материала с учётом припусков (если есть операция раскроя)
        const baseQty = calculateMaterialBaseQty(item, material);
        // Map operations
        const ops = (item.operations || []).map(op => ({
          materialOperationId: op.materialOperationId,
          quantity: op.quantity,
          wasteCoefficient: op.wasteCoefficient,
          parameters: op.parameters,
          additionalMaterials: op.additionalMaterials
        }));
        return {
          materialId: parseInt(item.materialId),
          quantity: baseQty,
          width: item.qty1 ? parseFloat(item.qty1) / 1000 : null,
          height: item.qty2 ? parseFloat(item.qty2) / 1000 : null,
          itemCount: parseInt(item.itemCount) || 1,
          readyDate: item.readyDate || null,
          operations: ops
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

       console.log('📤 Отправка данных на сервер:', orderData);
       await api.post('/api/v1/orders', orderData);
       setNotification({ open: true, message: 'Заказ успешно создан', severity: 'success' });

       // Invalidate queries
       await queryClient.invalidateQueries({ queryKey: ['orders'] });
        await queryClient.invalidateQueries({ queryKey: ['recentOrders'] });

       setTimeout(() => {
         handleClose();
       }, 1500);
     } catch (err) {
       console.error('❌ Ошибка при создании заказа:', err);
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
                     <TableCell width={150}>Размер 1 (мм)</TableCell>
                     <TableCell width={150}>Размер 2 (мм)</TableCell>
                     <TableCell width={80}>Кол-во</TableCell>
                     <TableCell width={150}>Срок готовности</TableCell>
                     <TableCell>Операции</TableCell>
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
                         <TableCell width={80}>
                           <TextField
                             fullWidth
                             size="small"
                             type="number"
                             value={item.itemCount}
                             onChange={(e) => updateItem(index, 'itemCount', e.target.value)}
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
                          {item.materialId && (
                            <Button size="small" startIcon={<Add />} onClick={() => openOperationDialog(index)}>
                              Операция
                            </Button>
                          )}
                          {item.operations && item.operations.length > 0 && (
                            <Typography variant="body2">{item.operations.length} оп.</Typography>
                          )}
                        </TableCell>
                        <TableCell>
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

      {/* Operation Selection & Configuration Dialog */}
      <Dialog open={opDialogOpen} onClose={() => { setOpDialogOpen(false); setSelectedOpTemplate(null); }} maxWidth="md" fullWidth>
        <DialogTitle>
          {!selectedOpTemplate ? 'Выберите операцию' : `Настройка: ${selectedOpTemplate.name}`}
        </DialogTitle>
        <DialogContent>
          {!selectedOpTemplate ? (
            <Box>
              {!formData.items[activeItemIdx]?.materialId ? (
                <Typography>Выберите материал для позиции.</Typography>
              ) : (materialOpsCache[formData.items[activeItemIdx]?.materialId] || []).length === 0 ? (
                <Typography>Нет операций для этого материала.</Typography>
              ) : (
                <List>
                  {(materialOpsCache[formData.items[activeItemIdx]?.materialId] || []).map(op => (
                    <ListItem button key={op.id} onClick={() => setSelectedOpTemplate(op)}>
                      <ListItemText primary={op.name} secondary={`${op.basePrice} ₽ за ${op.unit}`} />
                    </ListItem>
                  ))}
                </List>
              )}
            </Box>
          ) : (
            <OperationForm
              template={selectedOpTemplate}
              initialDimensions={{
                width: parseFloat(formData.items[activeItemIdx]?.qty1) || 0,
                height: parseFloat(formData.items[activeItemIdx]?.qty2) || 0
              }}
              itemQuantity={parseInt(formData.items[activeItemIdx]?.itemCount) || 1}
              materials={materialsData}
              onConfirm={(opData) => {
                const newOp = {
                  id: Date.now(),
                  materialOperationId: selectedOpTemplate.id,
                  name: selectedOpTemplate.name,
                  basePrice: selectedOpTemplate.basePrice,
                  unit: selectedOpTemplate.unit,
                  wasteCoefficient: selectedOpTemplate.wasteCoefficient || 1,
                  quantity: opData.quantity,
                  parameters: opData.parameters,
                  additionalMaterials: opData.additionalMaterials
                };
                newOp.cost = selectedOpTemplate.basePrice * opData.quantity * (selectedOpTemplate.wasteCoefficient || 1);
                setFormData(prev => {
                  const items = [...prev.items];
                  const idx = activeItemIdx;
                  items[idx].operations = items[idx].operations || [];
                  items[idx].operations.push(newOp);
                  return { ...prev, items };
                });
                setSelectedOpTemplate(null);
                setOpDialogOpen(false);
              }}
              onCancel={() => setSelectedOpTemplate(null)}
            />
          )}
        </DialogContent>
      </Dialog>

      <Snackbar
        open={notification.open}
        autoHideDuration={3000}
        onClose={() => setNotification({ ...notification, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
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
