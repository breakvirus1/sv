import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Tabs,
  Tab,
  Paper,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Button,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  Snackbar,
  Grid,
  Divider,
  TableContainer,
  Checkbox,
  FormControlLabel
} from '@mui/material';
import { Add, Edit, Delete, Refresh, Save, Cancel, Sync } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';
import ClientsTab from '../components/AdminPanel/ClientsTab';
import MaterialsTab from '../components/AdminPanel/MaterialsTab';
import ProductsTab from '../components/AdminPanel/ProductsTab';
import GenerateTab from '../components/AdminPanel/GenerateTab';

const OPERATION_TYPES = [
  { value: 'PRINT', label: 'Печать' },
  { value: 'LAMINATION', label: 'Ламинация' },
  { value: 'CUTTING', label: 'Раскрой' },
  { value: 'WELDING', label: 'Сварка' },
  { value: 'EYELETS', label: 'Люверсы' },
  { value: 'POCKET', label: 'Карман под трубу' },
  { value: 'INSTALLATION', label: 'Монтаж / оклейка' },
  { value: 'ADDITIONAL_MATERIAL', label: 'Дополнительные материалы' },
  { value: 'CUSTOM', label: 'Кастомная операция' }
];

const getOperationTypeLabel = (type) => {
  const found = OPERATION_TYPES.find(t => t.value === type);
  return found ? found.label : type;
};

