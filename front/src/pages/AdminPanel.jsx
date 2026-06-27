import { useState } from 'react';
import {
  Box, Typography, Tabs, Tab, Paper, Table, TableHead, TableRow, TableCell,
  TableBody, Button, IconButton, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, FormControl, InputLabel, Select, MenuItem, CircularProgress, Alert,
  Snackbar, Grid, Divider, TableContainer, Checkbox, Chip
} from '@mui/material';
import { Add, Edit, Delete, Refresh, Save, Cancel, Sync } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';
import GenerateTab from '../components/AdminPanel/GenerateTab';

const UNIT_DISPLAY_TO_ENUM = {
  'м²': 'SQUARE_METER',
  'п.м.': 'LINEAR_METER',
  'шт': 'PIECE'
};

const ENTITY_TABS = [
  { label: 'Clients', value: 'clients' },
  { label: 'Materials', value: 'materials' },
  { label: 'Operations', value: 'operations' },
  { label: 'Workshops', value: 'workshops' },
  { label: 'Employees', value: 'employees' },
  { label: 'Generate Data', value: 'generate' }
];

const AdminPanel = () => {
  const [tab, setTab] = useState(0);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
  const showNotification = (message, severity = 'success') => {
    setNotification({ open: true, message, severity });
  };

  // ---- Clients state ----
  const [clientDialogOpen, setClientDialogOpen] = useState(false);
  const [clientDeleteDialogOpen, setClientDeleteDialogOpen] = useState(false);
  const [selectedClient, setSelectedClient] = useState(null);
  const [clientForm, setClientForm] = useState({ name: '', type: 'PRIVATE', contactPerson: '', phone: '', email: '' });

  // ---- Materials state ----
  const [materialDialogOpen, setMaterialDialogOpen] = useState(false);
  const [materialDeleteDialogOpen, setMaterialDeleteDialogOpen] = useState(false);
  const [selectedMaterial, setSelectedMaterial] = useState(null);
  const [materialForm, setMaterialForm] = useState({ name: '', unit: '', price: '', wasteCoefficient: '1' });
  const [editingMaterialId, setEditingMaterialId] = useState(null);
  const [editMaterialForm, setEditMaterialForm] = useState({ name: '', unit: '', price: 0, wasteCoefficient: 1 });

  // ---- Operations state ----
  const [operationFilter, setOperationFilter] = useState('ALL');
  const [operationDialogOpen, setOperationDialogOpen] = useState(false);
  const [operationDeleteDialogOpen, setOperationDeleteDialogOpen] = useState(false);
  const [selectedOperation, setSelectedOperation] = useState(null);
  const [operationForm, setOperationForm] = useState({ name: '', unit: 'SQUARE_METER', price: '', applicableTo: 'BANNER', isDefault: false, hemWidthMm: '', hemCount: '' });
  const [editingOperationId, setEditingOperationId] = useState(null);
  const [editOperationForm, setEditOperationForm] = useState({ name: '', unit: 'SQUARE_METER', price: 0, applicableTo: 'BANNER', isDefault: false, hemWidthMm: null, hemCount: null });

  // ---- Workshops state ----
  const [workshopDialogOpen, setWorkshopDialogOpen] = useState(false);
  const [workshopDeleteDialogOpen, setWorkshopDeleteDialogOpen] = useState(false);
  const [selectedWorkshop, setSelectedWorkshop] = useState(null);
  const [workshopForm, setWorkshopForm] = useState({ name: '', operationIds: [] });

  // ---- Employees state ----
  const [employeeDialogOpen, setEmployeeDialogOpen] = useState(false);
  const [employeeDeleteDialogOpen, setEmployeeDeleteDialogOpen] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [employeeRoles, setEmployeeRoles] = useState([]);
  const [employeeForm, setEmployeeForm] = useState({ fullName: '', username: '', position: '', phone: '', email: '', workshopId: '', managerCashPercent: '' });
  const [syncing, setSyncing] = useState(false);

  // ---- Generation state ----
  const [generating, setGenerating] = useState({});

  // ---- Fetch data ----
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

  const { data: operationsData = [], refetch: refetchOperations } = useQuery({
    queryKey: ['admin-operations'],
    queryFn: async () => { const r = await api.get('/api/v1/calculations/operations'); return r.data || []; },
    enabled: tab === 2 || tab === 3
  });

  const { data: workshopsData = [], refetch: refetchWorkshops } = useQuery({
    queryKey: ['admin-workshops'],
    queryFn: async () => { const r = await api.get('/api/v1/workshops?size=100'); return r.data.content || []; },
    enabled: tab === 3
  });

  const { data: employeesData = [], refetch: refetchEmployees } = useQuery({
    queryKey: ['admin-employees'],
    queryFn: async () => { const r = await api.get('/api/v1/employees?size=100'); return r.data.content || []; },
    enabled: tab === 4
  });

  // ---- Client CRUD ----
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

  // ---- Material CRUD ----
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

  // ---- Operation CRUD ----
  const createOperationMutation = useMutation({
    mutationFn: (op) => api.post('/api/v1/admin/operations', op),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-operations'] }); setOperationDialogOpen(false); showNotification('Операция создана'); resetOperationForm(); },
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

  // ---- Generate mutation ----
  const generateMutation = useMutation({
    mutationFn: ({ endpoint, count }) => api.post(`/api/v1/admin/generate/${endpoint}/${count}`),
    onSuccess: (data, variables) => {
      const labels = {
        clients: 'клиентов',
        materials: 'материалов',
        operations: 'операций',
        workshops: 'цехов',
        employees: 'сотрудников',
        orders: 'заказов',
      };
      showNotification(`Сгенерировано ${variables.count} ${labels[variables.endpoint] || ''}`);
      queryClient.invalidateQueries({ queryKey: ['admin-clients'] });
      queryClient.invalidateQueries({ queryKey: ['admin-materials'] });
      queryClient.invalidateQueries({ queryKey: ['admin-operations'] });
      queryClient.invalidateQueries({ queryKey: ['admin-workshops'] });
      queryClient.invalidateQueries({ queryKey: ['admin-employees'] });
    },
    onError: (err) => {
      const msg = err.response?.data?.message || err.response?.data || err.message;
      showNotification('Ошибка: ' + msg, 'error');
    },
  });

  const handleGenerate = (endpoint, count) => {
    return generateMutation.mutateAsync({ endpoint, count });
  };

  const deleteMutation = useMutation({
    mutationFn: (endpoint) => api.delete(`/api/v1/admin/generate/${endpoint}`),
    onSuccess: (data, endpoint) => {
      const labels = {
        clients: 'клиентов',
        materials: 'материалов',
        operations: 'операций',
        workshops: 'цехов',
        employees: 'сотрудников',
        orders: 'заказов',
      };
      const deleted = data?.deleted ?? 0;
      showNotification(`Удалено ${deleted} ${labels[endpoint] || ''}`);
      queryClient.invalidateQueries({ queryKey: ['admin-clients'] });
      queryClient.invalidateQueries({ queryKey: ['admin-materials'] });
      queryClient.invalidateQueries({ queryKey: ['admin-operations'] });
      queryClient.invalidateQueries({ queryKey: ['admin-workshops'] });
      queryClient.invalidateQueries({ queryKey: ['admin-employees'] });
    },
    onError: (err) => {
      const msg = err.response?.data?.message || err.response?.data || err.message;
      showNotification('Ошибка: ' + msg, 'error');
    },
  });

  const handleDelete = (endpoint) => {
    return deleteMutation.mutateAsync(endpoint);
  };

  // ---- Handlers ----
  const handleTabChange = (_event, newValue) => { setTab(newValue); };

  const resetClientForm = () => { setClientForm({ name: '', type: 'PRIVATE', contactPerson: '', phone: '', email: '' }); setSelectedClient(null); };
  const resetMaterialForm = () => { setMaterialForm({ name: '', unit: '', price: '', wasteCoefficient: '1' }); setSelectedMaterial(null); };
  const resetOperationForm = () => { setOperationForm({ name: '', unit: 'SQUARE_METER', price: '', applicableTo: 'BANNER', isDefault: false, hemWidthMm: '', hemCount: '' }); setSelectedOperation(null); };
  const resetWorkshopForm = () => { setWorkshopForm({ name: '', sortOrder: 0, operationIds: '' }); setSelectedWorkshop(null); };
  const resetEmployeeForm = () => { setEmployeeForm({ fullName: '', username: '', position: '', phone: '', email: '', workshopId: '', managerCashPercent: '' }); setSelectedEmployee(null); };

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
      setWorkshopForm({ name: ws.name || '', operationIds: toArr(ws.operationIds) });
    } else { resetWorkshopForm(); }
    setWorkshopDialogOpen(true);
  };
  const openEmployeeDialog = (emp = null) => {
    if (emp) {
      setSelectedEmployee(emp);
      setEmployeeForm({ fullName: emp.fullName || '', username: emp.username || '', position: emp.position || '', phone: emp.phone || '', email: emp.email || '', workshopId: emp.workshopId || '', managerCashPercent: emp.managerCashPercent != null ? emp.managerCashPercent.toString() : '' });
      setEmployeeRoles(Array.isArray(emp.roles) ? emp.roles : []);
    } else {
      resetEmployeeForm();
      setEmployeeRoles([]);
    }
    setEmployeeDialogOpen(true);
  };

  const handleClientSubmit = () => {
    if (!clientForm.name) { showNotification('Введите название', 'error'); return; }
    const payload = { ...clientForm };
    selectedClient ? updateClientMutation.mutate({ id: selectedClient.id, data: payload }) : createClientMutation.mutate(payload);
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
    const payload = { name: workshopForm.name, operationIds: workshopForm.operationIds.map(Number) };
    selectedWorkshop ? updateWorkshopMutation.mutate({ id: selectedWorkshop.id, data: payload }) : createWorkshopMutation.mutate(payload);
  };
  const handleEmployeeSubmit = () => {
    if (!employeeForm.username) { showNotification('Введите логин', 'error'); return; }
    if (!selectedEmployee) { showNotification('Создание новых сотрудников запрещено', 'error'); return; }
    const payload = { ...employeeForm, workshopId: employeeForm.workshopId ? parseInt(employeeForm.workshopId) : null, managerCashPercent: employeeForm.managerCashPercent ? parseFloat(employeeForm.managerCashPercent) : null };
    updateEmployeeMutation.mutate({ id: selectedEmployee.id, data: payload });
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

  const handleOperationFilterChange = (e) => { setOperationFilter(e.target.value); };
  const filteredOperations = operationsData.filter(op => operationFilter === 'ALL' || op.applicableTo === operationFilter);

  // ---- Render ----
  const renderClientsTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление клиентами</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => openClientDialog()}>Добавить клиента</Button>
      </Box>
      {clientsData.length === 0 && <Typography>Нет данных</Typography>}
      {clientsData.length > 0 && (
        <TableContainer component={Paper}><Table size="small">
          <TableHead><TableRow><TableCell>Имя</TableCell><TableCell>Тип</TableCell><TableCell>Контактное лицо</TableCell><TableCell>Телефон</TableCell><TableCell>Email</TableCell><TableCell align="right">Действия</TableCell></TableRow></TableHead>
          <TableBody>
            {clientsData.map((c) => (
              <TableRow key={c.id}><TableCell>{c.name}</TableCell><TableCell>{c.type}</TableCell><TableCell>{c.contactPerson || '-'}</TableCell><TableCell>{c.phone || '-'}</TableCell><TableCell>{c.email || '-'}</TableCell>
                <TableCell align="right"><IconButton size="small" onClick={() => openClientDialog(c)}><Edit /></IconButton><IconButton size="small" color="error" onClick={() => { setSelectedClient(c); setClientDeleteDialogOpen(true); }}><Delete /></IconButton></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table></TableContainer>
      )}
    </Box>
  );

  const renderMaterialsTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление материалами</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => openMaterialDialog()}>Добавить материал</Button>
      </Box>
      {materialsData.length === 0 && <Typography>Нет данных</Typography>}
      {materialsData.length > 0 && (
        <TableContainer component={Paper}><Table size="small">
          <TableHead><TableRow><TableCell>Название</TableCell><TableCell>Ед. изм.</TableCell><TableCell>Цена</TableCell><TableCell>Коэф. отхода</TableCell><TableCell align="right">Действия</TableCell></TableRow></TableHead>
          <TableBody>
            {materialsData.map((mat) => (
              <TableRow key={mat.id}>
                <TableCell>{editingMaterialId === mat.id ? <TextField fullWidth size="small" value={editMaterialForm.name} onChange={handleEditMaterialChange('name')} /> : mat.name}</TableCell>
                <TableCell>{editingMaterialId === mat.id ? <FormControl fullWidth size="small"><Select value={editMaterialForm.unit} onChange={(e) => handleEditMaterialChange('unit')(e)}><MenuItem value="м2">м²</MenuItem><MenuItem value="м.п.">м.п.</MenuItem><MenuItem value="шт">шт</MenuItem></Select></FormControl> : mat.unit}</TableCell>
                <TableCell>{editingMaterialId === mat.id ? <TextField fullWidth size="small" type="number" value={editMaterialForm.price} onChange={handleEditMaterialChange('price')} inputProps={{ step: 0.01 }} /> : `${mat.price?.toFixed(2)} ₽`}</TableCell>
                <TableCell>{editingMaterialId === mat.id ? <TextField fullWidth size="small" type="number" value={editMaterialForm.wasteCoefficient} onChange={handleEditMaterialChange('wasteCoefficient')} inputProps={{ step: 0.1 }} /> : mat.wasteCoefficient?.toString()}</TableCell>
                <TableCell align="right">
                  {editingMaterialId === mat.id ? <><IconButton size="small" color="success" onClick={() => saveMaterialEdit(mat.id)}><Save /></IconButton><IconButton size="small" onClick={cancelEditMaterial}><Cancel /></IconButton></> : <IconButton size="small" onClick={() => startEditMaterial(mat)}><Edit /></IconButton>}
                  <IconButton size="small" color="error" onClick={() => { setSelectedMaterial(mat); setMaterialDeleteDialogOpen(true); }}><Delete /></IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table></TableContainer>
      )}
    </Box>
  );

  const renderOperationsTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление операциями</Typography>
        <Box display="flex" gap={2} alignItems="center">
          <FormControl size="small" sx={{ minWidth: 180 }}><InputLabel>Фильтр по типу</InputLabel>
            <Select value={operationFilter} label="Фильтр по типу" onChange={handleOperationFilterChange}><MenuItem value="ALL">Все</MenuItem><MenuItem value="BANNER">Баннер</MenuItem><MenuItem value="PLENKA">Пленка</MenuItem><MenuItem value="BOTH">Оба</MenuItem></Select>
          </FormControl>
          <Button variant="outlined" startIcon={<Refresh />} onClick={() => refetchOperations()}>Обновить</Button>
          <Button variant="contained" startIcon={<Add />} onClick={() => openOperationDialog()}>Добавить операцию</Button>
        </Box>
      </Box>
      {filteredOperations.length === 0 && <Typography>Нет операций</Typography>}
      {filteredOperations.length > 0 && (
        <TableContainer component={Paper}><Table size="small">
          <TableHead><TableRow><TableCell>Название</TableCell><TableCell>Цена</TableCell><TableCell>Ед. изм.</TableCell><TableCell>Применимо к</TableCell><TableCell>По умолчанию</TableCell><TableCell>Ширина подворота</TableCell><TableCell>Кол-во подворотов</TableCell><TableCell align="right">Действия</TableCell></TableRow></TableHead>
          <TableBody>
            {filteredOperations.map((op) => (
              <TableRow key={op.id}>
                <TableCell>{editingOperationId === op.id ? <TextField fullWidth size="small" value={editOperationForm.name} onChange={handleEditOperationChange('name')} /> : op.name}</TableCell>
                <TableCell>{editingOperationId === op.id ? <TextField fullWidth size="small" type="number" value={editOperationForm.price} onChange={handleEditOperationChange('price')} inputProps={{ step: 0.01 }} /> : `${op.price?.toFixed(2)} ₽`}</TableCell>
                <TableCell>{editingOperationId === op.id ? <FormControl fullWidth size="small"><Select value={editOperationForm.unit} onChange={(e) => handleEditOperationChange('unit')(e)}><MenuItem value="SQUARE_METER">м²</MenuItem><MenuItem value="LINEAR_METER">п.м.</MenuItem><MenuItem value="PIECE">шт</MenuItem></Select></FormControl> : op.unit}</TableCell>
                <TableCell>{editingOperationId === op.id ? <FormControl fullWidth size="small"><Select value={editOperationForm.applicableTo} onChange={(e) => handleEditOperationChange('applicableTo')(e)}><MenuItem value="BANNER">Баннер</MenuItem><MenuItem value="PLENKA">Пленка</MenuItem><MenuItem value="BOTH">Оба</MenuItem></Select></FormControl> : (op.applicableTo === 'BANNER' ? 'Баннер' : op.applicableTo === 'PLENKA' ? 'Пленка' : 'Оба')}</TableCell>
                <TableCell>{editingOperationId === op.id ? <Checkbox checked={editOperationForm.isDefault} onChange={handleEditOperationChange('isDefault')} /> : (op.isDefault ? 'Да' : 'Нет')}</TableCell>
                <TableCell>{editingOperationId === op.id ? <TextField fullWidth size="small" type="number" value={editOperationForm.hemWidthMm != null ? editOperationForm.hemWidthMm : ''} onChange={handleEditOperationChange('hemWidthMm')} inputProps={{ min: 0 }} /> : (op.hemWidthMm || '-')}</TableCell>
                <TableCell>{editingOperationId === op.id ? <TextField fullWidth size="small" type="number" value={editOperationForm.hemCount != null ? editOperationForm.hemCount : ''} onChange={handleEditOperationChange('hemCount')} inputProps={{ min: 0 }} /> : (op.hemCount || '-')}</TableCell>
                <TableCell align="right">
                  {editingOperationId === op.id ? <><IconButton size="small" color="success" onClick={() => saveOperationEdit(op.id)}><Save /></IconButton><IconButton size="small" onClick={cancelEditOperation}><Cancel /></IconButton></> : <IconButton size="small" onClick={() => startEditOperation(op)}><Edit /></IconButton>}
                  <IconButton size="small" color="error" onClick={() => { setSelectedOperation(op); setOperationDeleteDialogOpen(true); }}><Delete /></IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table></TableContainer>
      )}
    </Box>
  );

  const renderWorkshopsTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление цехами</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => openWorkshopDialog()}>Добавить цех</Button>
      </Box>
      {workshopsData.length === 0 && <Typography>Нет цехов</Typography>}
      {workshopsData.length > 0 && (
        <TableContainer component={Paper}><Table size="small">
          <TableHead><TableRow><TableCell>ID</TableCell><TableCell>Название</TableCell><TableCell>Операции</TableCell><TableCell align="right">Действия</TableCell></TableRow></TableHead>
          <TableBody>
            {workshopsData.map((ws) => (
              <TableRow key={ws.id}>
                <TableCell>{ws.id}</TableCell>
                <TableCell>{ws.name}</TableCell>
                <TableCell><Box display="flex" flexWrap="wrap" gap={0.5}>{(Array.isArray(ws.operationIds) ? ws.operationIds : []).map(id => { const op = operationsData.find(o => o.id === id); return <Chip key={id} label={op ? op.name : `#${id}`} size="small" variant="outlined" />; })}</Box></TableCell>
                <TableCell align="right">
                  <IconButton size="small" onClick={() => openWorkshopDialog(ws)}><Edit /></IconButton>
                  <IconButton size="small" color="error" onClick={() => { setSelectedWorkshop(ws); setWorkshopDeleteDialogOpen(true); }}><Delete /></IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table></TableContainer>
      )}
    </Box>
  );

  const getWorkshopName = (workshopId) => {
    const ws = workshopsData.find(w => w.id === workshopId);
    return ws ? ws.name : (workshopId || '-');
  };

  const renderEmployeesTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление сотрудниками</Typography>
        <Box display="flex" gap={2}>
          <Button variant="outlined" startIcon={syncing ? <CircularProgress size={16} /> : <Sync />} onClick={() => syncKeycloakMutation.mutate()} disabled={syncing}>
            {syncing ? 'Синхронизация...' : 'Синхронизировать из Keycloak'}
          </Button>
        </Box>
      </Box>
      {employeesData.length === 0 && <Typography>Нет сотрудников</Typography>}
      {employeesData.length > 0 && (
        <TableContainer component={Paper}><Table size="small">
           <TableHead><TableRow><TableCell>ID</TableCell><TableCell>ФИО</TableCell><TableCell>Логин</TableCell><TableCell>Должность</TableCell><TableCell>Телефон</TableCell><TableCell>Email</TableCell><TableCell>Роль</TableCell><TableCell>Роли</TableCell><TableCell>Цех</TableCell><TableCell>% заработка</TableCell><TableCell align="right">Действия</TableCell></TableRow></TableHead>
          <TableBody>
            {employeesData.map((emp) => (
              <TableRow key={emp.id}>
                <TableCell>{emp.id}</TableCell>
                <TableCell>{emp.fullName || '-'}</TableCell>
                <TableCell>{emp.username || '-'}</TableCell>
                <TableCell>{emp.position || '-'}</TableCell>
                <TableCell>{emp.phone || '-'}</TableCell>
                <TableCell>{emp.email || '-'}</TableCell>
                <TableCell>{emp.roleId ? <Chip label={`#${emp.roleId}`} size="small" color="primary" /> : '-'}</TableCell>
                <TableCell>
                  <Box display="flex" flexWrap="wrap" gap={0.5}>
                    {(emp.roles || []).map(role => (
                      <Chip key={role} label={role.replace('ROLE_', '')} size="small" variant="outlined" />
                    ))}
                    {(!emp.roles || emp.roles.length === 0) && '-'}
                  </Box>
                </TableCell>
                <TableCell>{emp.workshopId ? `#${emp.workshopId} ${getWorkshopName(emp.workshopId)}` : '-'}</TableCell>
                <TableCell>{emp.managerCashPercent != null ? `${emp.managerCashPercent}%` : '-'}</TableCell>
                <TableCell align="right">
                  <IconButton size="small" onClick={() => openEmployeeDialog(emp)}><Edit /></IconButton>
                  <IconButton size="small" color="error" onClick={() => { setSelectedEmployee(emp); setEmployeeDeleteDialogOpen(true); }}><Delete /></IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table></TableContainer>
      )}
    </Box>
  );

  const renderGenerateTab = () => (
    <GenerateTab onGenerate={handleGenerate} onDelete={handleDelete} />
  );

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>Admin Panel</Typography>
      <Paper sx={{ width: '100%', mt: 2 }}>
        <Tabs value={tab} onChange={handleTabChange} indicatorColor="secondary" textColor="secondary" variant="scrollable" scrollButtons="auto">
          {ENTITY_TABS.map((t, idx) => <Tab key={t.value} label={t.label} />)}
        </Tabs>
        <Divider />
        <Box sx={{ p: 2 }}>
          {tab === 0 && renderClientsTab()}
          {tab === 1 && renderMaterialsTab()}
          {tab === 2 && renderOperationsTab()}
          {tab === 3 && renderWorkshopsTab()}
          {tab === 4 && renderEmployeesTab()}
          {tab === 5 && renderGenerateTab()}
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

      {/* Material */}
      <Dialog open={materialDialogOpen} onClose={() => setMaterialDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedMaterial ? 'Редактировать материал' : 'Новый материал'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Название" value={materialForm.name} onChange={(e) => setMaterialForm({ ...materialForm, name: e.target.value })} />
          <FormControl fullWidth margin="dense"><InputLabel>Ед. изм.</InputLabel>
            <Select value={materialForm.unit} label="Ед. изм." onChange={(e) => setMaterialForm({ ...materialForm, unit: e.target.value })}><MenuItem value="м2">м²</MenuItem><MenuItem value="м.п.">м.п.</MenuItem><MenuItem value="шт">шт</MenuItem></Select>
          </FormControl>
          <TextField fullWidth margin="dense" label="Цена" type="number" value={materialForm.price} onChange={(e) => setMaterialForm({ ...materialForm, price: e.target.value })} inputProps={{ step: 0.01 }} />
          <TextField fullWidth margin="dense" label="Коэффициент отхода" type="number" value={materialForm.wasteCoefficient} onChange={(e) => setMaterialForm({ ...materialForm, wasteCoefficient: e.target.value })} inputProps={{ step: 0.1 }} />
        </DialogContent>
        <DialogActions><Button onClick={() => setMaterialDialogOpen(false)}>Отмена</Button><Button onClick={handleMaterialSubmit} variant="contained">Сохранить</Button></DialogActions>
      </Dialog>
      <Dialog open={materialDeleteDialogOpen} onClose={() => setMaterialDeleteDialogOpen(false)}>
        <DialogTitle>Удалить материал?</DialogTitle><DialogContent><Typography>Удалить "{selectedMaterial?.name}"?</Typography></DialogContent>
        <DialogActions><Button onClick={() => setMaterialDeleteDialogOpen(false)}>Отмена</Button><Button onClick={() => selectedMaterial && deleteMaterialMutation.mutate(selectedMaterial.id)} color="error" variant="contained">Удалить</Button></DialogActions>
      </Dialog>

      {/* Operation */}
      <Dialog open={operationDialogOpen} onClose={() => setOperationDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedOperation ? 'Редактировать операцию' : 'Новая операция'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Название" value={operationForm.name} onChange={(e) => setOperationForm({ ...operationForm, name: e.target.value })} />
          <FormControl fullWidth margin="dense"><InputLabel>Ед. изм.</InputLabel>
            <Select value={operationForm.unit} label="Ед. изм." onChange={(e) => setOperationForm({ ...operationForm, unit: e.target.value })}><MenuItem value="SQUARE_METER">м²</MenuItem><MenuItem value="LINEAR_METER">п.м.</MenuItem><MenuItem value="PIECE">шт</MenuItem></Select>
          </FormControl>
          <TextField fullWidth margin="dense" label="Цена" type="number" value={operationForm.price} onChange={(e) => setOperationForm({ ...operationForm, price: e.target.value })} inputProps={{ step: 0.01 }} />
          <FormControl fullWidth margin="dense"><InputLabel>Применимо к</InputLabel>
            <Select value={operationForm.applicableTo} label="Применимо к" onChange={(e) => setOperationForm({ ...operationForm, applicableTo: e.target.value })}><MenuItem value="BANNER">Баннер</MenuItem><MenuItem value="PLENKA">Пленка</MenuItem><MenuItem value="BOTH">Оба</MenuItem></Select>
          </FormControl>
          <TextField fullWidth margin="dense" label="Ширина подворота (мм)" type="number" value={operationForm.hemWidthMm} onChange={(e) => setOperationForm({ ...operationForm, hemWidthMm: e.target.value })} inputProps={{ min: 0 }} placeholder="Необязательно" />
          <TextField fullWidth margin="dense" label="Количество подворотов на сторону" type="number" value={operationForm.hemCount} onChange={(e) => setOperationForm({ ...operationForm, hemCount: e.target.value })} inputProps={{ min: 0 }} placeholder="Необязательно" />
          <Box display="flex" alignItems="center" mt={1}><Checkbox checked={operationForm.isDefault} onChange={(e) => setOperationForm({ ...operationForm, isDefault: e.target.checked })} /><Typography>По умолчанию</Typography></Box>
        </DialogContent>
        <DialogActions><Button onClick={() => setOperationDialogOpen(false)}>Отмена</Button><Button onClick={handleOperationSubmit} variant="contained">Сохранить</Button></DialogActions>
      </Dialog>
      <Dialog open={operationDeleteDialogOpen} onClose={() => setOperationDeleteDialogOpen(false)}>
        <DialogTitle>Удалить операцию?</DialogTitle><DialogContent><Typography>Удалить "{selectedOperation?.name}"?</Typography></DialogContent>
        <DialogActions><Button onClick={() => setOperationDeleteDialogOpen(false)}>Отмена</Button><Button onClick={() => selectedOperation && deleteOperationMutation.mutate(selectedOperation.id)} color="error" variant="contained">Удалить</Button></DialogActions>
      </Dialog>

      {/* Workshop */}
      <Dialog open={workshopDialogOpen} onClose={() => setWorkshopDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedWorkshop ? 'Редактировать цех' : 'Новый цех'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Название" value={workshopForm.name} onChange={(e) => setWorkshopForm({ ...workshopForm, name: e.target.value })} />
          <Box sx={{ mt: 1 }}>
            <Typography variant="subtitle2" gutterBottom>Операции</Typography>
            <Box display="flex" flexWrap="wrap" gap={1} mb={1}>
              {(Array.isArray(workshopForm.operationIds) ? workshopForm.operationIds : []).map(id => {
                const op = operationsData.find(o => o.id === id);
                return (
                  <Chip key={id} label={op ? op.name : `#${id}`} size="small" variant="outlined"
                    onDelete={() => setWorkshopForm({ ...workshopForm, operationIds: (Array.isArray(workshopForm.operationIds) ? workshopForm.operationIds : []).filter(x => x !== Number(id)) })} />
                );
              })}
            </Box>
            <FormControl fullWidth size="small" margin="dense">
              <InputLabel>Добавить операцию</InputLabel>
              <Select value="" label="Добавить операцию" onChange={(e) => { const v = Number(e.target.value); if (v && !workshopForm.operationIds.includes(v)) { setWorkshopForm({ ...workshopForm, operationIds: [...workshopForm.operationIds, v] }); } }}>
                {operationsData.filter(op => !(Array.isArray(workshopForm.operationIds) ? workshopForm.operationIds : []).includes(Number(op.id))).map(op => <MenuItem key={op.id} value={op.id}>{op.name}</MenuItem>)}
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions><Button onClick={() => setWorkshopDialogOpen(false)}>Отмена</Button><Button onClick={handleWorkshopSubmit} variant="contained" disabled={!workshopForm.name}>Сохранить</Button></DialogActions>
      </Dialog>
      <Dialog open={workshopDeleteDialogOpen} onClose={() => setWorkshopDeleteDialogOpen(false)}>
        <DialogTitle>Удалить цех?</DialogTitle><DialogContent><Typography>Удалить "{selectedWorkshop?.name}"?</Typography></DialogContent>
        <DialogActions><Button onClick={() => setWorkshopDeleteDialogOpen(false)}>Отмена</Button><Button onClick={() => selectedWorkshop && deleteWorkshopMutation.mutate(selectedWorkshop.id)} color="error" variant="contained">Удалить</Button></DialogActions>
      </Dialog>

      {/* Employee */}
      <Dialog open={employeeDialogOpen} onClose={() => setEmployeeDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedEmployee ? 'Редактировать сотрудника' : 'Новый сотрудник'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="ФИО" value={employeeForm.fullName} onChange={(e) => setEmployeeForm({ ...employeeForm, fullName: e.target.value })} />
          <TextField fullWidth margin="dense" label="Логин (username)" value={employeeForm.username} onChange={(e) => setEmployeeForm({ ...employeeForm, username: e.target.value })} />
          <TextField fullWidth margin="dense" label="Должность" value={employeeForm.position} onChange={(e) => setEmployeeForm({ ...employeeForm, position: e.target.value })} />
          <TextField fullWidth margin="dense" label="Телефон" value={employeeForm.phone} onChange={(e) => setEmployeeForm({ ...employeeForm, phone: e.target.value })} />
          <TextField fullWidth margin="dense" label="Email" value={employeeForm.email} onChange={(e) => setEmployeeForm({ ...employeeForm, email: e.target.value })} />
          {(employeeRoles.includes('ROLE_MANAGER') || employeeRoles.includes('manager')) && (
            <TextField fullWidth margin="dense" label="Процент заработка менеджера (managerCashPercent)" type="number" value={employeeForm.managerCashPercent} onChange={(e) => setEmployeeForm({ ...employeeForm, managerCashPercent: e.target.value })} inputProps={{ min: 0, max: 100, step: 0.01 }} />
          )}
          {!selectedEmployee || employeeRoles.includes('ROLE_PRODUCTION') || employeeRoles.includes('production') ? (
            <FormControl fullWidth margin="dense"><InputLabel>Цех</InputLabel>
              <Select value={employeeForm.workshopId} label="Цех" onChange={(e) => setEmployeeForm({ ...employeeForm, workshopId: e.target.value })}>
                <MenuItem value="">Не назначен</MenuItem>
                {workshopsData.map((ws) => <MenuItem key={ws.id} value={ws.id}>{ws.name}</MenuItem>)}
              </Select>
            </FormControl>
          ) : null}
        </DialogContent>
        <DialogActions><Button onClick={() => setEmployeeDialogOpen(false)}>Отмена</Button><Button onClick={handleEmployeeSubmit} variant="contained" disabled={!employeeForm.username}>Сохранить</Button></DialogActions>
      </Dialog>
      <Dialog open={employeeDeleteDialogOpen} onClose={() => setEmployeeDeleteDialogOpen(false)}>
        <DialogTitle>Удалить сотрудника?</DialogTitle><DialogContent><Typography>Удалить "{selectedEmployee?.fullName || selectedEmployee?.username}"?</Typography></DialogContent>
        <DialogActions><Button onClick={() => setEmployeeDeleteDialogOpen(false)}>Отмена</Button><Button onClick={() => selectedEmployee && deleteEmployeeMutation.mutate(selectedEmployee.id)} color="error" variant="contained">Удалить</Button></DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar open={notification.open} autoHideDuration={3000} onClose={() => setNotification({ ...notification, open: false })} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })} sx={{ width: '100%' }}>{notification.message}</Alert>
      </Snackbar>
    </Box>
  );
};

export default AdminPanel;
