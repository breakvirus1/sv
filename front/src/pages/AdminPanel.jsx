import { useState } from 'react';
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
   CircularProgress,
   Alert,
   Snackbar,
   Grid,
   Divider,
   TableContainer,
   Checkbox
 } from '@mui/material';
import { Add, Edit, Delete, Refresh, Save, Cancel } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';

// Map display names from OperationDto to UnitType enum values
const UNIT_DISPLAY_TO_ENUM = {
  'м²': 'SQUARE_METER',
  'п.м.': 'LINEAR_METER',
  'шт': 'PIECE'
};

// Entity types
const ENTITY_TABS = [
  { label: 'Clients', value: 'clients' },
  { label: 'Materials', value: 'materials' },
  { label: 'Operations', value: 'operations' },
  { label: 'Generate Data', value: 'generate' }
];

const AdminPanel = () => {
  const [tab, setTab] = useState(0);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Notification state
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
  const showNotification = (message, severity = 'success') => {
    setNotification({ open: true, message, severity });
  };

  // ---- Clients state ----
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

  // ---- Materials state ----
  const [materialDialogOpen, setMaterialDialogOpen] = useState(false);
  const [materialDeleteDialogOpen, setMaterialDeleteDialogOpen] = useState(false);
  const [selectedMaterial, setSelectedMaterial] = useState(null);
  const [materialForm, setMaterialForm] = useState({
    name: '',
    unit: '',
    price: '',
    wasteCoefficient: '1'
  });
  // Inline editing state for materials table
  const [editingMaterialId, setEditingMaterialId] = useState(null);
  const [editMaterialForm, setEditMaterialForm] = useState({
    name: '',
    unit: '',
    price: 0,
    wasteCoefficient: 1
  });

   // ---- Operations state ----
   const [operationFilter, setOperationFilter] = useState('ALL'); // ALL, BANNER, PLENKA
   const [operationDialogOpen, setOperationDialogOpen] = useState(false);
   const [operationDeleteDialogOpen, setOperationDeleteDialogOpen] = useState(false);
   const [selectedOperation, setSelectedOperation] = useState(null);
   const [operationForm, setOperationForm] = useState({
     name: '',
     unit: 'SQUARE_METER',
     price: '',
     applicableTo: 'BANNER',
     isDefault: false
   });
   // Inline editing state for operations table
   const [editingOperationId, setEditingOperationId] = useState(null);
   const [editOperationForm, setEditOperationForm] = useState({
     name: '',
     unit: 'SQUARE_METER',
     price: 0,
     applicableTo: 'BANNER',
     isDefault: false
   });

  // ---- Generation state ----
  const [generating, setGenerating] = useState({ clients: false, materials: false, orders: false });

  // Fetch data functions (enabled based on tab)
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

  const { data: operationsData = [], refetch: refetchOperations } = useQuery({
    queryKey: ['admin-operations'],
    queryFn: async () => {
      const response = await api.get('/api/v1/calculations/operations');
      return response.data || [];
    },
    enabled: tab === 2
  });

  // ---- CRUD operations for Clients ----
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

  // ---- Materials ----
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
      showNotification('Материал обновлен');
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

   // ---- Operations ----
   const createOperationMutation = useMutation({
     mutationFn: (op) => api.post('/api/v1/admin/operations', op),
     onSuccess: () => {
       queryClient.invalidateQueries({ queryKey: ['admin-operations'] });
       setOperationDialogOpen(false);
       showNotification('Операция создана');
       resetOperationForm();
     },
     onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
   });

   const updateOperationMutation = useMutation({
     mutationFn: ({ id, data }) => api.put(`/api/v1/admin/operations/${id}`, data),
     onSuccess: () => {
       queryClient.invalidateQueries({ queryKey: ['admin-operations'] });
       showNotification('Операция обновлена');
       setEditingOperationId(null);
     },
     onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
   });

   const deleteOperationMutation = useMutation({
     mutationFn: (id) => api.delete(`/api/v1/admin/operations/${id}`),
     onSuccess: () => {
       queryClient.invalidateQueries({ queryKey: ['admin-operations'] });
       setOperationDeleteDialogOpen(false);
       showNotification('Операция удалена');
     },
     onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
   });

   // ---- Generate mutations ----
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

  // Handlers
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

  // Inline material editing handlers
  const startEditMaterial = (mat) => {
    setEditingMaterialId(mat.id);
    setEditMaterialForm({
      name: mat.name,
      unit: mat.unit,
      price: mat.price,
      wasteCoefficient: mat.wasteCoefficient
    });
  };

  const cancelEditMaterial = () => {
    setEditingMaterialId(null);
    setEditMaterialForm({ name: '', unit: '', price: 0, wasteCoefficient: 1 });
  };

  const saveMaterialEdit = (id) => {
    const payload = {
      name: editMaterialForm.name,
      unit: editMaterialForm.unit,
      price: editMaterialForm.price,
      wasteCoefficient: editMaterialForm.wasteCoefficient
    };
    updateMaterialMutation.mutate({ id, data: payload });
    setEditingMaterialId(null);
    setEditMaterialForm({ name: '', unit: '', price: 0, wasteCoefficient: 1 });
  };

   const handleEditMaterialChange = (field) => (e) => {
     const val = field === 'price' || field === 'wasteCoefficient' ? parseFloat(e.target.value) || 0 : e.target.value;
     setEditMaterialForm(prev => ({ ...prev, [field]: val }));
   };

   // Operation handlers
   const openOperationDialog = (operation = null) => {
     if (operation) {
       setSelectedOperation(operation);
       setOperationForm({
         name: operation.name || '',
         unit: operation.unit || 'SQUARE_METER',
         price: operation.price ? operation.price.toString() : '',
         applicableTo: operation.applicableTo || 'BANNER',
         isDefault: operation.isDefault || false
       });
     } else {
       resetOperationForm();
     }
     setOperationDialogOpen(true);
   };

   const resetOperationForm = () => {
     setOperationForm({ name: '', unit: 'SQUARE_METER', price: '', applicableTo: 'BANNER', isDefault: false });
     setSelectedOperation(null);
   };

   const handleOperationSubmit = () => {
     if (!operationForm.name) {
       showNotification('Введите название', 'error');
       return;
     }
     const payload = {
       name: operationForm.name,
       unit: operationForm.unit,
       price: operationForm.price ? parseFloat(operationForm.price) : 0,
       applicableTo: operationForm.applicableTo,
       isDefault: operationForm.isDefault
     };
     if (selectedOperation) {
       updateOperationMutation.mutate({ id: selectedOperation.id, data: payload });
     } else {
       createOperationMutation.mutate(payload);
     }
   };

   const confirmDeleteOperation = (operation) => {
     setSelectedOperation(operation);
     setOperationDeleteDialogOpen(true);
   };

   const handleDeleteOperation = () => {
     if (selectedOperation) {
       deleteOperationMutation.mutate(selectedOperation.id);
     }
   };

   // Inline operation editing handlers
   const startEditOperation = (op) => {
     setEditingOperationId(op.id);
     setEditOperationForm({
       name: op.name,
       unit: UNIT_DISPLAY_TO_ENUM[op.unit] || op.unit,
       price: op.price,
       applicableTo: op.applicableTo,
       isDefault: op.isDefault
     });
   };

   const cancelEditOperation = () => {
     setEditingOperationId(null);
     setEditOperationForm({ name: '', unit: 'SQUARE_METER', price: 0, applicableTo: 'BANNER', isDefault: false });
   };

   const saveOperationEdit = (id) => {
     const payload = {
       name: editOperationForm.name,
       unit: editOperationForm.unit,
       price: editOperationForm.price,
       applicableTo: editOperationForm.applicableTo,
       isDefault: editOperationForm.isDefault
     };
     updateOperationMutation.mutate({ id, data: payload });
     setEditingOperationId(null);
   };

   const handleEditOperationChange = (field) => (e) => {
     let val;
     if (field === 'price') {
       val = parseFloat(e.target.value) || 0;
     } else if (field === 'isDefault') {
       val = e.target.checked;
     } else {
       val = e.target.value;
     }
     setEditOperationForm(prev => ({ ...prev, [field]: val }));
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

  // Operation filter handler
  const handleOperationFilterChange = (event) => {
    setOperationFilter(event.target.value);
  };

  // Derive filtered operations list
  const filteredOperations = operationsData.filter(op => {
    if (operationFilter === 'ALL') return true;
    return op.applicableTo === operationFilter;
  });

  // ---- Render helpers ----
  const renderClientsTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление клиентами</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => openClientDialog()}>
          Добавить клиента
        </Button>
      </Box>
      {clientsData.length === 0 && <Typography>Нет данных</Typography>}
      {clientsData.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Имя</TableCell>
                <TableCell>Тип</TableCell>
                <TableCell>Контактное лицо</TableCell>
                <TableCell>Телефон</TableCell>
                <TableCell>Email</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {clientsData.map((client) => (
                <TableRow key={client.id}>
                  <TableCell>{client.name}</TableCell>
                  <TableCell>{client.type}</TableCell>
                  <TableCell>{client.contactPerson || '-'}</TableCell>
                  <TableCell>{client.phone || '-'}</TableCell>
                  <TableCell>{client.email || '-'}</TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => openClientDialog(client)}><Edit /></IconButton>
                    <IconButton size="small" color="error" onClick={() => confirmDeleteClient(client)}><Delete /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );

  const renderMaterialsTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление материалами</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => openMaterialDialog()}>
          Добавить материал
        </Button>
      </Box>
      {materialsData.length === 0 && <Typography>Нет данных</Typography>}
      {materialsData.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Название</TableCell>
                <TableCell>Ед. изм.</TableCell>
                <TableCell>Цена</TableCell>
                <TableCell>Коэффициент отхода</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {materialsData.map((mat) => (
                <TableRow key={mat.id}>
                  <TableCell>
                    {editingMaterialId === mat.id ? (
                      <TextField fullWidth size="small" value={editMaterialForm.name} onChange={handleEditMaterialChange('name')} />
                    ) : (
                      mat.name
                    )}
                  </TableCell>
                  <TableCell>
                    {editingMaterialId === mat.id ? (
                      <FormControl fullWidth size="small">
                        <Select value={editMaterialForm.unit} onChange={(e) => handleEditMaterialChange('unit')(e)}>
                          <MenuItem value="м2">м²</MenuItem>
                          <MenuItem value="м.п.">м.п.</MenuItem>
                          <MenuItem value="шт">шт</MenuItem>
                        </Select>
                      </FormControl>
                    ) : (
                      mat.unit
                    )}
                  </TableCell>
                  <TableCell>
                    {editingMaterialId === mat.id ? (
                      <TextField fullWidth size="small" type="number" value={editMaterialForm.price} onChange={handleEditMaterialChange('price')} inputProps={{ step: 0.01 }} />
                    ) : (
                      `${mat.price?.toFixed(2)} ₽`
                    )}
                  </TableCell>
                  <TableCell>
                    {editingMaterialId === mat.id ? (
                      <TextField fullWidth size="small" type="number" value={editMaterialForm.wasteCoefficient} onChange={handleEditMaterialChange('wasteCoefficient')} inputProps={{ step: 0.1 }} />
                    ) : (
                      mat.wasteCoefficient?.toString()
                    )}
                  </TableCell>
                  <TableCell align="right">
                    {editingMaterialId === mat.id ? (
                      <>
                        <IconButton size="small" color="success" onClick={() => saveMaterialEdit(mat.id)}>
                          <Save />
                        </IconButton>
                        <IconButton size="small" onClick={cancelEditMaterial}>
                          <Cancel />
                        </IconButton>
                      </>
                    ) : (
                      <IconButton size="small" onClick={() => startEditMaterial(mat)}>
                        <Edit />
                      </IconButton>
                    )}
                    <IconButton size="small" color="error" onClick={() => confirmDeleteMaterial(mat)}>
                      <Delete />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );

   const renderGenerateTab = () => (
     <Box>
       <Typography variant="h6" gutterBottom>Генерация тестовых данных</Typography>
       <Grid container spacing={2}>
         <Grid item xs={12} sm={6} md={3}>
             <Button
               variant="contained"
               fullWidth
               startIcon={<Refresh />}
               onClick={() => handleGenerate('clients')}
               disabled={generating.clients}
             >
               {generating.clients ? 'Генерация...' : 'Сгенерировать клиентов (20)'}
             </Button>
         </Grid>
         <Grid item xs={12} sm={6} md={3}>
           <Button
             variant="contained"
             fullWidth
             startIcon={<Refresh />}
             onClick={() => handleGenerate('materials')}
             disabled={generating.materials}
           >
             {generating.materials ? 'Генерация...' : 'Сгенерировать материалы (20)'}
           </Button>
         </Grid>
         <Grid item xs={12} sm={6} md={3}>
           <Button
             variant="contained"
             fullWidth
             startIcon={<Refresh />}
             onClick={() => handleGenerate('orders')}
             disabled={generating.orders}
           >
             {generating.orders ? 'Генерация...' : 'Сгенерировать заказы (20)'}
           </Button>
         </Grid>
       </Grid>
       <Alert severity="info" sx={{ mt: 3 }}>
         При генерации заказов необходимо иметь хотя бы одного клиента и сотрудника. Рекомендуется сначала сгенерировать их.
       </Alert>
     </Box>
   );

   const renderOperationsTab = () => (
     <Box>
       <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
         <Typography variant="h6">Управление операциями</Typography>
         <Box display="flex" gap={2} alignItems="center">
           <FormControl size="small" sx={{ minWidth: 180 }}>
             <InputLabel>Фильтр по типу</InputLabel>
             <Select value={operationFilter} label="Фильтр по типу" onChange={handleOperationFilterChange}>
               <MenuItem value="ALL">Все</MenuItem>
               <MenuItem value="BANNER">Баннер</MenuItem>
               <MenuItem value="PLENKA">Пленка</MenuItem>
               <MenuItem value="BOTH">Оба</MenuItem>
             </Select>
           </FormControl>
           <Button variant="outlined" startIcon={<Refresh />} onClick={() => refetchOperations()}>
             Обновить
           </Button>
           <Button variant="contained" startIcon={<Add />} onClick={() => openOperationDialog()}>
             Добавить операцию
           </Button>
         </Box>
       </Box>
       {filteredOperations.length === 0 && <Typography>Нет операций</Typography>}
       {filteredOperations.length > 0 && (
         <TableContainer component={Paper}>
           <Table size="small">
             <TableHead>
               <TableRow>
                 <TableCell>Название</TableCell>
                 <TableCell>Цена</TableCell>
                 <TableCell>Ед. изм.</TableCell>
                 <TableCell>Применимо к</TableCell>
                 <TableCell>По умолчанию</TableCell>
                 <TableCell align="right">Действия</TableCell>
               </TableRow>
             </TableHead>
             <TableBody>
               {filteredOperations.map((op) => (
                 <TableRow key={op.id}>
                   <TableCell>
                     {editingOperationId === op.id ? (
                       <TextField fullWidth size="small" value={editOperationForm.name} onChange={handleEditOperationChange('name')} />
                     ) : (
                       op.name
                     )}
                   </TableCell>
                   <TableCell>
                     {editingOperationId === op.id ? (
                       <TextField fullWidth size="small" type="number" value={editOperationForm.price} onChange={handleEditOperationChange('price')} inputProps={{ step: 0.01 }} />
                     ) : (
                       `${op.price?.toFixed(2)} ₽`
                     )}
                   </TableCell>
                   <TableCell>
                     {editingOperationId === op.id ? (
                       <FormControl fullWidth size="small">
                         <Select value={editOperationForm.unit} onChange={(e) => handleEditOperationChange('unit')(e)}>
                           <MenuItem value="SQUARE_METER">м²</MenuItem>
                           <MenuItem value="LINEAR_METER">п.м.</MenuItem>
                           <MenuItem value="PIECE">шт</MenuItem>
                         </Select>
                       </FormControl>
                     ) : (
                       op.unit
                     )}
                   </TableCell>
                   <TableCell>
                     {editingOperationId === op.id ? (
                       <FormControl fullWidth size="small">
                         <Select value={editOperationForm.applicableTo} onChange={(e) => handleEditOperationChange('applicableTo')(e)}>
                           <MenuItem value="BANNER">Баннер</MenuItem>
                           <MenuItem value="PLENKA">Пленка</MenuItem>
                           <MenuItem value="BOTH">Оба</MenuItem>
                         </Select>
                       </FormControl>
                     ) : (
                       op.applicableTo === 'BANNER' ? 'Баннер' :
                       op.applicableTo === 'PLENKA' ? 'Пленка' : 'Оба'
                     )}
                   </TableCell>
                   <TableCell>
                     {editingOperationId === op.id ? (
                       <Checkbox checked={editOperationForm.isDefault} onChange={handleEditOperationChange('isDefault')} />
                     ) : (
                       op.isDefault ? 'Да' : 'Нет'
                     )}
                   </TableCell>
                   <TableCell align="right">
                     {editingOperationId === op.id ? (
                       <>
                         <IconButton size="small" color="success" onClick={() => saveOperationEdit(op.id)}>
                           <Save />
                         </IconButton>
                         <IconButton size="small" onClick={cancelEditOperation}>
                           <Cancel />
                         </IconButton>
                       </>
                     ) : (
                       <IconButton size="small" onClick={() => startEditOperation(op)}>
                         <Edit />
                       </IconButton>
                     )}
                     <IconButton size="small" color="error" onClick={() => confirmDeleteOperation(op)}>
                       <Delete />
                     </IconButton>
                   </TableCell>
                 </TableRow>
               ))}
             </TableBody>
           </Table>
         </TableContainer>
       )}
     </Box>
   );

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Admin Panel
      </Typography>
      <Paper sx={{ width: '100%', mt: 2 }}>
        <Tabs value={tab} onChange={handleTabChange} indicatorColor="secondary" textColor="secondary" variant="scrollable" scrollButtons="auto">
          {ENTITY_TABS.map((t, idx) => (
            <Tab key={t.value} label={t.label} />
          ))}
        </Tabs>
        <Divider />
         <Box sx={{ p: 2 }}>
           {tab === 0 && renderClientsTab()}
           {tab === 1 && renderMaterialsTab()}
           {tab === 2 && renderOperationsTab()}
           {tab === 3 && renderGenerateTab()}
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
      <Dialog open={materialDialogOpen} onClose={() => setMaterialDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedMaterial ? 'Редактировать материал' : 'Новый материал'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Название" value={materialForm.name} onChange={(e) => setMaterialForm({ ...materialForm, name: e.target.value })} />
          <FormControl fullWidth margin="dense">
            <InputLabel>Ед. изм.</InputLabel>
            <Select value={materialForm.unit} label="Ед. изм." onChange={(e) => setMaterialForm({ ...materialForm, unit: e.target.value })}>
              <MenuItem value="м2">м²</MenuItem>
              <MenuItem value="м.п.">м.п.</MenuItem>
              <MenuItem value="шт">шт</MenuItem>
            </Select>
          </FormControl>
          <TextField fullWidth margin="dense" label="Цена" type="number" value={materialForm.price} onChange={(e) => setMaterialForm({ ...materialForm, price: e.target.value })} inputProps={{ step: 0.01 }} />
          <TextField fullWidth margin="dense" label="Коэффициент отхода" type="number" value={materialForm.wasteCoefficient} onChange={(e) => setMaterialForm({ ...materialForm, wasteCoefficient: e.target.value })} inputProps={{ step: 0.1 }} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMaterialDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleMaterialSubmit} variant="contained" disabled={createMaterialMutation.isLoading}>
            {createMaterialMutation.isLoading ? <CircularProgress size={24} /> : 'Сохранить'}
          </Button>
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

      {/* Operation Dialog */}
      <Dialog open={operationDialogOpen} onClose={() => setOperationDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedOperation ? 'Редактировать операцию' : 'Новая операция'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Название" value={operationForm.name} onChange={(e) => setOperationForm({ ...operationForm, name: e.target.value })} />
          <FormControl fullWidth margin="dense">
            <InputLabel>Ед. изм.</InputLabel>
            <Select value={operationForm.unit} label="Ед. изм." onChange={(e) => setOperationForm({ ...operationForm, unit: e.target.value })}>
              <MenuItem value="SQUARE_METER">м²</MenuItem>
              <MenuItem value="LINEAR_METER">п.м.</MenuItem>
              <MenuItem value="PIECE">шт</MenuItem>
            </Select>
          </FormControl>
          <TextField fullWidth margin="dense" label="Цена" type="number" value={operationForm.price} onChange={(e) => setOperationForm({ ...operationForm, price: e.target.value })} inputProps={{ step: 0.01 }} />
          <FormControl fullWidth margin="dense">
            <InputLabel>Применимо к</InputLabel>
            <Select value={operationForm.applicableTo} label="Применимо к" onChange={(e) => setOperationForm({ ...operationForm, applicableTo: e.target.value })}>
              <MenuItem value="BANNER">Баннер</MenuItem>
              <MenuItem value="PLENKA">Пленка</MenuItem>
              <MenuItem value="BOTH">Оба</MenuItem>
            </Select>
          </FormControl>
          <Box display="flex" alignItems="center" mt={1}>
            <Checkbox checked={operationForm.isDefault} onChange={(e) => setOperationForm({ ...operationForm, isDefault: e.target.checked })} />
            <Typography>По умолчанию</Typography>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOperationDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleOperationSubmit} variant="contained" disabled={createOperationMutation.isLoading}>
            {createOperationMutation.isLoading ? <CircularProgress size={24} /> : 'Сохранить'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Operation Delete Confirmation */}
      <Dialog open={operationDeleteDialogOpen} onClose={() => setOperationDeleteDialogOpen(false)}>
        <DialogTitle>Удалить операцию?</DialogTitle>
        <DialogContent>
          <Typography>Удалить "{selectedOperation?.name}"?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOperationDeleteDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleDeleteOperation} color="error" variant="contained">Удалить</Button>
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