const AdminPanel = () => {
  const [tab, setTab] = useState(0);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
  const showNotification = (message, severity = 'success') => {
    setNotification({ open: true, message, severity });
  };

  // Clients
  const [clientDialogOpen, setClientDialogOpen] = useState(false);
  const [clientDeleteDialogOpen, setClientDeleteDialogOpen] = useState(false);
  const [selectedClient, setSelectedClient] = useState(null);
  const [clientForm, setClientForm] = useState({ name: '', type: 'PRIVATE', contactPerson: '', phone: '', email: '' });

   // Materials
   const [materialDialogOpen, setMaterialDialogOpen] = useState(false);
   const [materialDeleteDialogOpen, setMaterialDeleteDialogOpen] = useState(false);
   const [selectedMaterial, setSelectedMaterial] = useState(null);
   const [materialTab, setMaterialTab] = useState(0); // для вкладок диалога материала
   const [materialForm, setMaterialForm] = useState({
     name: '',
     unit: '',
     price: '',
     wasteCoefficient: '1'
   });
   // Material Operations (reference works for material)
   const [materialOperations, setMaterialOperations] = useState([]);
   const [opForm, setOpForm] = useState({
     name: '',
     description: '',
     operationType: 'PRINT',
     basePrice: '',
     wasteCoefficient: '1',
     unit: 'шт',
     requiresDimensions: false,
     allowsAdditionalMaterials: false,
     sortOrder: 0,
     active: true
   });
   const [parameters, setParameters] = useState([]);
   const [additionalMaterials, setAdditionalMaterials] = useState([]);
   const [editingOpId, setEditingOpId] = useState(null);

   // Products
  const [productDialogOpen, setProductDialogOpen] = useState(false);
  const [productDeleteDialogOpen, setProductDeleteDialogOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [productForm, setProductForm] = useState({
    name: '',
    article: '',
    description: '',
    width: '',
    height: '',
    unit: 'шт',
    basePrice: '',
    materials: [],
    operations: []
  });

  // Generation
  const [generating, setGenerating] = useState({ clients: false, materials: false, orders: false });

  // Queries
  const { data: clientsData = [], refetch: refetchClients } = useQuery({
    queryKey: ['admin-clients'],
    queryFn: async () => { const r = await api.get('/api/v1/clients?size=100'); return r.data.content || []; },
    enabled: tab === 0
  });

  const { data: materialsData = [], refetch: refetchMaterials } = useQuery({
    queryKey: ['admin-materials'],
    queryFn: async () => { const r = await api.get('/api/v1/materials?size=100'); return r.data.content || []; },
    enabled: tab === 1 || tab === 3
  });

   const { data: productsData = [], refetch: refetchProducts } = useQuery({
     queryKey: ['admin-products'],
     queryFn: async () => {
       const response = await api.get('/api/v1/products?size=100');
       return response.data || [];
     },
     enabled: tab === 2
   });

   // Query: Material Operations (for selected material)
   const { data: materialOpsData = [], refetch: refetchMaterialOps } = useQuery({
     queryKey: ['material-operations', selectedMaterial?.id],
     queryFn: async () => {
       if (!selectedMaterial) return [];
       const response = await api.get(`/api/v1/materials/${selectedMaterial.id}/operations`);
       return response.data || [];
     },
     enabled: !!selectedMaterial && materialDialogOpen
   });

   // Mutations for Clients
  const createClientMutation = useMutation({
    mutationFn: (c) => api.post('/api/v1/clients', c),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-clients'] }); setClientDialogOpen(false); showNotification('Клиент создан'); resetClientForm(); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });
  const updateClientMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/clients/${id}`, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-clients'] }); setClientDialogOpen(false); showNotification('Клиент обновлен'); resetClientForm(); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });
  const deleteClientMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/clients/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-clients'] }); setClientDeleteDialogOpen(false); showNotification('Клиент удален'); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  // Mutations for Materials
  const createMaterialMutation = useMutation({
    mutationFn: (m) => api.post('/api/v1/materials', m),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-materials'] }); setMaterialDialogOpen(false); showNotification('Материал создан'); resetMaterialForm(); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });
  const updateMaterialMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/materials/${id}`, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-materials'] }); showNotification('Материал обновлен'); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });
  const deleteMaterialMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/materials/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-materials'] }); setMaterialDeleteDialogOpen(false); showNotification('Материал удален'); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

   const deleteMaterialMutation = useMutation({
     mutationFn: (id) => api.delete(`/api/v1/materials/${id}`),
     onSuccess: () => {
       queryClient.invalidateQueries({ queryKey: ['admin-materials'] });
       setMaterialDeleteDialogOpen(false);
       showNotification('Материал удален');
     },
     onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
   });

    // Mutations for Material Operations
    const createMaterialOpMutation = useMutation({
      mutationFn: ({ materialId, data }) => api.post(`/api/v1/materials/${materialId}/operations`, data),
      onSuccess: (_, { materialId }) => {
        queryClient.invalidateQueries({ queryKey: ['material-operations', materialId] });
        queryClient.invalidateQueries({ queryKey: ['admin-materials'] });
        resetOpForm();
        showNotification('Операция добавлена');
      },
      onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
    });

    const updateMaterialOpMutation = useMutation({
      mutationFn: ({ materialId, opId, data }) => api.put(`/api/v1/materials/${materialId}/operations/${opId}`, data),
      onSuccess: (_, { materialId }) => {
        queryClient.invalidateQueries({ queryKey: ['material-operations', materialId] });
        queryClient.invalidateQueries({ queryKey: ['admin-materials'] });
        resetOpForm();
        showNotification('Операция обновлена');
      },
      onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
    });

   const deleteMaterialOpMutation = useMutation({
     mutationFn: ({ materialId, opId }) => api.delete(`/api/v1/materials/${materialId}/operations/${opId}`),
     onSuccess: (_, { materialId }) => {
       queryClient.invalidateQueries({ queryKey: ['material-operations', materialId] });
       queryClient.invalidateQueries({ queryKey: ['admin-materials'] });
       showNotification('Операция удалена');
     },
     onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
    });

    // Effect to sync materialOperations from query
    useEffect(() => {
      if (materialOpsData) {
        setMaterialOperations(materialOpsData);
      }
    }, [materialOpsData]);

    // Operation form handlers
    const handleOpChange = (field, value) => {
      // Special handling for CUTTING operation type
      if (field === 'operationType' && value === 'CUTTING') {
        setOpForm(prev => ({
          ...prev,
          operationType: value,
          requiresDimensions: false,
          unit: 'шт',
          basePrice: '0',
          wasteCoefficient: '1'
        }));
        // Add default parameters for cutting if not present
        setParameters(prev => {
          const existingKeys = new Set(prev.map(p => p.paramKey));
          const needed = [
            { paramKey: 'marginWidth', displayName: 'Припуск по ширине (мм)', type: 'NUMBER', unit: 'мм', defaultValue: 50, required: false, sortOrder: prev.length },
            { paramKey: 'marginHeight', displayName: 'Припуск по высоте (мм)', type: 'NUMBER', unit: 'мм', defaultValue: 50, required: false, sortOrder: prev.length + 1 },
            { paramKey: 'sides', displayName: 'Количество сторон', type: 'NUMBER', unit: 'шт', defaultValue: 1, required: false, sortOrder: prev.length + 2 }
          ];
          const newParams = [...prev];
          let added = false;
          needed.forEach(rp => {
            if (!existingKeys.has(rp.paramKey)) {
              newParams.push({ id: Date.now() + Math.random(), ...rp });
              added = true;
            }
          });
          return added ? newParams : prev;
        });
      } else if (field === 'operationType' && value === 'EYELETS') {
        // Настройки для операции "Люверсы"
        setOpForm(prev => ({
          ...prev,
          operationType: value,
          requiresDimensions: false,
          unit: 'шт',
          basePrice: '15',
          wasteCoefficient: '1'
        }));
        // Заменяем параметры на стандартные для люверсов: step, edgeDistance, diameter
        setParameters([
          { id: Date.now(), paramKey: 'step', displayName: 'Шаг установки (мм)', type: 'NUMBER', unit: 'мм', defaultValue: 500, required: false, sortOrder: 0 },
          { id: Date.now() + 1, paramKey: 'edgeDistance', displayName: 'Отступ от края (мм)', type: 'NUMBER', unit: 'мм', defaultValue: 50, required: false, sortOrder: 1 },
          { id: Date.now() + 2, paramKey: 'diameter', displayName: 'Диаметр люверса (мм)', type: 'NUMBER', unit: 'мм', defaultValue: 12, required: false, sortOrder: 2 }
        ]);
      } else {
        setOpForm(prev => ({ ...prev, [field]: value }));
      }
    };

    // Parameters handlers
    const addParameter = () => {
      setParameters(prev => [...prev, {
        id: Date.now(),
        paramKey: '',
        displayName: '',
        type: 'NUMBER',
        unit: '',
        defaultValue: '',
        required: false,
        sortOrder: prev.length
      }]);
    };

    // Auto-generate paramKey from displayName using simple transliteration
    const generateParamKey = (displayName) => {
      if (!displayName) return '';
      return displayName
        .toLowerCase()
        .trim()
        .replace(/\s+/g, '_')           // spaces → underscore
        .replace(/[а-яё]/g, char => {   // Cyrillic → Latin mapping
          const map = {
            'а':'a','б':'b','в':'v','г':'g','д':'d','е':'e','ё':'yo','ж':'zh','з':'z','и':'i','й':'y','к':'k','л':'l','м':'m','н':'n','о':'o','п':'p','р':'r','с':'s','т':'t','у':'u','ф':'f','х':'kh','ц':'ts','ч':'ch','ш':'sh','щ':'shch','ъ':'','ы':'y','ь':'','э':'e','ю':'yu','я':'ya',
            'А':'A','Б':'B','В':'V','Г':'G','Д':'D','Е':'E','Ё':'Yo','Ж':'Zh','З':'Z','И':'I','Й':'Y','К':'K','Л':'L','М':'M','Н':'N','О':'O','П':'P','Р':'R','С':'S','Т':'T','У':'U','Ф':'F','Х':'Kh','Ц':'Ts','Ч':'Ch','Ш':'Sh','Щ':'Shch','Ъ':'','Ы':'Y','Ь':'','Э':'E','Ю':'Yu','Я':'Ya'
          };
          return map[char] || char;
        })
        .replace(/[^a-z0-9_]+/g, '_')   // replace non-alphanumeric with underscore
        .replace(/^_+|_+$/g, '')        // trim underscores
        .replace(/_+/g, '_');           // collapse multiple underscores
    };

    const updateParameter = (index, field, value) => {
      setParameters(prev => prev.map((p, i) => {
        if (i !== index) return p;
        const updated = { ...p, [field]: value };
        // Auto-generate key when displayName changes and key is empty
        if (field === 'displayName' && (!p.paramKey || p.paramKey.trim() === '')) {
          updated.paramKey = generateParamKey(value);
        }
        return updated;
      }));
    };

    const removeParameter = (index) => {
      setParameters(prev => prev.filter((_, i) => i !== index));
    };

    // Additional Materials handlers
    const addOpAdditionalMaterial = () => {
      setAdditionalMaterials(prev => [...prev, {
        id: Date.now(),
        materialId: '',
        defaultQuantity: 1,
        unit: 'шт',
        pricePerUnit: 0
      }]);
    };

    const updateOpAdditionalMaterial = (index, field, value) => {
      setAdditionalMaterials(prev => prev.map((am, i) => {
        if (i !== index) return am;
        const updated = { ...am, [field]: value };
        // Auto-fill price and unit from material if materialId changes
        if (field === 'materialId' && value) {
          const mat = materialsData.find(m => m.id === parseInt(value));
          if (mat) {
            updated.pricePerUnit = parseFloat(mat.price) || 0;
            updated.unit = mat.unit || 'шт';
          }
        }
        return updated;
      }));
    };

    const removeOpAdditionalMaterial = (index) => {
      setAdditionalMaterials(prev => prev.filter((_, i) => i !== index));
    };

    const handleSaveOperation = async () => {
      if (!opForm.name || !opForm.basePrice) {
        showNotification('Заполните название и цену', 'error');
        return;
      }
      if (!selectedMaterial) return;

      // Filter out empty parameters
      const validParams = parameters.filter(p => p.paramKey && p.displayName);

      // Filter out empty additional materials
      const validAddMats = additionalMaterials.filter(am => am.materialId);

       const payload = {
         name: opForm.name,
         description: opForm.description,
         operationType: opForm.operationType,
         basePrice: parseFloat(opForm.basePrice),
         wasteCoefficient: parseFloat(opForm.wasteCoefficient) || 1,
         unit: opForm.unit,
         requiresDimensions: opForm.requiresDimensions,
         allowsAdditionalMaterials: opForm.allowsAdditionalMaterials,
         sortOrder: parseInt(opForm.sortOrder) || 0,
         active: opForm.active,
         parameters: validParams,
         additionalMaterials: validAddMats
       };
       // Установка quantityFormula для операции "Люверсы"
       if (opForm.operationType === 'EYELETS') {
         payload.quantityFormula = 'helper.eyeletCount(step, edgeDistance, quantity)';
       }
       if (editingOpId) {
        updateMaterialOpMutation.mutate({ materialId: selectedMaterial.id, opId: editingOpId, data: payload });
      } else {
        createMaterialOpMutation.mutate({ materialId: selectedMaterial.id, data: payload });
      }
    };

    const handleEditOperation = (op) => {
      const isCutting = op.operationType === 'CUTTING';
      const isEyelets = op.operationType === 'EYELETS';
      setOpForm({
        name: op.name || '',
        description: op.description || '',
        operationType: op.operationType || 'PRINT',
        basePrice: isCutting ? '0' : (op.basePrice ? op.basePrice.toString() : (isEyelets ? '15' : '')),
        wasteCoefficient: isCutting ? '1' : (op.wasteCoefficient ? op.wasteCoefficient.toString() : (isEyelets ? '1' : '')),
        unit: isCutting ? 'шт' : (op.unit || 'шт'),
        requiresDimensions: isCutting || isEyelets ? false : (op.requiresDimensions || false),
        allowsAdditionalMaterials: op.allowsAdditionalMaterials || false,
        sortOrder: op.sortOrder || 0,
        active: op.active !== false
      });
      // Set parameters: для EYELETS — только step, edgeDistance, diameter (сохраняем значения из существующих параметров, если есть)
      if (isEyelets) {
        const oldParams = op.parameters || [];
        const findParam = (key, defDisplayName, defUnit, defVal) => {
          const p = oldParams.find(pp => pp.paramKey === key);
          if (p) {
            return {
              id: p.id || Date.now() + Math.random(),
              paramKey: p.paramKey,
              displayName: p.displayName || defDisplayName,
              type: p.type || 'NUMBER',
              unit: p.unit || defUnit,
              defaultValue: p.defaultValue !== undefined ? p.defaultValue : defVal,
              required: p.required || false,
              sortOrder: p.sortOrder || 0
            };
          } else {
            return {
              id: Date.now() + Math.random(),
              paramKey: key,
              displayName: defDisplayName,
              type: 'NUMBER',
              unit: defUnit,
              defaultValue: defVal,
              required: false,
              sortOrder: 0
            };
          }
        };
        setParameters([
          findParam('step', 'Шаг установки (мм)', 'мм', 500),
          findParam('edgeDistance', 'Отступ от края (мм)', 'мм', 50),
          findParam('diameter', 'Диаметр люверса (мм)', 'мм', 12)
        ]);
      } else if (op.parameters && Array.isArray(op.parameters)) {
        setParameters(op.parameters.map(p => ({
          id: p.id || Date.now() + Math.random(),
          paramKey: p.paramKey || '',
          displayName: p.displayName || '',
          type: p.type || 'NUMBER',
          unit: p.unit || '',
          defaultValue: p.defaultValue || '',
          required: p.required || false,
          sortOrder: p.sortOrder || 0
        })));
      } else {
        setParameters([]);
      }
      // Set additional materials
      if (op.additionalMaterials && Array.isArray(op.additionalMaterials)) {
        setAdditionalMaterials(op.additionalMaterials.map(am => ({
          id: am.id || Date.now() + Math.random(),
          materialId: am.materialId || '',
          defaultQuantity: am.defaultQuantity || 1,
          unit: am.unit || 'шт',
          pricePerUnit: am.pricePerUnit || 0
        })));
      } else {
        setAdditionalMaterials([]);
      }
      setEditingOpId(op.id);
    };

    const handleDeleteOperation = (opId) => {
      if (selectedMaterial && window.confirm('Удалить операцию?')) {
        deleteMaterialOpMutation.mutate({ materialId: selectedMaterial.id, opId });
      }
    };

    const resetOpForm = () => {
      setOpForm({
        name: '',
        description: '',
        operationType: 'PRINT',
        basePrice: '',
        wasteCoefficient: '1',
        unit: 'шт',
        requiresDimensions: false,
        allowsAdditionalMaterials: false,
        sortOrder: 0,
        active: true
      });
      setParameters([]);
      setAdditionalMaterials([]);
      setEditingOpId(null);
    };

    // Mutations for Products
  const createProductMutation = useMutation({
    mutationFn: (product) => api.post('/api/v1/products', product),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-products'] });
      setProductDialogOpen(false);
      showNotification('Продукт создан');
      resetProductForm();
    },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });
  const updateOperationMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/admin/operations/${id}`, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-operations'] }); showNotification('Операция обновлена'); setEditingOperationId(null); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });
  const deleteOperationMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/admin/operations/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-operations'] }); setOperationDeleteDialogOpen(false); showNotification('Операция удалена'); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  // ---- Workshop CRUD ----
  const createWorkshopMutation = useMutation({
    mutationFn: (w) => api.post('/api/v1/workshops', w),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-workshops'] }); setWorkshopDialogOpen(false); showNotification('Цех создан'); resetWorkshopForm(); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });
  const updateWorkshopMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/workshops/${id}`, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-workshops'] }); setWorkshopDialogOpen(false); showNotification('Цех обновлен'); resetWorkshopForm(); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });
  const deleteWorkshopMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/workshops/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-workshops'] }); setWorkshopDeleteDialogOpen(false); showNotification('Цех удален'); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  // ---- Employee CRUD ----
  const createEmployeeMutation = useMutation({
    mutationFn: (e) => api.post('/api/v1/employees', e),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-employees'] }); setEmployeeDialogOpen(false); showNotification('Сотрудник создан'); resetEmployeeForm(); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });
  const updateEmployeeMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/employees/${id}`, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-employees'] }); setEmployeeDialogOpen(false); showNotification('Сотрудник обновлен'); resetEmployeeForm(); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });
  const deleteEmployeeMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/employees/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-employees'] }); setEmployeeDeleteDialogOpen(false); showNotification('Сотрудник удален'); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  const syncKeycloakMutation = useMutation({
    mutationFn: async () => {
      setSyncing(true);
      const r = await api.post('/api/v1/employees/sync-all');
      return r.data;
    },
    onSuccess: (data) => { showNotification(data); queryClient.invalidateQueries({ queryKey: ['admin-employees'] }); },
    onError: (err) => {
      const msg = err.response?.data?.message || err.response?.data || err.message;
      showNotification('Ошибка синхронизации: ' + msg, 'error');
    },
    onSettled: () => setSyncing(false)
  });

  const updateProductMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/products/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-products'] });
      setProductDialogOpen(false);
      showNotification('Продукт обновлен');
      resetProductForm();
    },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  const deleteProductMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/products/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-products'] });
      setProductDeleteDialogOpen(false);
      showNotification('Продукт удален');
    },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  // Generate mutations
  const generateClientsMutation = useMutation({
    mutationFn: () => api.post('/api/v1/admin/clients/generate'),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-clients'] }); showNotification('Сгенерировано 20 клиентов'); setGenerating(p => ({ ...p, clients: false })); },
    onError: (err) => { showNotification('Ошибка: ' + err.message, 'error'); setGenerating(p => ({ ...p, clients: false })); }
  });
  const generateMaterialsMutation = useMutation({
    mutationFn: () => api.post('/api/v1/admin/materials/generate'),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-materials'] }); showNotification('Сгенерировано 20 материалов'); setGenerating(p => ({ ...p, materials: false })); },
    onError: (err) => { showNotification('Ошибка: ' + err.message, 'error'); setGenerating(p => ({ ...p, materials: false })); }
  });
  const generateOrdersMutation = useMutation({
    mutationFn: () => api.post('/api/v1/admin/orders/generate'),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-orders'] }); showNotification('Сгенерировано 20 заказов'); setGenerating(p => ({ ...p, orders: false })); },
    onError: (err) => { showNotification('Ошибка: ' + err.message, 'error'); setGenerating(p => ({ ...p, orders: false })); }
  });

  const handleTabChange = (event, newValue) => {
    setTab(newValue);
  };

  const openClientDialog = (client = null) => {
    if (client) { setSelectedClient(client); setClientForm({ name: client.name || '', type: client.type || 'PRIVATE', contactPerson: client.contactPerson || '', phone: client.phone || '', email: client.email || '' }); } else { resetClientForm(); }
    setClientDialogOpen(true);
  };
  const openMaterialDialog = (mat = null) => {
    if (mat) { setSelectedMaterial(mat); setMaterialForm({ name: mat.name || '', unit: mat.unit || '', price: mat.price ? mat.price.toString() : '', wasteCoefficient: mat.wasteCoefficient ? mat.wasteCoefficient.toString() : '1' }); } else { resetMaterialForm(); }
    setMaterialDialogOpen(true);
  };
  const openOperationDialog = (op = null) => {
    if (op) {
      setSelectedOperation(op);
      setOperationForm({ name: op.name || '', unit: op.unit || 'SQUARE_METER', price: op.price ? op.price.toString() : '', applicableTo: op.applicableTo || 'BANNER', isDefault: op.isDefault || false, hemWidthMm: op.hemWidthMm ? op.hemWidthMm.toString() : '', hemCount: op.hemCount ? op.hemCount.toString() : '' });
    } else { resetOperationForm(); }
    setOperationDialogOpen(true);
  };
  const openWorkshopDialog = (ws = null) => {
    if (ws) {
      setSelectedWorkshop(ws);
      const toArr = (v) => Array.isArray(v) ? v.map(Number) : [];
      setWorkshopForm({ name: ws.name || '', operationIds: toArr(ws.operationIds), materialIds: toArr(ws.materialIds) });
    } else { resetWorkshopForm(); }
    setWorkshopDialogOpen(true);
  };
  const openEmployeeDialog = (emp = null) => {
    if (emp) {
      setSelectedEmployee(emp);
      setEmployeeForm({ fullName: emp.fullName || '', username: emp.username || '', position: emp.position || '', phone: emp.phone || '', email: emp.email || '', workshopId: emp.workshopId || '' });
    } else { resetEmployeeForm(); }
    setEmployeeDialogOpen(true);
  };

  const handleClientSubmit = () => {
    if (!clientForm.name) { showNotification('Введите название', 'error'); return; }
    const payload = { ...clientForm };
    selectedClient ? updateClientMutation.mutate({ id: selectedClient.id, data: payload }) : createClientMutation.mutate(payload);
  };

  const confirmDeleteClient = (client) => {
    setSelectedClient(client);
    setClientDeleteDialogOpen(true);
  };

  const handleDeleteClient = () => {
    if (selectedClient) {
      deleteClientMutation.mutate(selectedClient.id);
    }
  };

   // Material handlers
   const openMaterialDialog = (material = null) => {
     if (material) {
       setSelectedMaterial(material);
       setMaterialForm({
         name: material.name || '',
         unit: material.unit || '',
         price: material.price ? material.price.toString() : '',
         wasteCoefficient: material.wasteCoefficient ? material.wasteCoefficient.toString() : '1'
       });
     } else {
       resetMaterialForm();
     }
     setMaterialTab(0);
     setMaterialDialogOpen(true);
   };

  const resetMaterialForm = () => {
    setMaterialForm({ name: '', unit: '', price: '', wasteCoefficient: '1' });
    setSelectedMaterial(null);
  };

  const handleMaterialSubmit = () => {
    if (!materialForm.name) { showNotification('Введите название', 'error'); return; }
    const payload = { name: materialForm.name, unit: materialForm.unit, price: materialForm.price ? parseFloat(materialForm.price) : 0, wasteCoefficient: parseFloat(materialForm.wasteCoefficient) || 1 };
    selectedMaterial ? updateMaterialMutation.mutate({ id: selectedMaterial.id, data: payload }) : createMaterialMutation.mutate(payload);
  };
  const handleOperationSubmit = () => {
    if (!operationForm.name) { showNotification('Введите название', 'error'); return; }
    const payload = { name: operationForm.name, unit: operationForm.unit, price: operationForm.price ? parseFloat(operationForm.price) : 0, applicableTo: operationForm.applicableTo, isDefault: operationForm.isDefault, hemWidthMm: operationForm.hemWidthMm ? parseInt(operationForm.hemWidthMm) : null, hemCount: operationForm.hemCount ? parseInt(operationForm.hemCount) : null };
    selectedOperation ? updateOperationMutation.mutate({ id: selectedOperation.id, data: payload }) : createOperationMutation.mutate(payload);
  };
  const handleWorkshopSubmit = () => {
    if (!workshopForm.name) { showNotification('Введите название', 'error'); return; }
    const payload = { name: workshopForm.name, operationIds: workshopForm.operationIds.map(Number), materialIds: workshopForm.materialIds.map(Number) };
    selectedWorkshop ? updateWorkshopMutation.mutate({ id: selectedWorkshop.id, data: payload }) : createWorkshopMutation.mutate(payload);
  };
  const handleEmployeeSubmit = () => {
    if (!employeeForm.username) { showNotification('Введите логин', 'error'); return; }
    const payload = { ...employeeForm, workshopId: employeeForm.workshopId ? parseInt(employeeForm.workshopId) : null };
    selectedEmployee ? updateEmployeeMutation.mutate({ id: selectedEmployee.id, data: payload }) : createEmployeeMutation.mutate(payload);
  };

  const startEditMaterial = (mat) => { setEditingMaterialId(mat.id); setEditMaterialForm({ name: mat.name, unit: mat.unit, price: mat.price, wasteCoefficient: mat.wasteCoefficient }); };
  const cancelEditMaterial = () => { setEditingMaterialId(null); setEditMaterialForm({ name: '', unit: '', price: 0, wasteCoefficient: 1 }); };
  const saveMaterialEdit = (id) => { updateMaterialMutation.mutate({ id, data: { name: editMaterialForm.name, unit: editMaterialForm.unit, price: editMaterialForm.price, wasteCoefficient: editMaterialForm.wasteCoefficient } }); setEditingMaterialId(null); };
  const handleEditMaterialChange = (field) => (e) => { const val = field === 'price' || field === 'wasteCoefficient' ? parseFloat(e.target.value) || 0 : e.target.value; setEditMaterialForm(p => ({ ...p, [field]: val })); };

  const startEditOperation = (op) => { setEditingOperationId(op.id); setEditOperationForm({ name: op.name, unit: UNIT_DISPLAY_TO_ENUM[op.unit] || op.unit, price: op.price, applicableTo: op.applicableTo, isDefault: op.isDefault, hemWidthMm: op.hemWidthMm, hemCount: op.hemCount }); };
  const cancelEditOperation = () => { setEditingOperationId(null); setEditOperationForm({ name: '', unit: 'SQUARE_METER', price: 0, applicableTo: 'BANNER', isDefault: false, hemWidthMm: null, hemCount: null }); };
  const saveOperationEdit = (id) => { updateOperationMutation.mutate({ id, data: { name: editOperationForm.name, unit: editOperationForm.unit, price: editOperationForm.price, applicableTo: editOperationForm.applicableTo, isDefault: editOperationForm.isDefault, hemWidthMm: editOperationForm.hemWidthMm, hemCount: editOperationForm.hemCount } }); setEditingOperationId(null); };
  const handleEditOperationChange = (field) => (e) => {
    let val;
    if (field === 'price') val = parseFloat(e.target.value) || 0;
    else if (field === 'isDefault') val = e.target.checked;
    else if (field === 'hemWidthMm' || field === 'hemCount') val = e.target.value ? parseInt(e.target.value) : null;
    else val = e.target.value;
    setEditOperationForm(p => ({ ...p, [field]: val }));
  };

  const handleGenerate = (type) => { setGenerating(p => ({ ...p, [type]: true })); if (type === 'clients') generateClientsMutation.mutate(); else if (type === 'materials') generateMaterialsMutation.mutate(); else if (type === 'orders') generateOrdersMutation.mutate(); };
  const handleOperationFilterChange = (e) => { setOperationFilter(e.target.value); };
  const filteredOperations = operationsData.filter(op => operationFilter === 'ALL' || op.applicableTo === operationFilter);

  // Product handlers
  const openProductDialog = (product = null) => {
    if (product) {
      setSelectedProduct(product);
      setProductForm({
        name: product.name || '',
        article: product.article || '',
        description: product.description || '',
        width: product.width ? product.width.toString() : '',
        height: product.height ? product.height.toString() : '',
        unit: product.unit || 'шт',
        basePrice: product.basePrice ? product.basePrice.toString() : '',
        materials: product.materials ? product.materials.map(m => ({
          id: m.id || Date.now() + Math.random(),
          materialId: m.materialId,
          quantity: m.quantity.toString(),
          wasteCoefficient: m.wasteCoefficient ? m.wasteCoefficient.toString() : '1',
          sortOrder: m.sortOrder || 0
        })) : [],
        operations: product.operations ? product.operations.map(op => ({
          id: op.id || Date.now() + Math.random(),
          name: op.name,
          pricePerUnit: op.pricePerUnit ? op.pricePerUnit.toString() : '',
          normTime: op.normTime || '',
          unit: op.unit || 'шт',
          sortOrder: op.sortOrder || 0
        })) : []
      });
    } else {
      resetProductForm();
    }
    setProductDialogOpen(true);
  };

  const resetProductForm = () => {
    setProductForm({
      name: '',
      article: '',
      description: '',
      width: '',
      height: '',
      unit: 'шт',
      basePrice: '',
      materials: [],
      operations: []
    });
    setSelectedProduct(null);
  };

  const addProductMaterial = () => {
    setProductForm(prev => ({
      ...prev,
      materials: [...prev.materials, {
        id: Date.now() + Math.random(),
        materialId: '',
        quantity: '1',
        wasteCoefficient: '1',
        sortOrder: 0
      }]
    }));
  };

  const updateProductMaterial = (index, field, value) => {
    setProductForm(prev => {
      const newMaterials = [...prev.materials];
      newMaterials[index] = { ...newMaterials[index], [field]: value };
      return { ...prev, materials: newMaterials };
    });
  };

  const removeProductMaterial = (index) => {
    setProductForm(prev => ({
      ...prev,
      materials: prev.materials.filter((_, i) => i !== index)
    }));
  };

  const addProductOperation = () => {
    setProductForm(prev => ({
      ...prev,
      operations: [...prev.operations, {
        id: Date.now() + Math.random(),
        name: '',
        pricePerUnit: '',
        normTime: '',
        unit: 'шт',
        sortOrder: 0
      }]
    }));
  };

  const updateProductOperation = (index, field, value) => {
    setProductForm(prev => {
      const newOps = [...prev.operations];
      newOps[index] = { ...newOps[index], [field]: value };
      return { ...prev, operations: newOps };
    });
  };

  const removeProductOperation = (index) => {
    setProductForm(prev => ({
      ...prev,
      operations: prev.operations.filter((_, i) => i !== index)
    }));
  };

   const handleProductSubmit = () => {
     if (!productForm.name) {
       showNotification('Введите название продукта', 'error');
       return;
     }

     // Filter out invalid materials (no materialId or invalid quantity)
     const validMaterials = productForm.materials
       .filter(m => m.materialId && m.quantity)
       .map(m => ({
         materialId: parseInt(m.materialId),
         quantity: parseFloat(m.quantity),
         wasteCoefficient: parseFloat(m.wasteCoefficient) || 1,
         sortOrder: m.sortOrder
       }));

     // Filter out invalid operations (no name or pricePerUnit)
     const validOperations = productForm.operations
       .filter(op => op.name && op.pricePerUnit)
       .map(op => ({
         name: op.name,
         pricePerUnit: parseFloat(op.pricePerUnit),
         normTime: op.normTime || null,
         unit: op.unit,
         sortOrder: op.sortOrder
       }));

     const payload = {
       name: productForm.name,
       article: productForm.article,
       description: productForm.description,
       width: productForm.width ? parseFloat(productForm.width) : null,
       height: productForm.height ? parseFloat(productForm.height) : null,
       unit: productForm.unit,
       basePrice: productForm.basePrice ? parseFloat(productForm.basePrice) : 0,
       materials: validMaterials,
       operations: validOperations
     };
     if (selectedProduct) {
       updateProductMutation.mutate({ id: selectedProduct.id, data: payload });
     } else {
       createProductMutation.mutate(payload);
     }
   };

  const confirmDeleteProduct = (product) => {
    setSelectedProduct(product);
    setProductDeleteDialogOpen(true);
  };

  const handleDeleteProduct = () => {
    if (selectedProduct) {
      deleteProductMutation.mutate(selectedProduct.id);
    }
  };

  // Generate handlers
  const handleGenerate = (type) => {
    setGenerating(prev => ({ ...prev, [type]: true }));
    switch (type) {
      case 'clients': generateClientsMutation.mutate(); break;
      case 'materials': generateMaterialsMutation.mutate(); break;
      case 'orders': generateOrdersMutation.mutate(); break;
      default: break;
    }
   };

   return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>Admin Panel</Typography>
      <Paper sx={{ width: '100%', mt: 2 }}>
        <Tabs value={tab} onChange={handleTabChange} indicatorColor="secondary" textColor="secondary" variant="scrollable" scrollButtons="auto">
          <Tab label="Clients" />
          <Tab label="Materials" />
          <Tab label="Products" />
          <Tab label="Generate" />
        </Tabs>
        <Divider />
        <Box sx={{ p: 2 }}>
          {tab === 0 && (
            <ClientsTab
              clientsData={clientsData}
              onAddClick={() => openClientDialog()}
              onEditClick={openClientDialog}
              onDeleteClick={confirmDeleteClient}
            />
          )}
          {tab === 1 && (
            <MaterialsTab
              materialsData={materialsData}
              onAddClick={() => openMaterialDialog()}
              onEditClick={openMaterialDialog}
              onDeleteClick={confirmDeleteMaterial}
            />
          )}
          {tab === 2 && (
            <ProductsTab
              productsData={productsData}
              onAddClick={() => openProductDialog()}
              onEditClick={openProductDialog}
              onDeleteClick={confirmDeleteProduct}
            />
          )}
          {tab === 3 && (
            <GenerateTab
              generating={generating}
              onGenerate={handleGenerate}
            />
          )}
        </Box>
      </Paper>

      {/* ---- Dialogs ---- */}

      {/* Client */}
      <Dialog open={clientDialogOpen} onClose={() => setClientDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedClient ? 'Редактировать клиента' : 'Новый клиент'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Название" value={clientForm.name} onChange={(e) => setClientForm({ ...clientForm, name: e.target.value })} />
          <FormControl fullWidth margin="dense"><InputLabel>Тип</InputLabel>
            <Select value={clientForm.type} label="Тип" onChange={(e) => setClientForm({ ...clientForm, type: e.target.value })}><MenuItem value="PRIVATE">Частник</MenuItem><MenuItem value="COMPANY">Компания</MenuItem></Select>
          </FormControl>
          <TextField fullWidth margin="dense" label="Контактное лицо" value={clientForm.contactPerson} onChange={(e) => setClientForm({ ...clientForm, contactPerson: e.target.value })} />
          <TextField fullWidth margin="dense" label="Телефон" value={clientForm.phone} onChange={(e) => setClientForm({ ...clientForm, phone: e.target.value })} />
          <TextField fullWidth margin="dense" label="Email" value={clientForm.email} onChange={(e) => setClientForm({ ...clientForm, email: e.target.value })} />
        </DialogContent>
        <DialogActions><Button onClick={() => setClientDialogOpen(false)}>Отмена</Button><Button onClick={handleClientSubmit} variant="contained">Сохранить</Button></DialogActions>
      </Dialog>
      <Dialog open={clientDeleteDialogOpen} onClose={() => setClientDeleteDialogOpen(false)}>
        <DialogTitle>Удалить клиента?</DialogTitle><DialogContent><Typography>Вы уверены, что хотите удалить "{selectedClient?.name}"?</Typography></DialogContent>
        <DialogActions><Button onClick={() => setClientDeleteDialogOpen(false)}>Отмена</Button><Button onClick={() => selectedClient && deleteClientMutation.mutate(selectedClient.id)} color="error" variant="contained">Удалить</Button></DialogActions>
      </Dialog>

      {/* Material Dialog */}
      <Dialog open={materialDialogOpen} onClose={() => setMaterialDialogOpen(false)} maxWidth="md" fullWidth scroll="paper">
        <DialogTitle>
          <Tabs value={materialTab} onChange={(e, newTab) => setMaterialTab(newTab)} variant="fullWidth">
            <Tab label="Основное" />
            <Tab label="Операции" />
          </Tabs>
        </DialogTitle>
        <DialogContent dividers sx={{ maxHeight: '60vh' }}>
          {materialTab === 0 && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField autoFocus fullWidth label="Название" value={materialForm.name} onChange={(e) => setMaterialForm({ ...materialForm, name: e.target.value })} />
              <FormControl fullWidth>
                <InputLabel>Ед. изм.</InputLabel>
                <Select value={materialForm.unit} label="Ед. изм." onChange={(e) => setMaterialForm({ ...materialForm, unit: e.target.value })}>
                  <MenuItem value="м2">м²</MenuItem>
                  <MenuItem value="м.п.">м.п.</MenuItem>
                  <MenuItem value="шт">шт</MenuItem>
                </Select>
              </FormControl>
              <TextField fullWidth label="Цена" type="number" value={materialForm.price} onChange={(e) => setMaterialForm({ ...materialForm, price: e.target.value })} inputProps={{ step: 0.01 }} />
              <TextField fullWidth label="Коэффициент отхода" type="number" value={materialForm.wasteCoefficient} onChange={(e) => setMaterialForm({ ...materialForm, wasteCoefficient: e.target.value })} inputProps={{ step: 0.1 }} />
            </Box>
          )}
          {materialTab === 1 && (
            <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
              {materialOperations.length === 0 ? (
                <Typography color="text.secondary">Нет операций для этого материала. Добавьте ниже.</Typography>
              ) : (
                <>
                  <Box display="flex" justifyContent="space-between" alignItems="center">
                    <Typography variant="subtitle1">Операции материала</Typography>
                    <Button variant="contained" size="small" startIcon={<Add />} onClick={() => { resetOpForm(); setMaterialTab(1); }}>
                      Добавить операцию
                    </Button>
                  </Box>
                  <TableContainer component={Paper} variant="outlined">
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Тип</TableCell>
                          <TableCell>Название</TableCell>
                          <TableCell>Базовая цена</TableCell>
                          <TableCell>Ед.изм.</TableCell>
                          <TableCell>Размеры</TableCell>
                          <TableCell>Доп. материалы</TableCell>
                          <TableCell align="right">Действия</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {materialOperations.map((op) => (
                          <TableRow key={op.id}>
                            <TableCell>{getOperationTypeLabel(op.operationType)}</TableCell>
                            <TableCell>{op.name}</TableCell>
                            <TableCell>{op.basePrice?.toFixed(2)} ₽</TableCell>
                            <TableCell>{op.unit}</TableCell>
                            <TableCell>{op.requiresDimensions ? 'Да' : 'Нет'}</TableCell>
                            <TableCell>{(op.additionalMaterials || []).length}</TableCell>
                            <TableCell align="right">
                              <IconButton size="small" onClick={() => { handleEditOperation(op); setMaterialTab(1); }}><Edit /></IconButton>
                              <IconButton size="small" color="error" onClick={() => { handleDeleteOperation(op.id); }}><Delete /></IconButton>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </>
              )}

              <Divider />
              <Typography variant="subtitle1" gutterBottom>{editingOpId ? 'Редактировать операцию' : 'Новая операция'}</Typography>
              <Box sx={{ p: 2, border: '1px dashed #ccc', borderRadius: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
                <TextField label="Название операции" fullWidth size="small" value={opForm.name} onChange={(e) => handleOpChange('name', e.target.value)} />
                <TextField label="Описание" fullWidth size="small" multiline rows={2} value={opForm.description || ''} onChange={(e) => handleOpChange('description', e.target.value)} />

                <Box display="flex" gap={2} flexWrap="wrap">
                       <FormControl sx={{ minWidth: 180 }}>
                         <InputLabel>Тип операции</InputLabel>
                         <Select value={opForm.operationType} label="Тип операции" onChange={(e) => handleOpChange('operationType', e.target.value)}>
                           {OPERATION_TYPES.map(type => (
                             <MenuItem key={type.value} value={type.value}>{type.label}</MenuItem>
                           ))}
                         </Select>
                       </FormControl>
                    {opForm.operationType === 'CUTTING' ? (
                      <Box sx={{ width: 250 }}>
                        <Typography variant="caption" display="block" sx={{ fontSize: 10, mb: 0.5 }}>
                          Базовая цена (включена в материал)
                        </Typography>
                        {selectedMaterial?.price ? (
                          <TextField
                            value={selectedMaterial.price.toFixed(2) + ' ₽/м²'}
                            disabled
                            size="small"
                            fullWidth
                          />
                         ) : (
                           <TextField
                             value={`Припуск: ${(parseFloat(parameters.find(p => p.paramKey === 'marginWidth')?.defaultValue) || 50) + (parseFloat(parameters.find(p => p.paramKey === 'marginHeight')?.defaultValue) || 50)} мм`}
                             disabled
                             size="small"
                             fullWidth
                           />
                         )}
                         <input type="hidden" name="basePrice" value="0" />
                       </Box>
                     ) : (
                       <TextField
                         label="Базовая цена"
                         type="number"
                         size="small"
                         value={opForm.basePrice}
                         onChange={(e) => handleOpChange('basePrice', e.target.value)}
                         inputProps={{ step: 0.01 }}
                         sx={{ width: 150 }}
                       />
                     )}
                  <FormControl sx={{ minWidth: 120 }}>
                    <InputLabel>Ед. изм.</InputLabel>
                    <Select value={opForm.unit} label="Ед. изм." onChange={(e) => handleOpChange('unit', e.target.value)}>
                      <MenuItem value="шт">шт</MenuItem>
                      <MenuItem value="м²">м²</MenuItem>
                      <MenuItem value="пог.м">пог.м</MenuItem>
                    </Select>
                  </FormControl>
                </Box>

                 <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
                   {opForm.operationType === 'CUTTING' ? (
                     <Box sx={{ width: 200 }}>
                       <Typography variant="caption" display="block" sx={{ fontSize: 10, mb: 0.5 }}>
                         Коэффициент отходов (не применяется)
                       </Typography>
                       <TextField value="1" disabled size="small" fullWidth />
                     </Box>
                   ) : (
                     <TextField label="Коэффициент отходов" type="number" size="small" value={opForm.wasteCoefficient} onChange={(e) => handleOpChange('wasteCoefficient', e.target.value)} inputProps={{ step: 0.1 }} sx={{ width: 200 }} />
                   )}
                   <FormControlLabel control={<Checkbox checked={opForm.requiresDimensions} onChange={(e) => handleOpChange('requiresDimensions', e.target.checked)} />} label="Требует размеры" />
                   <FormControlLabel control={<Checkbox checked={opForm.allowsAdditionalMaterials} onChange={(e) => handleOpChange('allowsAdditionalMaterials', e.target.checked)} />} label="Доп. материалы" />
                 </Box>

                {/* Parameters Section */}
                <Box>
                  <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="subtitle2">Параметры операции</Typography>
                    <Button size="small" startIcon={<Add />} onClick={addParameter}>Добавить параметр</Button>
                  </Box>
                   {parameters.map((param, idx) => (
                     <Box key={param.id} sx={{ mb: 1, p: 1, border: '1px dashed #ccc', borderRadius: 1 }} display="flex" gap={1} alignItems="center" flexWrap="wrap">
                       {/* Ключ не отображается — генерируется автоматически из названия */}
                       <TextField label="Название" size="small" value={param.displayName} onChange={(e) => updateParameter(idx, 'displayName', e.target.value)} sx={{ minWidth: 180 }} />
                       <FormControl size="small" sx={{ minWidth: 120 }}>
                         <InputLabel>Тип</InputLabel>
                         <Select value={param.type} label="Тип" onChange={(e) => updateParameter(idx, 'type', e.target.value)}>
                           <MenuItem value="NUMBER">Число</MenuItem>
                           <MenuItem value="TEXT">Текст</MenuItem>
                           <MenuItem value="SELECT">Выбор</MenuItem>
                           <MenuItem value="CHECKBOX">Флажок</MenuItem>
                         </Select>
                       </FormControl>
                       <TextField label="Ед.изм." size="small" value={param.unit} onChange={(e) => updateParameter(idx, 'unit', e.target.value)} sx={{ width: 80 }} />
                       <TextField label="По умолч." size="small" value={param.defaultValue} onChange={(e) => updateParameter(idx, 'defaultValue', e.target.value)} sx={{ width: 100 }} />
                       <FormControlLabel control={<Checkbox checked={param.required} onChange={(e) => updateParameter(idx, 'required', e.target.checked)} />} label="Обязательное" />
                       <IconButton size="small" onClick={() => removeParameter(idx)}><Delete /></IconButton>
                     </Box>
                   ))}
                </Box>

                {/* Additional Materials Section */}
                <Box>
                  <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="subtitle2">Дополнительные материалы</Typography>
                    <Button size="small" startIcon={<Add />} onClick={addOpAdditionalMaterial}>Добавить материал</Button>
                  </Box>
                  {additionalMaterials.map((am, idx) => (
                    <Box key={am.id} sx={{ mb: 1, p: 1, border: '1px dashed #ccc', borderRadius: 1 }} display="flex" gap={1} alignItems="center" flexWrap="wrap">
                      <FormControl size="small" sx={{ minWidth: 200 }}>
                        <InputLabel>Материал</InputLabel>
                        <Select value={am.materialId} label="Материал" onChange={(e) => updateOpAdditionalMaterial(idx, 'materialId', e.target.value)}>
                          <MenuItem value="">Выберите</MenuItem>
                          {materialsData.map(m => <MenuItem key={m.id} value={m.id}>{m.name} ({m.unit})</MenuItem>)}
                        </Select>
                      </FormControl>
                      <TextField label="Кол-во" type="number" size="small" value={am.defaultQuantity} onChange={(e) => updateOpAdditionalMaterial(idx, 'defaultQuantity', parseFloat(e.target.value) || 1)} sx={{ width: 100 }} />
                      <TextField label="Ед.изм." size="small" value={am.unit} onChange={(e) => updateOpAdditionalMaterial(idx, 'unit', e.target.value)} sx={{ width: 100 }} />
                      <Typography variant="body2" sx={{ width: 120 }}>Цена: {(am.pricePerUnit || 0).toFixed(2)} ₽</Typography>
                      <IconButton size="small" onClick={() => removeOpAdditionalMaterial(idx)}><Delete /></IconButton>
                    </Box>
                  ))}
                </Box>

                <Box display="flex" gap={2} mt={2}>
                  <Button variant="contained" onClick={handleSaveOperation}>{editingOpId ? 'Обновить операцию' : 'Создать операцию'}</Button>
                  {editingOpId && (
                    <Button variant="outlined" onClick={() => { resetOpForm(); }}>Отмена</Button>
                  )}
                </Box>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          {materialTab === 0 && (
            <>
              <Button onClick={() => setMaterialDialogOpen(false)}>Отмена</Button>
              <Button onClick={handleMaterialSubmit} variant="contained">Сохранить</Button>
            </>
          )}
          {materialTab === 1 && (
            <Button onClick={() => setMaterialDialogOpen(false)}>Закрыть</Button>
          )}
        </DialogActions>
      </Dialog>
      <Dialog open={materialDeleteDialogOpen} onClose={() => setMaterialDeleteDialogOpen(false)}>
        <DialogTitle>Удалить материал?</DialogTitle><DialogContent><Typography>Удалить "{selectedMaterial?.name}"?</Typography></DialogContent>
        <DialogActions><Button onClick={() => setMaterialDeleteDialogOpen(false)}>Отмена</Button><Button onClick={() => selectedMaterial && deleteMaterialMutation.mutate(selectedMaterial.id)} color="error" variant="contained">Удалить</Button></DialogActions>
      </Dialog>

      {/* Product Dialog */}
      <Dialog open={productDialogOpen} onClose={() => setProductDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{selectedProduct ? 'Редактировать продукт' : 'Новый продукт'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Название" value={productForm.name} onChange={(e) => setProductForm({ ...productForm, name: e.target.value })} />
          <TextField fullWidth margin="dense" label="Артикул" value={productForm.article} onChange={(e) => setProductForm({ ...productForm, article: e.target.value })} />
          <TextField fullWidth margin="dense" label="Описание" multiline rows={2} value={productForm.description} onChange={(e) => setProductForm({ ...productForm, description: e.target.value })} />
          <Box display="flex" gap={2} mt={1}>
            <TextField label="Ширина" type="number" value={productForm.width} onChange={(e) => setProductForm({ ...productForm, width: e.target.value })} inputProps={{ step: 0.001 }} sx={{ flex: 1 }} />
            <TextField label="Высота" type="number" value={productForm.height} onChange={(e) => setProductForm({ ...productForm, height: e.target.value })} inputProps={{ step: 0.001 }} sx={{ flex: 1 }} />
          </Box>
          <Box display="flex" gap={2} mt={1}>
            <FormControl sx={{ flex: 1 }}>
              <InputLabel>Ед. изм.</InputLabel>
              <Select value={productForm.unit} label="Ед. изм." onChange={(e) => setProductForm({ ...productForm, unit: e.target.value })}>
                <MenuItem value="шт">шт</MenuItem>
                <MenuItem value="м2">м²</MenuItem>
                <MenuItem value="м.п.">м.п.</MenuItem>
              </Select>
            </FormControl>
            <TextField label="Базовая цена" type="number" value={productForm.basePrice} onChange={(e) => setProductForm({ ...productForm, basePrice: e.target.value })} inputProps={{ step: 0.01 }} sx={{ flex: 1 }} />
          </Box>

          <Divider sx={{ my: 2 }} />
          <Typography variant="subtitle2" gutterBottom>Материалы</Typography>
          {productForm.materials.map((mat, idx) => (
            <Box key={idx} sx={{ mb: 1, p: 1, border: '1px dashed #ccc', borderRadius: 1 }}>
              <Box display="flex" gap={1} alignItems="center">
                <FormControl fullWidth size="small">
                  <Select value={mat.materialId} label="Материал" onChange={(e) => updateProductMaterial(idx, 'materialId', e.target.value)}>
                    <MenuItem value="">Выберите материал</MenuItem>
                    {materialsData.map(m => <MenuItem key={m.id} value={m.id}>{m.name} ({m.unit})</MenuItem>)}
                  </Select>
                </FormControl>
                <TextField label="Кол-во" type="number" size="small" value={mat.quantity} onChange={(e) => updateProductMaterial(idx, 'quantity', e.target.value)} inputProps={{ step: 0.001 }} sx={{ width: 120 }} />
                <TextField label="Отход" type="number" size="small" value={mat.wasteCoefficient} onChange={(e) => updateProductMaterial(idx, 'wasteCoefficient', e.target.value)} inputProps={{ step: 0.1 }} sx={{ width: 100 }} />
                <IconButton size="small" onClick={() => removeProductMaterial(idx)}><Delete /></IconButton>
              </Box>
            </Box>
          ))}
          <Button size="small" startIcon={<Add />} onClick={addProductMaterial}>Добавить материал</Button>

          <Divider sx={{ my: 2 }} />
          <Typography variant="subtitle2" gutterBottom>Операции</Typography>
          {productForm.operations.map((op, idx) => (
            <Box key={idx} sx={{ mb: 1, p: 1, border: '1px dashed #ccc', borderRadius: 1 }}>
              <Box display="flex" gap={1} alignItems="center">
                <TextField label="Название" fullWidth size="small" value={op.name} onChange={(e) => updateProductOperation(idx, 'name', e.target.value)} />
                <TextField label="Цена" type="number" size="small" value={op.pricePerUnit} onChange={(e) => updateProductOperation(idx, 'pricePerUnit', e.target.value)} inputProps={{ step: 0.01 }} sx={{ width: 120 }} />
                <TextField label="Время (ч:мм:сс)" size="small" value={op.normTime} onChange={(e) => updateProductOperation(idx, 'normTime', e.target.value)} sx={{ width: 140 }} />
                <IconButton size="small" onClick={() => removeProductOperation(idx)}><Delete /></IconButton>
              </Box>
            </Box>
          ))}
          <Button size="small" startIcon={<Add />} onClick={addProductOperation}>Добавить операцию</Button>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setProductDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleProductSubmit} variant="contained">Сохранить</Button>
        </DialogActions>
      </Dialog>

      {/* Product Delete Confirmation */}
      <Dialog open={productDeleteDialogOpen} onClose={() => setProductDeleteDialogOpen(false)}>
        <DialogTitle>Удалить продукт?</DialogTitle>
        <DialogContent>
          <Typography>Удалить "{selectedProduct?.name}"? Это также удалит все связанные материалы и операции.</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setProductDeleteDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleDeleteProduct} color="error" variant="contained">Удалить</Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar Notification */}
      <Snackbar open={notification.open} autoHideDuration={3000} onClose={() => setNotification({ ...notification, open: false })} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })} sx={{ width: '100%' }}>{notification.message}</Alert>
      </Snackbar>
    </Box>
  );
};

export default AdminPanel;
