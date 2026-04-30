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
  TableContainer
} from '@mui/material';
import { Add, Edit, Delete, Refresh } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';

// Entity types
const ENTITY_TABS = [
  { label: 'Clients', value: 'clients' },
  { label: 'Materials', value: 'materials' },
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
          {tab === 2 && renderGenerateTab()}
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