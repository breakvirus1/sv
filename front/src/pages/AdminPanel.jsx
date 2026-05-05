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
import { Add, Edit, Delete, Refresh } from '@mui/icons-material';
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
  const [clientForm, setClientForm] = useState({
    name: '',
    type: 'PRIVATE',
    contactPerson: '',
    phone: '',
    email: ''
  });

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
    queryFn: async () => {
      const response = await api.get('/api/v1/clients?size=100');
      return response.data.content || [];
    },
    enabled: tab === 0
  });

  const { data: materialsData = [], refetch: refetchMaterials } = useQuery({
    queryKey: ['admin-materials'],
    queryFn: async () => {
      const response = await api.get('/api/v1/materials?size=100');
      return response.data.content || [];
    },
    enabled: tab === 1
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
    mutationFn: (client) => api.post('/api/v1/clients', client),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-clients'] });
      setClientDialogOpen(false);
      showNotification('Клиент создан');
      resetClientForm();
    },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  const updateClientMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/clients/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-clients'] });
      setClientDialogOpen(false);
      showNotification('Клиент обновлен');
      resetClientForm();
    },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  const deleteClientMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/clients/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-clients'] });
      setClientDeleteDialogOpen(false);
      showNotification('Клиент удален');
    },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  // Mutations for Materials
  const createMaterialMutation = useMutation({
    mutationFn: (material) => api.post('/api/v1/materials', material),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-materials'] });
      setMaterialDialogOpen(false);
      showNotification('Материал создан');
      resetMaterialForm();
    },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  const updateMaterialMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/materials/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-materials'] });
      setMaterialDialogOpen(false);
      showNotification('Материал обновлен');
      resetMaterialForm();
    },
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
      setOpForm(prev => ({ ...prev, [field]: value }));
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

    const updateParameter = (index, field, value) => {
      setParameters(prev => prev.map((p, i) => i === index ? { ...p, [field]: value } : p));
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
      if (editingOpId) {
        updateMaterialOpMutation.mutate({ materialId: selectedMaterial.id, opId: editingOpId, data: payload });
      } else {
        createMaterialOpMutation.mutate({ materialId: selectedMaterial.id, data: payload });
      }
    };

    const handleEditOperation = (op) => {
      setOpForm({
        name: op.name || '',
        description: op.description || '',
        operationType: op.operationType || 'PRINT',
        basePrice: op.basePrice ? op.basePrice.toString() : '',
        wasteCoefficient: op.wasteCoefficient ? op.wasteCoefficient.toString() : '1',
        unit: op.unit || 'шт',
        requiresDimensions: op.requiresDimensions || false,
        allowsAdditionalMaterials: op.allowsAdditionalMaterials || false,
        sortOrder: op.sortOrder || 0,
        active: op.active !== false
      });
      // Set parameters
      if (op.parameters && Array.isArray(op.parameters)) {
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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-clients'] });
      showNotification('Сгенерировано 20 клиентов');
      setGenerating(prev => ({ ...prev, clients: false }));
    },
    onError: (err) => {
      showNotification('Ошибка: ' + err.message, 'error');
      setGenerating(prev => ({ ...prev, clients: false }));
    }
  });

  const generateMaterialsMutation = useMutation({
    mutationFn: () => api.post('/api/v1/admin/materials/generate'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-materials'] });
      showNotification('Сгенерировано 20 материалов');
      setGenerating(prev => ({ ...prev, materials: false }));
    },
    onError: (err) => {
      showNotification('Ошибка: ' + err.message, 'error');
      setGenerating(prev => ({ ...prev, materials: false }));
    }
  });

  const generateOrdersMutation = useMutation({
    mutationFn: () => api.post('/api/v1/admin/orders/generate'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-orders'] });
      showNotification('Сгенерировано 20 заказов');
      setGenerating(prev => ({ ...prev, orders: false }));
    },
    onError: (err) => {
      showNotification('Ошибка: ' + err.message, 'error');
      setGenerating(prev => ({ ...prev, orders: false }));
    }
  });

  const handleTabChange = (event, newValue) => {
    setTab(newValue);
  };

  // Client handlers
  const openClientDialog = (client = null) => {
    if (client) {
      setSelectedClient(client);
      setClientForm({
        name: client.name || '',
        type: client.type || 'PRIVATE',
        contactPerson: client.contactPerson || '',
        phone: client.phone || '',
        email: client.email || ''
      });
    } else {
      resetClientForm();
    }
    setClientDialogOpen(true);
  };

  const resetClientForm = () => {
    setClientForm({ name: '', type: 'PRIVATE', contactPerson: '', phone: '', email: '' });
    setSelectedClient(null);
  };

  const handleClientSubmit = () => {
    if (!clientForm.name) {
      showNotification('Введите название', 'error');
      return;
    }
    const payload = { ...clientForm, type: clientForm.type };
    if (selectedClient) {
      updateClientMutation.mutate({ id: selectedClient.id, data: payload });
    } else {
      createClientMutation.mutate(payload);
    }
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
    if (!materialForm.name) {
      showNotification('Введите название', 'error');
      return;
    }
    const payload = {
      name: materialForm.name,
      unit: materialForm.unit,
      price: materialForm.price ? parseFloat(materialForm.price) : 0,
      wasteCoefficient: parseFloat(materialForm.wasteCoefficient) || 1
    };
    if (selectedMaterial) {
      updateMaterialMutation.mutate({ id: selectedMaterial.id, data: payload });
    } else {
      createMaterialMutation.mutate(payload);
    }
  };

  const confirmDeleteMaterial = (material) => {
    setSelectedMaterial(material);
    setMaterialDeleteDialogOpen(true);
  };

  const handleDeleteMaterial = () => {
    if (selectedMaterial) {
      deleteMaterialMutation.mutate(selectedMaterial.id);
    }
  };

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

      {/* Client Dialog */}
      <Dialog open={clientDialogOpen} onClose={() => setClientDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedClient ? 'Редактировать клиента' : 'Новый клиент'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Название" value={clientForm.name} onChange={(e) => setClientForm({ ...clientForm, name: e.target.value })} />
          <FormControl fullWidth margin="dense">
            <InputLabel>Тип</InputLabel>
            <Select value={clientForm.type} label="Тип" onChange={(e) => setClientForm({ ...clientForm, type: e.target.value })}>
              <MenuItem value="PRIVATE">Частник</MenuItem>
              <MenuItem value="COMPANY">Компания</MenuItem>
            </Select>
          </FormControl>
          <TextField fullWidth margin="dense" label="Контактное лицо" value={clientForm.contactPerson} onChange={(e) => setClientForm({ ...clientForm, contactPerson: e.target.value })} />
          <TextField fullWidth margin="dense" label="Телефон" value={clientForm.phone} onChange={(e) => setClientForm({ ...clientForm, phone: e.target.value })} />
          <TextField fullWidth margin="dense" label="Email" value={clientForm.email} onChange={(e) => setClientForm({ ...clientForm, email: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setClientDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleClientSubmit} variant="contained">Сохранить</Button>
        </DialogActions>
      </Dialog>

      {/* Client Delete Confirmation */}
      <Dialog open={clientDeleteDialogOpen} onClose={() => setClientDeleteDialogOpen(false)}>
        <DialogTitle>Удалить клиента?</DialogTitle>
        <DialogContent>
          <Typography>Вы уверены, что хотите удалить "{selectedClient?.name}"?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setClientDeleteDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleDeleteClient} color="error" variant="contained">Удалить</Button>
        </DialogActions>
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
                  <TextField label="Базовая цена" type="number" size="small" value={opForm.basePrice} onChange={(e) => handleOpChange('basePrice', e.target.value)} inputProps={{ step: 0.01 }} sx={{ width: 150 }} />
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
                  <TextField label="Коэффициент отходов" type="number" size="small" value={opForm.wasteCoefficient} onChange={(e) => handleOpChange('wasteCoefficient', e.target.value)} inputProps={{ step: 0.1 }} sx={{ width: 200 }} />
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
                      <TextField label="Ключ" size="small" value={param.paramKey} onChange={(e) => updateParameter(idx, 'paramKey', e.target.value)} sx={{ width: 120 }} />
                      <TextField label="Название" size="small" value={param.displayName} onChange={(e) => updateParameter(idx, 'displayName', e.target.value)} sx={{ width: 150 }} />
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

      {/* Material Delete Confirmation */}
      <Dialog open={materialDeleteDialogOpen} onClose={() => setMaterialDeleteDialogOpen(false)}>
        <DialogTitle>Удалить материал?</DialogTitle>
        <DialogContent>
          <Typography>Удалить "{selectedMaterial?.name}"?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMaterialDeleteDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleDeleteMaterial} color="error" variant="contained">Удалить</Button>
        </DialogActions>
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
        <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })} sx={{ width: '100%' }}>
          {notification.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default AdminPanel;