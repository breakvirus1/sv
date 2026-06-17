import { useState } from 'react';
import { Box, Typography, Grid, Button, Alert, TextField, CircularProgress, Dialog, DialogTitle, DialogContent, DialogActions } from '@mui/material';
import { Refresh, Delete } from '@mui/icons-material';

const TABLES = [
  { key: 'clients', label: 'Клиенты', endpoint: 'clients' },
  { key: 'materials', label: 'Материалы', endpoint: 'materials' },
  { key: 'operations', label: 'Операции', endpoint: 'operations' },
  { key: 'workshops', label: 'Цеха', endpoint: 'workshops' },
  { key: 'employees', label: 'Сотрудники', endpoint: 'employees' },
  { key: 'orders', label: 'Заказы', endpoint: 'orders' },
];

const GenerateTab = ({ onGenerate, onDelete }) => {
  const [counts, setCounts] = useState({
    clients: 20,
    materials: 20,
    operations: 20,
    workshops: 5,
    employees: 20,
    orders: 20,
  });
  const [generating, setGenerating] = useState({});
  const [deleting, setDeleting] = useState({});
  const [deleteDialog, setDeleteDialog] = useState({ open: false, key: null, label: '' });

  const handleCountChange = (key) => (e) => {
    const val = parseInt(e.target.value, 10) || 0;
    setCounts((prev) => ({ ...prev, [key]: val }));
  };

  const handleGenerate = (endpoint, key) => {
    const count = counts[key];
    if (count <= 0) return;
    setGenerating((prev) => ({ ...prev, [key]: true }));
    onGenerate(endpoint, count).finally(() => {
      setGenerating((prev) => ({ ...prev, [key]: false }));
    });
  };

  const handleDeleteClick = (key, label) => {
    setDeleteDialog({ open: true, key, label });
  };

  const handleDeleteConfirm = () => {
    const { key } = deleteDialog;
    setDeleteDialog({ open: false, key: null, label: '' });
    setDeleting((prev) => ({ ...prev, [key]: true }));
    onDelete(key).finally(() => {
      setDeleting((prev) => ({ ...prev, [key]: false }));
    });
  };

  const handleDeleteCancel = () => {
    setDeleteDialog({ open: false, key: null, label: '' });
  };

  return (
    <Box>
      <Typography variant="h6" gutterBottom>Генерация тестовых данных</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Укажите количество записей для каждой таблицы и нажмите кнопку генерации.
      </Typography>
      <Grid container spacing={2}>
        {TABLES.map(({ key, label }) => (
          <Grid item xs={12} sm={6} md={4} key={key}>
            <Box display="flex" gap={1} alignItems="center">
              <TextField
                type="number"
                size="small"
                label={label}
                value={counts[key]}
                onChange={handleCountChange(key)}
                inputProps={{ min: 1, max: 1000 }}
                sx={{ width: 100 }}
              />
              <Button
                variant="contained"
                fullWidth
                startIcon={generating[key] ? <CircularProgress size={18} color="inherit" /> : <Refresh />}
                onClick={() => handleGenerate(key, key)}
                disabled={generating[key] || counts[key] <= 0}
              >
                {generating[key] ? '...' : 'Сгенерировать'}
              </Button>
              <Button
                variant="outlined"
                color="error"
                startIcon={deleting[key] ? <CircularProgress size={18} color="inherit" /> : <Delete />}
                onClick={() => handleDeleteClick(key, label)}
                disabled={deleting[key]}
                sx={{ minWidth: 40 }}
              >
                {deleting[key] ? '' : 'Удалить'}
              </Button>
            </Box>
          </Grid>
        ))}
      </Grid>
      <Alert severity="info" sx={{ mt: 3 }}>
        Для генерации заказов необходимо иметь хотя бы одного клиента и сотрудника. Рекомендуется сначала сгенерировать их.
        Для генерации сотрудников и цехов желательно наличие цехов и операций соответственно.
      </Alert>

      <Dialog open={deleteDialog.open} onClose={handleDeleteCancel}>
        <DialogTitle>Подтверждение удаления</DialogTitle>
        <DialogContent>
          <Typography>
            Вы уверены, что хотите удалить все записи из таблицы «{deleteDialog.label}»?
            Это действие необратимо.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel}>Отмена</Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained">Удалить</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default GenerateTab;
