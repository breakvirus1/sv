import { useState } from 'react';
import {
  Box, Typography, Paper, TextField, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  Table, TableHead, TableRow, TableCell, TableBody, IconButton, MenuItem, Select, FormControl, InputLabel,
  Snackbar, Alert, CircularProgress, Divider, Chip, Tabs, Tab, TableContainer,
  InputAdornment, Checkbox, FormControlLabel, FormHelperText
} from '@mui/material';
import { Add, Edit, Delete, Backup, VpnKey, Visibility, VisibilityOff, Autorenew } from '@mui/icons-material';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { getApiErrorMessage } from '../utils/apiError';
import { useCompanies, useEmployees } from '../hooks/useSharedQueries';
import { queryKeys } from '../hooks/queryKeys';

const PASSWORD_MIN_LENGTH = 6;
const PASSWORD_MAX_LENGTH = 100;

// Без похожих друг на друга символов (0/O, 1/l), чтобы пароль можно было продиктовать
const randomPassword = () => {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
  const bytes = new Uint8Array(12);
  window.crypto.getRandomValues(bytes);
  return Array.from(bytes, (byte) => alphabet[byte % alphabet.length]).join('');
};

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
  const [adminForm, setAdminForm] = useState({ username: '', password: '', fullName: '', email: '', companyId: '', role: 'ADMIN' });

  // ---- Смена пароля (Keycloak хранит только хеш, поэтому пароль сбрасывается, а не показывается) ----
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [passwordTarget, setPasswordTarget] = useState(null);
  const [passwordForm, setPasswordForm] = useState({ password: '', temporary: false });
  const [showPassword, setShowPassword] = useState(false);

  // ---- Fetch data ----
  const { data: companiesData = [], isLoading: isLoadingCompanies, refetch: refetchCompanies } = useCompanies();
  const { data: employeesData = [], isLoading: isLoadingEmployees, refetch: refetchEmployees } = useEmployees({ enabled: true });

  // ---- Company CRUD ----
  const createCompanyMutation = useMutation({
    mutationFn: (c) => api.post('/api/v1/companies', c),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: queryKeys.companies('list') }); setCompanyDialogOpen(false); showNotification('Компания создана'); resetCompanyForm(); },
    onError: (err) => showNotification('Ошибка: ' + getApiErrorMessage(err), 'error')
  });
  const updateCompanyMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/v1/companies/${id}`, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: queryKeys.companies('list') }); setCompanyDialogOpen(false); showNotification('Компания обновлена'); resetCompanyForm(); },
    onError: (err) => showNotification('Ошибка: ' + getApiErrorMessage(err), 'error')
  });
  const deleteCompanyMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/companies/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: queryKeys.companies('list') }); setCompanyDeleteDialogOpen(false); showNotification('Компания удалена'); },
    onError: (err) => showNotification('Ошибка: ' + getApiErrorMessage(err), 'error')
  });

  // ---- Company Admin creation (через employee-service: креды Keycloak не попадают в браузер) ----
  const createAdminMutation = useMutation({
    mutationFn: async (adminData) => {
      await api.post('/api/v1/employees/keycloak-user', {
        username: adminData.username,
        password: adminData.password,
        fullName: adminData.fullName,
        email: adminData.email,
        role: adminData.role,
        companyId: adminData.companyId ? Number(adminData.companyId) : undefined
      });
      await api.post(`/api/v1/employees/sync-by-company?companyId=${adminData.companyId}`);
      return true;
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: queryKeys.employees({ size: 100 }) }); setAdminDialogOpen(false); showNotification('Администратор компании создан'); resetAdminForm(); },
    onError: (err) => showNotification('Ошибка: ' + getApiErrorMessage(err), 'error')
  });

  const deleteEmployeeMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/v1/employees/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: queryKeys.employees({ size: 100 }) }); setAdminDeleteDialogOpen(false); showNotification('Администратор удален'); },
    onError: (err) => showNotification('Ошибка: ' + getApiErrorMessage(err), 'error')
  });

  // ---- Password reset via employee-service ----
  const updatePasswordMutation = useMutation({
    mutationFn: ({ username, password, temporary }) =>
      api.put('/api/v1/employees/keycloak-password', { username, password, temporary }),
    onSuccess: () => {
      showNotification(`Пароль обновлён: ${passwordTarget?.username || ''}`);
      closePasswordDialog();
    },
    onError: (err) => showNotification('Ошибка: ' + getApiErrorMessage(err), 'error')
  });

  // ---- Backup mutation ----
  const backupMutation = useMutation({
    mutationFn: (companyId) => api.post(`/api/v1/companies/${companyId}/backup`, null, {
      responseType: 'blob'
    }),
    onSuccess: (response, companyId) => {
      const blob = new Blob([response.data], { type: 'application/sql' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `svdb_company_${companyId}_${Date.now()}.sql`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      showNotification('Бэкап компании успешно скачан');
    },
    onError: (err) => showNotification('Ошибка бэкапа: ' + getApiErrorMessage(err), 'error')
  });

  // ---- Handlers ----
  const resetCompanyForm = () => { setCompanyForm({ name: '' }); setSelectedCompany(null); };
  const resetAdminForm = () => { setAdminForm({ username: '', password: '', fullName: '', email: '', companyId: '', role: 'ADMIN' }); setSelectedAdmin(null); };

  const openCompanyDialog = (company = null) => {
    if (company) { setSelectedCompany(company); setCompanyForm({ name: company.name || '' }); } else { resetCompanyForm(); }
    setCompanyDialogOpen(true);
  };

  const openAdminDialog = (emp = null) => {
    if (emp) {
      setSelectedAdmin(emp);
      setAdminForm({ username: emp.username || '', password: '', fullName: emp.fullName || '', email: emp.email || '', companyId: emp.companyId ? String(emp.companyId) : '', role: 'ADMIN' });
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

  const openPasswordDialog = (emp) => {
    setPasswordTarget(emp);
    setPasswordForm({ password: '', temporary: false });
    setShowPassword(false);
    setPasswordDialogOpen(true);
  };

  const closePasswordDialog = () => {
    setPasswordDialogOpen(false);
    setPasswordTarget(null);
    setPasswordForm({ password: '', temporary: false });
    setShowPassword(false);
  };

  const generatePassword = () => {
    setPasswordForm((form) => ({ ...form, password: randomPassword() }));
    setShowPassword(true);
  };

  const handlePasswordSubmit = () => {
    if (!passwordTarget?.username) {
      showNotification('Не известен логин пользователя', 'error');
      return;
    }
    if (passwordForm.password.length < PASSWORD_MIN_LENGTH) {
      showNotification(`Пароль должен быть не короче ${PASSWORD_MIN_LENGTH} символов`, 'error');
      return;
    }
    updatePasswordMutation.mutate({
      username: passwordTarget.username,
      password: passwordForm.password,
      temporary: passwordForm.temporary
    });
  };

  const getEmployeeCompanyName = (emp) => {
    if (!emp.companyId) return '-';
    const company = companiesData.find(c => c.id === emp.companyId);
    return company ? company.name : `#${emp.companyId}`;
  };

  const companyAdmins = employeesData.filter(emp => {
    const roles = Array.isArray(emp.roles) ? emp.roles : [];
    return roles.includes('ROLE_ADMIN');
  });

  const isLoading = isLoadingCompanies || isLoadingEmployees;

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
              <TableCell sx={{ fontWeight: 600, minWidth: 100 }}>Бэкап</TableCell>
              <TableCell sx={{ fontWeight: 600, textAlign: 'center', minWidth: 120 }}>Действия</TableCell>
            </TableRow></TableHead>
            <TableBody>
              {companiesData.map((c) => (
                <TableRow key={c.id}>
                  <TableCell>{c.id}</TableCell>
                  <TableCell>{c.name}</TableCell>
                  <TableCell>{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '-'}</TableCell>
                  <TableCell>
                    <IconButton
                      size="small"
                      color="primary"
                      onClick={() => backupMutation.mutate(c.id)}
                      disabled={backupMutation.isPending}
                      title="Скачать бэкап базы данных"
                    >
                      {backupMutation.isPending && backupMutation.variables === c.id
                        ? <CircularProgress size={20} />
                        : <Backup />}
                    </IconButton>
                  </TableCell>
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
                    <IconButton size="small" onClick={() => openPasswordDialog(emp)} title="Сменить пароль" aria-label={`Сменить пароль для ${emp.username || ''}`}>
                      <VpnKey />
                    </IconButton>
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
    <Box sx={{ p: { xs: 1, sm: 2, md: 3 }, position: 'relative' }}>
      {isLoading && (
        <Box
          sx={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            bgcolor: 'background.paper',
            zIndex: 10,
          }}
        >
          <CircularProgress size={40} />
        </Box>
      )}
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
              <Select value={adminForm.role} label="Роль" onChange={(e) => setAdminForm({ ...adminForm, role: e.target.value })} disabled>
                <MenuItem value="ADMIN">Администратор компании</MenuItem>
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

      {/* Password Dialog */}
      <Dialog open={passwordDialogOpen} onClose={closePasswordDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Пароль: {passwordTarget?.fullName || passwordTarget?.username}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            Посмотреть текущий пароль нельзя — Keycloak хранит только его хеш. Можно установить новый.
          </Typography>
          <TextField autoFocus fullWidth margin="dense" label="Логин" value={passwordTarget?.username || ''} InputProps={{ readOnly: true }} />
          <TextField
            fullWidth
            margin="dense"
            label="Новый пароль"
            required
            type={showPassword ? 'text' : 'password'}
            value={passwordForm.password}
            onChange={(e) => setPasswordForm({ ...passwordForm, password: e.target.value.slice(0, PASSWORD_MAX_LENGTH) })}
            helperText={`От ${PASSWORD_MIN_LENGTH} до ${PASSWORD_MAX_LENGTH} символов`}
            onKeyDown={(e) => { if (e.key === 'Enter') handlePasswordSubmit(); }}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    onClick={() => setShowPassword((prev) => !prev)}
                    edge="end"
                    aria-label={showPassword ? 'Скрыть пароль' : 'Показать пароль'}
                  >
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                  <IconButton onClick={generatePassword} edge="end" aria-label="Сгенерировать пароль" title="Сгенерировать">
                    <Autorenew />
                  </IconButton>
                </InputAdornment>
              )
            }}
          />
          <FormControlLabel
            control={(
              <Checkbox
                checked={passwordForm.temporary}
                onChange={(e) => setPasswordForm({ ...passwordForm, temporary: e.target.checked })}
              />
            )}
            label="Временный — потребовать смену при первом входе"
          />
          <FormHelperText>Активные сессии пользователя после смены пароля не отзываются.</FormHelperText>
        </DialogContent>
        <DialogActions>
          <Button onClick={closePasswordDialog}>Отмена</Button>
          <Button variant="contained" onClick={handlePasswordSubmit} disabled={updatePasswordMutation.isPending}>
            {updatePasswordMutation.isPending ? <CircularProgress size={20} /> : 'Сменить пароль'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={notification.open} autoHideDuration={4000} onClose={() => setNotification({ ...notification, open: false })}>
        <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })}>{notification.message}</Alert>
      </Snackbar>
    </Box>
  );
};

export default AdminDashboard;
