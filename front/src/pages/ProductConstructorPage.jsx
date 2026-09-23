import React, { useState, useEffect } from 'react';
import {
  Box, Typography, Paper, Button, Grid, IconButton, Dialog, DialogTitle, DialogContent, DialogActions,
  Table, TableHead, TableRow, TableCell, TableBody, TableContainer, TextField, FormControl, InputLabel, Select, MenuItem, CircularProgress, Alert, Snackbar, useMediaQuery, Card, CardContent, Divider
} from '@mui/material';
import { Add, Delete, Save, ArrowBack } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '@mui/material/styles';
import api from '../services/api';

const ProductConstructorPage = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [materials, setMaterials] = useState([]);
  const [operations, setOperations] = useState([]);
  const [allMaterials, setAllMaterials] = useState([]);
  const [allOperations, setAllOperations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [nameDialogOpen, setNameDialogOpen] = useState(false);
  const [productName, setProductName] = useState('');
  const [saving, setSaving] = useState(false);
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });

  useEffect(() => {
    Promise.all([
      api.get('/api/v1/materials?size=1000'),
      api.get('/api/v1/calculations/operations')
    ]).then(([matRes, opRes]) => {
      setAllMaterials(matRes.data.content || []);
      setAllOperations(opRes.data || []);
    }).catch(err => {
      console.error('Failed to load data', err);
      setNotification({ open: true, message: 'Ошибка загрузки данных', severity: 'error' });
    }).finally(() => setLoading(false));
  }, []);

  const addMaterialRow = () => {
    setMaterials([...materials, {
      id: Date.now() + Math.random(),
      materialId: null,
      name: '',
      price: 0,
      quantity: 1,
      unit: 'шт',
      wasteCoef: 1,
      coefficient: 1,
      cost: 0
    }]);
  };

  const updateMaterial = (id, field, value) => {
    setMaterials(materials.map(row => {
      if (row.id === id) {
        const updated = { ...row, [field]: value };
        if (field === 'materialId') {
          const mat = allMaterials.find(m => m.id === value);
          if (mat) {
            updated.name = mat.name;
            updated.price = Number(mat.price) || 0;
            updated.unit = mat.unit || 'шт';
            updated.wasteCoef = mat.wasteCoefficient || 1;
          }
        }
        if (['materialId', 'price', 'quantity', 'wasteCoef', 'coefficient'].includes(field)) {
          updated.cost = Number((updated.price * updated.quantity * (updated.wasteCoef || 1) * (updated.coefficient || 1)).toFixed(2));
        }
        return updated;
      }
      return row;
    }));
  };

  const deleteMaterial = (id) => {
    setMaterials(materials.filter(row => row.id !== id));
  };

  const addOperationRow = () => {
    setOperations([...operations, {
      id: Date.now() + Math.random(),
      operationId: null,
      name: '',
      pricePerUnit: 0,
      quantity: 1,
      unit: 'шт',
      coefficient: 1,
      cost: 0
    }]);
  };

  const updateOperation = (id, field, value) => {
    setOperations(operations.map(row => {
      if (row.id === id) {
        const updated = { ...row, [field]: value };
        if (field === 'operationId') {
          const op = allOperations.find(o => o.id === value);
          if (op) {
            updated.name = op.name;
            updated.pricePerUnit = Number(op.price) || 0;
            updated.unit = op.unit || 'шт';
          }
        }
        if (['operationId', 'pricePerUnit', 'quantity', 'coefficient'].includes(field)) {
          updated.cost = Number((updated.pricePerUnit * updated.quantity * (updated.coefficient || 1)).toFixed(2));
        }
        return updated;
      }
      return row;
    }));
  };

  const deleteOperation = (id) => {
    setOperations(operations.filter(row => row.id !== id));
  };

  const totalMaterials = materials.reduce((sum, m) => sum + (m.cost || 0), 0);
  const totalOperations = operations.reduce((sum, op) => sum + (op.cost || 0), 0);
  const grandTotal = totalMaterials + totalOperations;

  const handleSaveProduct = async () => {
    if (!productName.trim()) return;
    setSaving(true);
    try {
      const payload = {
        name: productName.trim(),
        article: '',
        description: '',
        width: null,
        height: null,
        unit: 'шт',
        basePrice: grandTotal,
        category: '',
        isActive: true,
        formulaJson: null,
        materials: materials.filter(m => m.materialId).map(m => ({
          materialId: m.materialId,
          quantity: m.quantity,
          wasteCoefficient: m.wasteCoef || 1,
          sortOrder: 0,
          quantityFormula: null
        })),
        operations: operations.filter(o => o.operationId).map(o => ({
          name: o.name,
          pricePerUnit: o.pricePerUnit,
          unit: o.unit,
          sortOrder: 0,
          quantityFormula: null,
          quantity: o.quantity,
          coefficient: o.coefficient || 1
        }))
      };
      await api.post('/api/v1/products', payload);
      setNotification({ open: true, message: 'Изделие сохранено', severity: 'success' });
      setNameDialogOpen(false);
      setProductName('');
      setMaterials([]);
      setOperations([]);
    } catch (err) {
      console.error('Failed to save product', err);
      setNotification({ open: true, message: 'Ошибка сохранения: ' + (err.response?.data?.message || err.message), severity: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const renderMaterialFields = (m) => (
    <Box key={m.id} sx={{ mb: 2, p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
      <Grid container spacing={2} alignItems="flex-start">
        <Grid item xs={12} md={4}>
          <FormControl fullWidth size="small">
            <InputLabel>Материал</InputLabel>
            <Select value={m.materialId || ''} label="Материал" onChange={(e) => updateMaterial(m.id, 'materialId', Number(e.target.value))}>
              <MenuItem value="">Выберите материал</MenuItem>
              {allMaterials.map(mat => <MenuItem key={mat.id} value={mat.id}>{mat.name} — {mat.price?.toFixed(2)} ₽</MenuItem>)}
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={6} md={2}>
          <TextField fullWidth size="small" label="Кол-во" type="number" value={m.quantity} onChange={(e) => updateMaterial(m.id, 'quantity', parseFloat(e.target.value) || 0)} inputProps={{ step: 0.01, min: 0 }} />
        </Grid>
        <Grid item xs={6} md={2}>
          <TextField fullWidth size="small" label="Коэф. отход" type="number" value={m.wasteCoef} onChange={(e) => updateMaterial(m.id, 'wasteCoef', parseFloat(e.target.value) || 1)} inputProps={{ step: 0.1, min: 0 }} />
        </Grid>
        <Grid item xs={6} md={2}>
          <TextField fullWidth size="small" label="Коэф. кол-ва" type="number" value={m.coefficient} onChange={(e) => updateMaterial(m.id, 'coefficient', parseFloat(e.target.value) || 1)} inputProps={{ step: 0.1, min: 0 }} />
        </Grid>
        <Grid item xs={6} md={2} sx={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <Box>
            <Typography variant="caption" color="text.secondary">Ед. изм.</Typography>
            <Typography variant="body2">{m.unit || 'шт'}</Typography>
          </Box>
          <Box sx={{ mt: 1 }}>
            <Typography variant="caption" color="text.secondary">Стоимость</Typography>
            <Typography variant="body2" fontWeight="medium">{m.cost?.toFixed(2) || '0.00'} ₽</Typography>
          </Box>
        </Grid>
        <Grid item xs={12} md={1} sx={{ textAlign: 'right' }}>
          <IconButton color="error" onClick={() => deleteMaterial(m.id)}><Delete /></IconButton>
        </Grid>
      </Grid>
    </Box>
  );

  const renderOperationFields = (op) => (
    <Box key={op.id} sx={{ mb: 2, p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
      <Grid container spacing={2} alignItems="flex-start">
        <Grid item xs={12} md={5}>
          <FormControl fullWidth size="small">
            <InputLabel>Операция</InputLabel>
            <Select value={op.operationId || ''} label="Операция" onChange={(e) => updateOperation(op.id, 'operationId', Number(e.target.value))}>
              <MenuItem value="">Выберите операцию</MenuItem>
              {allOperations.map(o => <MenuItem key={o.id} value={o.id}>{o.name} — {o.price?.toFixed(2)} ₽</MenuItem>)}
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={6} md={2}>
          <TextField fullWidth size="small" label="Кол-во" type="number" value={op.quantity} onChange={(e) => updateOperation(op.id, 'quantity', parseFloat(e.target.value) || 0)} inputProps={{ step: 0.01, min: 0 }} />
        </Grid>
        <Grid item xs={6} md={2}>
          <TextField fullWidth size="small" label="Коэф. кол-ва" type="number" value={op.coefficient} onChange={(e) => updateOperation(op.id, 'coefficient', parseFloat(e.target.value) || 1)} inputProps={{ step: 0.1, min: 0 }} />
        </Grid>
        <Grid item xs={6} md={2} sx={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <Box>
            <Typography variant="caption" color="text.secondary">Ед. изм.</Typography>
            <Typography variant="body2">{op.unit || 'шт'}</Typography>
          </Box>
          <Box sx={{ mt: 1 }}>
            <Typography variant="caption" color="text.secondary">Стоимость</Typography>
            <Typography variant="body2" fontWeight="medium">{op.cost?.toFixed(2) || '0.00'} ₽</Typography>
          </Box>
        </Grid>
        <Grid item xs={6} md={1} sx={{ textAlign: 'right' }}>
          <IconButton color="error" onClick={() => deleteOperation(op.id)}><Delete /></IconButton>
        </Grid>
      </Grid>
    </Box>
  );

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: { xs: 1, sm: 2, md: 3 } }}>
      <Button startIcon={<ArrowBack />} onClick={() => navigate('/admin')} sx={{ mb: 2 }}>
        Назад в админ панель
      </Button>
      <Typography variant="h4" gutterBottom sx={{ fontSize: { xs: '1.5rem', md: '2.125rem' } }}>Конструктор изделий</Typography>
      <Paper sx={{ p: { xs: 1, sm: 2, md: 3 } }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2} flexDirection={{ xs: 'column', sm: 'row' }} gap={1}>
          <Typography variant="h6">Материалы и работы</Typography>
          <Box display="flex" gap={1} width={{ xs: '100%', sm: 'auto' }} flexDirection={{ xs: 'column', sm: 'row' }}>
            <Button variant="contained" startIcon={<Add />} onClick={addMaterialRow} fullWidth={isMobile}>Добавить материал</Button>
            <Button variant="outlined" startIcon={<Add />} onClick={addOperationRow} fullWidth={isMobile}>Добавить операцию</Button>
          </Box>
        </Box>

        {materials.length === 0 && operations.length === 0 && (
          <Alert severity="info" sx={{ mb: 2 }}>Нет позиций. Нажмите "Добавить материал" или "Добавить операцию"</Alert>
        )}

        {materials.length > 0 && (
          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle1" sx={{ mb: 1, fontWeight: 600 }}>Материалы</Typography>
            {!isMobile ? (
              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ minWidth: 200 }}>Материал</TableCell>
                      <TableCell sx={{ minWidth: 80 }}>Кол-во</TableCell>
                      <TableCell sx={{ minWidth: 100 }}>Коэф. отход</TableCell>
                      <TableCell sx={{ minWidth: 120 }}>Коэф. кол-ва</TableCell>
                      <TableCell>Ед. изм.</TableCell>
                      <TableCell>Стоимость</TableCell>
                      <TableCell align="right" sx={{ width: 80 }}></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {materials.map((m) => (
                      <TableRow key={m.id}>
                        <TableCell sx={{ minWidth: 200 }}>
                          <FormControl fullWidth size="small">
                            <InputLabel>Материал</InputLabel>
                            <Select value={m.materialId || ''} label="Материал" onChange={(e) => updateMaterial(m.id, 'materialId', Number(e.target.value))}>
                              <MenuItem value="">Выберите материал</MenuItem>
                              {allMaterials.map(mat => <MenuItem key={mat.id} value={mat.id}>{mat.name} — {mat.price?.toFixed(2)} ₽</MenuItem>)}
                            </Select>
                          </FormControl>
                        </TableCell>
                        <TableCell sx={{ minWidth: 80 }}>
                          <TextField size="small" type="number" value={m.quantity} onChange={(e) => updateMaterial(m.id, 'quantity', parseFloat(e.target.value) || 0)} inputProps={{ step: 0.01, min: 0 }} />
                        </TableCell>
                        <TableCell sx={{ minWidth: 100 }}>
                          <TextField size="small" type="number" value={m.wasteCoef} onChange={(e) => updateMaterial(m.id, 'wasteCoef', parseFloat(e.target.value) || 1)} inputProps={{ step: 0.1, min: 0 }} />
                        </TableCell>
                        <TableCell sx={{ minWidth: 120 }}>
                          <TextField size="small" type="number" value={m.coefficient} onChange={(e) => updateMaterial(m.id, 'coefficient', parseFloat(e.target.value) || 1)} inputProps={{ step: 0.1, min: 0 }} />
                        </TableCell>
                        <TableCell>{m.unit}</TableCell>
                        <TableCell>{m.cost?.toFixed(2) || '0.00'} ₽</TableCell>
                        <TableCell align="right">
                          <IconButton color="error" onClick={() => deleteMaterial(m.id)}><Delete /></IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Box>{materials.map(renderMaterialFields)}</Box>
            )}
            <Typography variant="subtitle1" align="right" sx={{ mt: 1 }}>
              Итого материалы: <strong>{totalMaterials.toFixed(2)} ₽</strong>
            </Typography>
          </Box>
        )}

        {operations.length > 0 && (
          <Box>
            <Typography variant="subtitle1" sx={{ mb: 1, fontWeight: 600 }}>Работы / Операции</Typography>
            {!isMobile ? (
               <TableContainer component={Paper} variant="outlined">
                 <Table size="small">
                   <TableHead>
                     <TableRow>
                       <TableCell sx={{ minWidth: 200 }}>Операция</TableCell>
                       <TableCell sx={{ minWidth: 80 }}>Кол-во</TableCell>
                       <TableCell sx={{ minWidth: 120 }}>Коэф. кол-ва</TableCell>
                       <TableCell>Ед. изм.</TableCell>
                       <TableCell>Стоимость</TableCell>
                       <TableCell align="right" sx={{ width: 80 }}></TableCell>
                     </TableRow>
                   </TableHead>
                  <TableBody>
                    {operations.map((op) => (
                      <TableRow key={op.id}>
                        <TableCell sx={{ minWidth: 200 }}>
                          <FormControl fullWidth size="small">
                            <InputLabel>Операция</InputLabel>
                            <Select value={op.operationId || ''} label="Операция" onChange={(e) => updateOperation(op.id, 'operationId', Number(e.target.value))}>
                              <MenuItem value="">Выберите операцию</MenuItem>
                              {allOperations.map(o => <MenuItem key={o.id} value={o.id}>{o.name} — {o.price?.toFixed(2)} ₽</MenuItem>)}
                            </Select>
                          </FormControl>
                        </TableCell>
                        <TableCell sx={{ minWidth: 80 }}>
                          <TextField size="small" type="number" value={op.quantity} onChange={(e) => updateOperation(op.id, 'quantity', parseFloat(e.target.value) || 0)} inputProps={{ step: 0.01, min: 0 }} />
                        </TableCell>
                        <TableCell sx={{ minWidth: 120 }}>
                          <TextField size="small" type="number" value={op.coefficient} onChange={(e) => updateOperation(op.id, 'coefficient', parseFloat(e.target.value) || 1)} inputProps={{ step: 0.1, min: 0 }} />
                        </TableCell>
                        <TableCell>{op.unit}</TableCell>
                        <TableCell>{op.cost?.toFixed(2) || '0.00'} ₽</TableCell>
                        <TableCell align="right">
                          <IconButton color="error" onClick={() => deleteOperation(op.id)}><Delete /></IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Box>{operations.map(renderOperationFields)}</Box>
            )}
            <Typography variant="subtitle1" align="right" sx={{ mt: 1 }}>
              Итого работы: <strong>{totalOperations.toFixed(2)} ₽</strong>
            </Typography>
          </Box>
        )}

        <Grid container justifyContent="space-between" alignItems="center" sx={{ mt: 4, pt: 3, borderTop: '2px solid', borderColor: 'primary.main' }} flexDirection={{ xs: 'column', sm: 'row' }} gap={2}>
          <Typography variant="h4" sx={{ fontSize: { xs: '1.25rem', md: '2.125rem' } }}>
            Общий итог: <strong style={{ color: '#1976d2' }}>{grandTotal.toFixed(2)} ₽</strong>
          </Typography>
          <Button variant="contained" color="success" size="large" startIcon={<Save />} onClick={() => setNameDialogOpen(true)} fullWidth={isMobile}>
            Добавить изделие
          </Button>
        </Grid>
      </Paper>

      <Dialog open={nameDialogOpen} onClose={() => setNameDialogOpen(false)} maxWidth="xs" fullWidth fullScreen={isMobile}>
        <DialogTitle>Добавить изделие</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Название изделия" value={productName} onChange={(e) => setProductName(e.target.value)} />
          <Alert severity="info" sx={{ mt: 2 }}>
            Общая ориентировочная стоимость: <strong>{grandTotal.toFixed(2)} ₽</strong>
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNameDialogOpen(false)} disabled={saving}>Отмена</Button>
          <Button onClick={handleSaveProduct} variant="contained" disabled={saving || !productName.trim()}>{saving ? 'Сохранение...' : 'Добавить'}</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={notification.open} autoHideDuration={3000} onClose={() => setNotification({ ...notification, open: false })} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert severity={notification.severity} onClose={() => setNotification({ ...notification, open: false })} sx={{ width: '100%' }}>{notification.message}</Alert>
      </Snackbar>
    </Box>
  );
};

export default ProductConstructorPage;
