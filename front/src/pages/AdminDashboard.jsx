import { useState } from 'react';
import {
  Box, Typography, Paper, TextField, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  Table, TableHead, TableRow, TableCell, TableBody, IconButton, MenuItem, Select, FormControl, InputLabel,
  Snackbar, Alert, CircularProgress, Divider, Chip, Tabs, Tab, TableContainer
} from '@mui/material';
import { Add, Edit, Delete, Refresh } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';

const AdminDashboard = () => {
  const [tab, setTab] = useState(0);
  const queryClient = useQueryClient();
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });
  const showNotification = (message, severity = 'success') => {
    setNotification({ open: true, message, severity });
  };

  // ---- Companies state ----
  const [companyDialogOpen, setCompanyDialogOpen] = useState(false);
  const [companyDeleteDialogOpen, setCompanyDeleteDialogOpen] = useState(false);
  const [selectedCompany, setSelectedCompany] = useState(null);
  const [companyForm, setCompanyForm] = useState({ name: '' });

  // ---- Company Admins state ----
  const [adminDialogOpen, setAdminDialogOpen] = useState(false);
  const [adminDeleteDialogOpen, setAdminDeleteDialogOpen] = useState(false);
  const [selectedAdmin, setSelectedAdmin] = useState(null);
  const [adminForm, setAdminForm] = useState({ username: '', password: '', fullName: '', email: '', companyId: '', role: 'COMPANY_ADMIN' });

  // ---- Fetch data ----
  const { data: companiesData = [], refetch: refetchCompanies } = useQuery({
    queryKey: ['admin-dashboard-companies'],
    queryFn: async () => { const r = await api.get('/api/v1/companies?size=100'); return r.data.content || []; },
  });

  const { data: employeesData = [], refetch: refetchEmployees } = useQuery({
    queryKey: ['admin-dashboard-employees'],
    queryFn: async () => { const r = await api.get('/api/v1/employees?size=100'); return r.data.content || []; },
  });

  // ---- Company CRUD ----
  const createCompanyMutation = useMutation({
    mutationFn: (c) => api.post('/api/v1/companies', c),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-dashboard-companies'] }); setCompanyDialogOpen(false); showNotification('Компания создана'); resetCompanyForm(); },
    onError: (err) => showNotification('Ошибка: ' + (err.response?.data?.message || err.message), 'error')
  });
  const updateCompanyMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/companies/${id}`, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-dashboard-companies'] }); setCompanyDialogOpen(false); showNotification('Компания обновлена'); resetCompanyForm(); },
    onError: (err) => showNotification('Ошибка: ' + (err.response?.data?.message || err.message), 'error')
  });
  const deleteCompanyMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/companies/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-dashboard-companies'] }); setCompanyDeleteDialogOpen(false); showNotification('Компания удалена'); },
    onError: (err) => showNotification('Ошибка: ' + (err.response?.data?.message || err.message), 'error')
  });

  // ---- Company Admin creation via Keycloak API ----
  const createAdminMutation = useMutation({
    mutationFn: async (adminData) => {
      const keycloakUrl = import.meta.env.VITE_KEYCLOAK_ISSUER || `http://${import.meta.env.VITE_SERVER_IP || '192.168.1.40'}:8080/realms/print-sv`;
      const adminTokenRes = await fetch(`${keycloakUrl.replace('/realms/print-sv', '')}/realms/master/protocol/openid-connect/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          grant_type: 'password',
          client_id: 'admin-cli',
          username: 'admin',
          password: 'admin'
        })
      });
      if (!adminTokenRes.ok) throw new Error('Не удалось получить токен Keycloak');
      const tokenData = await adminTokenRes.json();
      const adminToken = tokenData.access_token;

      const createUserRes = await fetch(`${keycloakUrl}/admin/realms/print-sv/users`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${adminToken}`
        },
        body: JSON.stringify({
          username: adminData.username,
          password: adminData.password,
          firstName: adminData.fullName,
          lastName: '',
          email: adminData.email,
          enabled: true,
          credentials: [{ type: 'password', value: adminData.password, temporary: false }],
          realmRoles: [adminData.role]
        })
      });

      if (!createUserRes.ok) {
        const errText = await createUserRes.text();
        throw new Error(errText || 'Ошибка создания пользователя в Keycloak');
      }

      await api.post('/api/v1/employees/sync');
      return true;
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-dashboard-employees'] }); setAdminDialogOpen(false); showNotification('Администратор компании создан'); resetAdminForm(); },
    onError: (err) => showNotification('Ошибка: ' + err.message, 'error')
  });

  const deleteEmployeeMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/employees/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-dashboard-employees'] }); setAdminDeleteDialogOpen(false); showNotification('Администратор удален'); },
    onError: (err) => showNotification('Ошибка: ' + (err.response?.data?.message || err.message), 'error')
  });

  // ---- Handlers ----
  const resetCompanyForm = () => { setCompanyForm({ name: '' }); setSelectedCompany(null); };
  const resetAdminForm = () => { setAdminForm({ username: '', password: '', fullName: '', email: '', companyId: '', role: 'COMPANY_ADMIN' }); setSelectedAdmin(null); };

  const openCompanyDialog = (company = null) => {
    if (company) { setSelectedCompany(company); setCompanyForm({ name: company.name || '' }); } else { resetCompanyForm(); }
    setCompanyDialogOpen(true);
  };

  const openAdminDialog = (emp = null) => {
    if (emp) {
      setSelectedAdmin(emp);
      setAdminForm({ username: emp.username || '', password: '', fullName: emp.fullName || '', email: emp.email || '', companyId: emp.companyId ? String(emp.companyId) : '', role: emp.roles?.includes('ROLE_ADMIN') ? 'COMPANY_ADMIN' : 'COMPANY_ADMIN' });
    } else {
      resetAdminForm();
    }
    setAdminDialogOpen(true);
  };

  const handleCompanySubmit = () => {
    if (!companyForm.name) { showNotification('Введите название компании', 'error'); return; }
    const payload = { name: companyForm.name };
    selectedCompany ? updateCompanyMutation.mutate({ id: selectedCompany.id, data: payload }) : createCompanyMutation.mutate(payload);
  };

  const handleAdminSubmit = () => {
    if (!adminForm.username || !adminForm.password || !adminForm.fullName || !adminForm.companyId) {
      showNotification('Заполните все поля', 'error'); return;
    }
    createAdminMutation.mutate(adminForm);
  };

  const handleDeleteCompany = () => {
    if (selectedCompany) deleteCompanyMutation.mutate(selectedCompany.id);
  };

  const handleDeleteAdmin = () => {
    if (selectedAdmin) deleteEmployeeMutation.mutate(selectedAdmin.id);
  };

  const getEmployeeCompanyName = (emp) => {
    if (!emp.companyId) return '-';
    const company = companiesData.find(c => c.id === emp.companyId);
    return company ? company.name : `#${emp.companyId}`;
  };

  const companyAdmins = employeesData.filter(emp => {
    const roles = Array.isArray(emp.roles) ? emp.roles : [];
    return roles.includes('ROLE_ADMIN') || roles.includes('ROLE_GOD');
  });

  // ---- Render ----
  const renderCompaniesTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2} flexDirection={{ xs: 'column', sm: 'row' }} gap={1}>
        <Typography variant="h6">Управление компаниями</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => openCompanyDialog()} fullWidth={false}>Добавить компанию</Button>
      </Box>
      {companiesData.length === 0 && <Typography>Нет компаний</Typography>}
      {companiesData.length > 0 && (
        <Box sx={{ overflowX: 'auto' }}>
          <TableContainer component={Paper}><Table size="small">
            <TableHead><TableRow>
              <TableCell sx={{ fontWeight: 600, minWidth: 80 }}>ID</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 200 }}>Название</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 180 }}>Дата создания</TableCell>
              <TableCell sx={{ fontWeight: 600, textAlign: 'center', minWidth: 120 }}>Действия</TableCell>
            </TableRow></TableHead>
            <TableBody>
              {companiesData.map((c) => (
                <TableRow key={c.id}>
                  <TableCell>{c.id}</TableCell>
                  <TableCell>{c.name}</TableCell>
                  <TableCell>{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '-'}</TableCell>
                  <TableCell sx={{ textAlign: 'center' }}>
                    <IconButton size="small" onClick={() => openCompanyDialog(c)}><Edit /></IconButton>
                    <IconButton size="small" color="error" onClick={() => { setSelectedCompany(c); setCompanyDeleteDialogOpen(true); }}><Delete /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table></TableContainer>
        </Box>
      )}
    </Box>
  );

  const renderCompanyAdminsTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2} flexDirection={{ xs: 'column', sm: 'row' }} gap={1}>
        <Typography variant="h6">Администраторы компаний</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => openAdminDialog()} fullWidth={false}>Добавить администратора</Button>
      </Box>
      {companyAdmins.length === 0 && <Typography>Нет администраторов</Typography>}
      {companyAdmins.length > 0 && (
        <Box sx={{ overflowX: 'auto' }}>
          <TableContainer component={Paper}><Table size="small">
            <TableHead><TableRow>
              <TableCell sx={{ fontWeight: 600, minWidth: 80 }}>ID</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 180 }}>ФИО</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 140 }}>Логин</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 200 }}>Email</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 180 }}>Компания</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 120 }}>Роли</TableCell>
              <TableCell sx={{ fontWeight: 600, textAlign: 'center', minWidth: 100 }}>Действия</TableCell>
            </TableRow></TableHead>
            <TableBody>
              {companyAdmins.map((emp) => (
                <TableRow key={emp.id}>
                  <TableCell>{emp.id}</TableCell>
                  <TableCell>{emp.fullName || '-'}</TableCell>
                  <TableCell>{emp.username || '-'}</TableCell>
                  <TableCell>{emp.email || '-'}</TableCell>
                  <TableCell>{getEmployeeCompanyName(emp)}</TableCell>
                  <TableCell>
                    {(Array.isArray(emp.roles) ? emp.roles : []).map(r => (
                      <Chip key={r} label={r.replace('ROLE_', '')} size="small" sx={{ mr: 0.5 }} />
                    ))}
                  </TableCell>
                  <TableCell sx={{ textAlign: 'center' }}>
                    <IconButton size="small" onClick={() => openAdminDialog(emp)}><Edit /></IconButton>
                    <IconButton size="small" color="error" onClick={() => { setSelectedAdmin(emp); setAdminDeleteDialogOpen(true); }}><Delete /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table></TableContainer>
        </Box>
      )}
    </Box>
  );

  return (
    <Box sx={{ p: { xs: 1, sm: 2, md: 3 } }}>
      <Typography variant="h4" gutterBottom sx={{ fontSize: { xs: '1.5rem', md: '2.125rem' } }}>Admin Dashboard</Typography>
      <Paper sx={{ width: '100%', mt: 2, overflow: 'hidden' }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tab} onChange={(_, v) => setTab(v)} textColor="secondary" indicatorColor="secondary">
            <Tab label="Компании" />
            <Tab label="Администраторы компаний" />
          </Tabs>
        </Box>
        <Box sx={{ p: { xs: 1, sm: 2 } }}>
          {tab === 0 && renderCompaniesTab()}
          {tab === 1 && renderCompanyAdminsTab()}
        </Box>
      </Paper>

      {/* Company Dialog */}
      <Dialog open={companyDialogOpen} onClose={() => setCompanyDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedCompany ? 'Редактировать компанию' : 'Новая компания'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Название компании" value={companyForm.name} onChange={(e) => setCompanyForm({ ...companyForm, name: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCompanyDialogOpen(false)}>Отмена</Button>
          <Button variant="contained" onClick={handleCompanySubmit} disabled={createCompanyMutation.isPending || updateCompanyMutation.isPending}>
            {selectedCompany ? 'Сохранить' : 'Создать'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Company Delete Dialog */}
      <Dialog open={companyDeleteDialogOpen} onClose={() => setCompanyDeleteDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Удалить компанию?</DialogTitle>
        <DialogContent>
          <Typography>Вы уверены, что хотите удалить компанию &quot;{selectedCompany?.name}&quot;?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCompanyDeleteDialogOpen(false)}>Отмена</Button>
          <Button color="error" variant="contained" onClick={handleDeleteCompany} disabled={deleteCompanyMutation.isPending}>Удалить</Button>
        </DialogActions>
      </Dialog>

      {/* Admin Dialog */}
      <Dialog open={adminDialogOpen} onClose={() => setAdminDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedAdmin ? 'Редактировать администратора' : 'Новый администратор компании'}</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="ФИО" value={adminForm.fullName} onChange={(e) => setAdminForm({ ...adminForm, fullName: e.target.value })} />
          <TextField fullWidth margin="dense" label="Логин" value={adminForm.username} onChange={(e) => setAdminForm({ ...adminForm, username: e.target.value })} disabled={!!selectedAdmin} />
          {!selectedAdmin && <TextField fullWidth margin="dense" label="Пароль" type="password" value={adminForm.password} onChange={(e) => setAdminForm({ ...adminForm, password: e.target.value })} />}
          <TextField fullWidth margin="dense" label="Email" value={adminForm.email} onChange={(e) => setAdminForm({ ...adminForm, email: e.target.value })} />
          <FormControl fullWidth margin="dense">
            <InputLabel>Компания</InputLabel>
            <Select value={adminForm.companyId} label="Компания" onChange={(e) => setAdminForm({ ...adminForm, companyId: e.target.value })} disabled={!!selectedAdmin}>
              {companiesData.map(c => <MenuItem key={c.id} value={String(c.id)}>{c.name}</MenuItem>)}
            </Select>
          </FormControl>
          {!selectedAdmin && (
            <FormControl fullWidth margin="dense">
              <InputLabel>Роль</InputLabel>
              <Select value={adminForm.role} label="Роль" onChange={(e) => setAdminForm({ ...adminForm, role: e.target.value })}>
                <MenuItem value="COMPANY_ADMIN">Администратор компании</MenuItem>
                <MenuItem value="ADMIN">Администратор</MenuItem>
              </Select>
            </FormControl>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAdminDialogOpen(false)}>Отмена</Button>
          <Button variant="contained" onClick={handleAdminSubmit} disabled={createAdminMutation.isPending}>
            {selectedAdmin ? 'Сохранить' : 'Создать'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Admin Delete Dialog */}
      <Dialog open={adminDeleteDialogOpen} onClose={() => setAdminDeleteDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Удалить администратора?</DialogTitle>
        <DialogContent>
          <Typography>Вы уверены, что хотите удалить администратора &quot;{selectedAdmin?.fullName || selectedAdmin?.username}&quot;?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAdminDeleteDialogOpen(false)}>Отмена</Button>
          <Button color="error" variant="contained" onClick={handleDeleteAdmin} disabled={deleteEmployeeMutation.isPending}>Удалить</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={notification.open} autoHideDuration={4000} onClose={() => setNotification({ ...notification, open: false })}>
        <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })}>{notification.message}</Alert>
      </Snackbar>
    </Box>
  );
};

export default AdminDashboard;
