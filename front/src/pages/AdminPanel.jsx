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
  Alert,
  Snackbar,
  Grid,
  Divider,
  TableContainer
} from '@mui/material';
import { Add, Edit, Delete, Refresh } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';

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
  const [materialForm, setMaterialForm] = useState({
    name: '',
    unit: '',
    price: '',
    wasteCoefficient: '1'
  });

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
    const payload = {
      name: productForm.name,
      article: productForm.article,
      description: productForm.description,
      width: productForm.width ? parseFloat(productForm.width) : null,
      height: productForm.height ? parseFloat(productForm.height) : null,
      unit: productForm.unit,
      basePrice: productForm.basePrice ? parseFloat(productForm.basePrice) : 0,
      materials: productForm.materials.map(m => ({
        materialId: parseInt(m.materialId),
        quantity: parseFloat(m.quantity),
        wasteCoefficient: parseFloat(m.wasteCoefficient) || 1,
        sortOrder: m.sortOrder
      })),
      operations: productForm.operations.map(op => ({
        name: op.name,
        pricePerUnit: parseFloat(op.pricePerUnit),
        normTime: op.normTime || null,
        unit: op.unit,
        sortOrder: op.sortOrder
      }))
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

  // Render helpers
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
                <TableCell>Коэф. отход</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {materialsData.map((mat) => (
                <TableRow key={mat.id}>
                  <TableCell>{mat.name}</TableCell>
                  <TableCell>{mat.unit}</TableCell>
                  <TableCell>{mat.price?.toFixed(2)} ₽</TableCell>
                  <TableCell>{mat.wasteCoefficient?.toString()}</TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => openMaterialDialog(mat)}><Edit /></IconButton>
                    <IconButton size="small" color="error" onClick={() => confirmDeleteMaterial(mat)}><Delete /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );

  const renderProductsTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление продуктами (шаблоны изделий)</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => openProductDialog()}>
          Добавить продукт
        </Button>
      </Box>
      {productsData.length === 0 && <Typography>Нет данных</Typography>}
      {productsData.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Название</TableCell>
                <TableCell>Артикул</TableCell>
                <TableCell>Базовая цена</TableCell>
                <TableCell>Материалов</TableCell>
                <TableCell>Операций</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {productsData.map((prod) => (
                <TableRow key={prod.id}>
                  <TableCell>{prod.name}</TableCell>
                  <TableCell>{prod.article || '-'}</TableCell>
                  <TableCell>{(prod.basePrice || 0).toFixed(2)} ₽</TableCell>
                  <TableCell>{prod.materials ? prod.materials.length : 0}</TableCell>
                  <TableCell>{prod.operations ? prod.operations.length : 0}</TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => openProductDialog(prod)}><Edit /></IconButton>
                    <IconButton size="small" color="error" onClick={() => confirmDeleteProduct(prod)}><Delete /></IconButton>
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
          {tab === 0 && renderClientsTab()}
          {tab === 1 && renderMaterialsTab()}
          {tab === 2 && renderProductsTab()}
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
            </Select>
          </FormControl>
          <TextField fullWidth margin="dense" label="Цена" type="number" value={materialForm.price} onChange={(e) => setMaterialForm({ ...materialForm, price: e.target.value })} inputProps={{ step: 0.01 }} />
          <TextField fullWidth margin="dense" label="Коэффициент отхода" type="number" value={materialForm.wasteCoefficient} onChange={(e) => setMaterialForm({ ...materialForm, wasteCoefficient: e.target.value })} inputProps={{ step: 0.1 }} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMaterialDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleMaterialSubmit} variant="contained">Сохранить</Button>
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