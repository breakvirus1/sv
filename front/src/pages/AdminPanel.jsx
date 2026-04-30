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
  Chip,
  Divider,
  Tooltip,
  TableContainer
} from '@mui/material';
import { Add, Edit, Delete, Refresh, Visibility } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';

// Entity types
const ENTITY_TABS = [
  { label: 'Clients', value: 'clients' },
  { label: 'Materials', value: 'materials' },
  { label: 'Employees', value: 'employees' },
  { label: 'Orders', value: 'orders' },
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
    email: '',
    inn: '',
    address: ''
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

  // ---- Employees state ----
  const [employeeDialogOpen, setEmployeeDialogOpen] = useState(false);
  const [employeeDeleteDialogOpen, setEmployeeDeleteDialogOpen] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [employeeForm, setEmployeeForm] = useState({
    fullName: '',
    username: '',
    position: '',
    phone: '',
    email: ''
  });

  // ---- Generation state ----
  const [generating, setGenerating] = useState({ clients: false, materials: false, employees: false, orders: false });

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

  const { data: employeesData = [], refetch: refetchEmployees } = useQuery({
    queryKey: ['admin-employees'],
    queryFn: async () => {
      const response = await api.get('/api/v1/employees?size=100');
      return response.data.content || [];
    },
    enabled: tab === 2
  });

  const { data: ordersData = [], refetch: refetchOrders } = useQuery({
    queryKey: ['admin-orders'],
    queryFn: async () => {
      const response = await api.get('/api/v1/orders?size=100');
      return response.data.content || [];
    },
    enabled: tab === 3
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

  // ---- Employees ----
  const createEmployeeMutation = useMutation({
    mutationFn: (employee) => api.post('/api/v1/employees', employee),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-employees'] });
      setEmployeeDialogOpen(false);
      showNotification('Сотрудник создан');
      resetEmployeeForm();
    },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  const updateEmployeeMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/employees/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-employees'] });
      setEmployeeDialogOpen(false);
      showNotification('Сотрудник обновлен');
      resetEmployeeForm();
    },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  const deleteEmployeeMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/employees/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-employees'] });
      setEmployeeDeleteDialogOpen(false);
      showNotification('Сотрудник удален');
    },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  // ---- Orders ----
  const deleteOrderMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/orders/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-orders'] });
      showNotification('Заказ удален');
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

  const generateEmployeesMutation = useMutation({
    mutationFn: () => api.post('/api/v1/admin/employees/generate'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-employees'] });
      showNotification('Сгенерировано 20 сотрудников');
      setGenerating(prev => ({ ...prev, employees: false }));
    },
    onError: (err) => {
      showNotification('Ошибка: ' + err.message, 'error');
      setGenerating(prev => ({ ...prev, employees: false }));
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
        email: client.email || '',
        inn: client.inn || '',
        address: client.address || ''
      });
    } else {
      resetClientForm();
    }
    setClientDialogOpen(true);
  };

  const resetClientForm = () => {
    setClientForm({ name: '', type: 'PRIVATE', contactPerson: '', phone: '', email: '', inn: '', address: '' });
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

  // Employee handlers
  const openEmployeeDialog = (employee = null) => {
    if (employee) {
      setSelectedEmployee(employee);
      setEmployeeForm({
        fullName: employee.fullName || '',
        username: employee.username || '',
        position: employee.position || '',
        phone: employee.phone || '',
        email: employee.email || ''
      });
    } else {
      resetEmployeeForm();
    }
    setEmployeeDialogOpen(true);
  };

  const resetEmployeeForm = () => {
    setEmployeeForm({ fullName: '', username: '', position: '', phone: '', email: '' });
    setSelectedEmployee(null);
  };

  const handleEmployeeSubmit = () => {
    if (!employeeForm.fullName) {
      showNotification('Введите ФИО', 'error');
      return;
    }
    const payload = { ...employeeForm };
    if (selectedEmployee) {
      updateEmployeeMutation.mutate({ id: selectedEmployee.id, data: payload });
    } else {
      createEmployeeMutation.mutate(payload);
    }
  };

  const confirmDeleteEmployee = (employee) => {
    setSelectedEmployee(employee);
    setEmployeeDeleteDialogOpen(true);
  };

  const handleDeleteEmployee = () => {
    if (selectedEmployee) {
      deleteEmployeeMutation.mutate(selectedEmployee.id);
    }
  };

  // Generate handlers
  const handleGenerate = (type) => {
    setGenerating(prev => ({ ...prev, [type]: true }));
    switch (type) {
      case 'clients': generateClientsMutation.mutate(); break;
      case 'materials': generateMaterialsMutation.mutate(); break;
      case 'employees': generateEmployeesMutation.mutate(); break;
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

  const renderEmployeesTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление сотрудниками</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => openEmployeeDialog()}>
          Добавить сотрудника
        </Button>
      </Box>
      {employeesData.length === 0 && <Typography>Нет данных</Typography>}
      {employeesData.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ФИО</TableCell>
                <TableCell>Логин</TableCell>
                <TableCell>Должность</TableCell>
                <TableCell>Телефон</TableCell>
                <TableCell>Email</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {employeesData.map((emp) => (
                <TableRow key={emp.id}>
                  <TableCell>{emp.fullName}</TableCell>
                  <TableCell>{emp.username}</TableCell>
                  <TableCell>{emp.position || '-'}</TableCell>
                  <TableCell>{emp.phone || '-'}</TableCell>
                  <TableCell>{emp.email || '-'}</TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => openEmployeeDialog(emp)}><Edit /></IconButton>
                    <IconButton size="small" color="error" onClick={() => confirmDeleteEmployee(emp)}><Delete /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );

  const renderOrdersTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление заказами</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => navigate('/orders/new')}>
          Создать заказ
        </Button>
      </Box>
      {ordersData.length === 0 && <Typography>Нет данных</Typography>}
      {ordersData.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>№ заказа</TableCell>
                <TableCell>Клиент</TableCell>
                <TableCell>Статус</TableCell>
                <TableCell>Сумма</TableCell>
                <TableCell>Оплачено</TableCell>
                <TableCell>Менеджер</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {ordersData.map((order) => (
                <TableRow key={order.id}>
                  <TableCell>{order.orderNumber}</TableCell>
                  <TableCell>{order.client?.name || order.clientId}</TableCell>
                  <TableCell>
                    <Chip label={order.status} size="small" color={
                      order.status === 'WAITING' ? 'warning' :
                      order.status === 'LAUNCHED' ? 'info' :
                      order.status === 'IN_PROGRESS' ? 'primary' :
                      order.status === 'READY' ? 'success' : 'default'
                    } />
                  </TableCell>
                  <TableCell>{order.totalAmount?.toFixed(2)} ₽</TableCell>
                  <TableCell>{order.paidAmount?.toFixed(2)} ₽</TableCell>
                  <TableCell>{order.manager?.fullName || order.managerId || '-'}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="Просмотр">
                      <IconButton size="small" onClick={() => navigate(`/orders/${order.id}`)}>
                        <Visibility />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Удалить">
                      <IconButton size="small" color="error" onClick={() => {
                        if (window.confirm('Удалить заказ?')) deleteOrderMutation.mutate(order.id);
                      }}>
                        <Delete />
                      </IconButton>
                    </Tooltip>
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
            onClick={() => handleGenerate('employees')}
            disabled={generating.employees}
          >
            {generating.employees ? 'Генерация...' : 'Сгенерировать сотрудников (20)'}
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
          {tab === 2 && renderEmployeesTab()}
          {tab === 3 && renderOrdersTab()}
          {tab === 4 && renderGenerateTab()}
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
          <TextField fullWidth margin="dense" label="ИНН" value={clientForm.inn} onChange={(e) => setClientForm({ ...clientForm, inn: e.target.value })} />
          <TextField fullWidth margin="dense" label="Адрес" value={clientForm.address} onChange={(e) => setClientForm({ ...clientForm, address: e.target.value })} />
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
          <TextField fullWidth margin="dense" label="Ед. изм." value={materialForm.unit} onChange={(e) => setMaterialForm({ ...materialForm, unit: e.target.value })} />
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

      {/* Employee Dialog */}
      <Dialog open={employeeDialogOpen} onClose={() => setEmployeeDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedEmployee ? 'Редактировать сотрудника' : 'Новый сотрудник'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="ФИО" value={employeeForm.fullName} onChange={(e) => setEmployeeForm({ ...employeeForm, fullName: e.target.value })} />
          <TextField fullWidth margin="dense" label="Логин" value={employeeForm.username} onChange={(e) => setEmployeeForm({ ...employeeForm, username: e.target.value })} />
          <TextField fullWidth margin="dense" label="Должность" value={employeeForm.position} onChange={(e) => setEmployeeForm({ ...employeeForm, position: e.target.value })} />
          <TextField fullWidth margin="dense" label="Телефон" value={employeeForm.phone} onChange={(e) => setEmployeeForm({ ...employeeForm, phone: e.target.value })} />
          <TextField fullWidth margin="dense" label="Email" type="email" value={employeeForm.email} onChange={(e) => setEmployeeForm({ ...employeeForm, email: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEmployeeDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleEmployeeSubmit} variant="contained">Сохранить</Button>
        </DialogActions>
      </Dialog>

      {/* Employee Delete Confirmation */}
      <Dialog open={employeeDeleteDialogOpen} onClose={() => setEmployeeDeleteDialogOpen(false)}>
        <DialogTitle>Удалить сотрудника?</DialogTitle>
        <DialogContent>
          <Typography>Удалить "{selectedEmployee?.fullName}"?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEmployeeDeleteDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleDeleteEmployee} color="error" variant="contained">Удалить</Button>
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
