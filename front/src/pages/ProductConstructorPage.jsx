import React, { useState, useEffect } from 'react';
import {
  Box, Typography, Paper, Button, Grid, IconButton, Dialog, DialogTitle, DialogContent, DialogActions,
  Table, TableHead, TableRow, TableCell, TableBody, TableContainer, TextField, FormControl, InputLabel, Select, MenuItem, CircularProgress, Alert, Snackbar, useMediaQuery
} from '@mui/material';
import { Add, Delete, Save, ArrowBack } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '@mui/material/styles';
import api from '../services/api';
import { getApiErrorMessage } from '../utils/apiError';

const round2 = (value) => Number((Number(value) || 0).toFixed(2));

const ProductConstructorPage = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [allMaterials, setAllMaterials] = useState([]);
  const [allOperations, setAllOperations] = useState([]);
  const [positions, setPositions] = useState([]);
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

  const createOperationRow = () => ({
    key: Date.now() + Math.random(),
    operationId: '',
    name: '',
    pricePerUnit: 0,
    unit: 'шт',
    quantity: 1,
    coefficient: 1
  });

  const addPosition = () => {
    setPositions(prev => [...prev, {
      key: Date.now() + Math.random(),
      materialId: '',
      name: '',
      price: 0,
      unit: 'шт',
      quantity: 1,
      wasteCoefficient: 1,
      operations: []
    }]);
  };

  const deletePosition = (positionKey) => {
    setPositions(prev => prev.filter(p => p.key !== positionKey));
  };

  const updatePosition = (positionKey, field, value) => {
    setPositions(prev => prev.map(position => {
      if (position.key !== positionKey) return position;
      const updated = { ...position, [field]: value };
      if (field === 'materialId') {
        const material = allMaterials.find(m => m.id === value);
        if (material) {
          updated.name = material.name;
          updated.price = Number(material.price) || 0;
          updated.unit = material.unit || 'шт';
        }
      }
      return updated;
    }));
  };

  const addOperationToPosition = (positionKey) => {
    setPositions(prev => prev.map(position => (
      position.key === positionKey
        ? { ...position, operations: [...position.operations, createOperationRow()] }
        : position
    )));
  };

  const deleteOperationFromPosition = (positionKey, operationKey) => {
    setPositions(prev => prev.map(position => (
      position.key === positionKey
        ? { ...position, operations: position.operations.filter(op => op.key !== operationKey) }
        : position
    )));
  };

  const updateOperation = (positionKey, operationKey, field, value) => {
    setPositions(prev => prev.map(position => {
      if (position.key !== positionKey) return position;
      return {
        ...position,
        operations: position.operations.map(op => {
          if (op.key !== operationKey) return op;
          const updated = { ...op, [field]: value };
          if (field === 'operationId') {
            const operation = allOperations.find(o => o.id === value);
            if (operation) {
              updated.name = operation.name;
              updated.pricePerUnit = Number(operation.price) || 0;
              updated.unit = operation.unit || 'шт';
            }
          }
          return updated;
        })
      };
    }));
  };

  const operationCost = (operation) => (
    (Number(operation.pricePerUnit) || 0)
    * (Number(operation.quantity) || 0)
    * (Number(operation.coefficient) || 1)
  );

  const materialCost = (position) => (
    (Number(position.price) || 0)
    * (Number(position.quantity) || 0)
    * (Number(position.wasteCoefficient) || 1)
  );

  const positionSum = (position) => round2(
    materialCost(position)
    + position.operations.reduce((sum, operation) => sum + operationCost(operation), 0)
  );

  const grandTotal = round2(positions.reduce((sum, position) => sum + positionSum(position), 0));

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
        positions: positions.filter(p => p.materialId).map((position, positionIndex) => ({
          materialId: position.materialId,
          quantity: Number(position.quantity) || 0,
          wasteCoefficient: Number(position.wasteCoefficient) || 1,
          sortOrder: positionIndex,
          quantityFormula: null,
          operations: position.operations.filter(op => op.operationId).map((operation, operationIndex) => ({
            name: operation.name,
            pricePerUnit: Number(operation.pricePerUnit) || 0,
            unit: operation.unit,
            sortOrder: operationIndex,
            quantityFormula: null,
            quantity: Number(operation.quantity) || 0,
            coefficient: Number(operation.coefficient) || 1
          }))
        }))
      };
      await api.post('/api/v1/products', payload);
      setNotification({ open: true, message: 'Изделие сохранено', severity: 'success' });
      setNameDialogOpen(false);
      setProductName('');
      setPositions([]);
    } catch (err) {
      console.error('Failed to save product', err);
      setNotification({ open: true, message: 'Ошибка сохранения: ' + (getApiErrorMessage(err)), severity: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const renderOperations = (position) => (
    <Box sx={{ mt: 2, pl: { xs: 0, md: 2 } }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1} flexDirection={{ xs: 'column', sm: 'row' }} gap={1}>
        <Typography variant="subtitle2" fontWeight={600}>Операции позиции</Typography>
        <Button size="small" variant="outlined" startIcon={<Add />} onClick={() => addOperationToPosition(position.key)}>
          Добавить операцию
        </Button>
      </Box>
      {position.operations.length === 0 && (
        <Typography variant="body2" color="text.secondary">Операции не добавлены</Typography>
      )}
      {position.operations.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={{ minWidth: 200 }}>Операция</TableCell>
                <TableCell sx={{ minWidth: 90 }}>Кол-во</TableCell>
                <TableCell sx={{ minWidth: 120 }}>Коэф. кол-ва</TableCell>
                <TableCell sx={{ minWidth: 80 }}>Ед. изм.</TableCell>
                <TableCell sx={{ width: 60 }} />
              </TableRow>
            </TableHead>
            <TableBody>
              {position.operations.map(operation => (
                <TableRow key={operation.key}>
                  <TableCell>
                    <FormControl fullWidth size="small">
                      <InputLabel>Операция</InputLabel>
                      <Select
                        value={operation.operationId || ''}
                        label="Операция"
                        onChange={(e) => updateOperation(position.key, operation.key, 'operationId', Number(e.target.value))}
                      >
                        <MenuItem value="">Выберите операцию</MenuItem>
                        {allOperations.map(op => (
                          <MenuItem key={op.id} value={op.id}>{op.name} — {Number(op.price || 0).toFixed(2)} ₽</MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </TableCell>
                  <TableCell>
                    <TextField
                      size="small"
                      type="number"
                      value={operation.quantity}
                      onChange={(e) => updateOperation(position.key, operation.key, 'quantity', parseFloat(e.target.value) || 0)}
                      inputProps={{ step: 0.01, min: 0, 'aria-label': 'Кол-во операции' }}
                    />
                  </TableCell>
                  <TableCell>
                    <TextField
                      size="small"
                      type="number"
                      value={operation.coefficient}
                      onChange={(e) => updateOperation(position.key, operation.key, 'coefficient', parseFloat(e.target.value) || 1)}
                      inputProps={{ step: 0.1, min: 0, 'aria-label': 'Коэф. кол-ва операции' }}
                    />
                  </TableCell>
                  <TableCell>{operation.unit || 'шт'}</TableCell>
                  <TableCell>
                    <IconButton color="error" onClick={() => deleteOperationFromPosition(position.key, operation.key)}>
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
          <Typography variant="h6">Позиции изделия (материал + операции)</Typography>
          <Button variant="contained" startIcon={<Add />} onClick={addPosition} fullWidth={isMobile}>
            Добавить позицию
          </Button>
        </Box>

        {positions.length === 0 && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Нет позиций. Нажмите "Добавить позицию", выберите материал и добавьте операции к этой позиции.
          </Alert>
        )}

        {positions.map((position, index) => (
          <Paper key={position.key} variant="outlined" sx={{ p: { xs: 1, sm: 2 }, mb: 2 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2} gap={1} flexDirection={{ xs: 'column', sm: 'row' }}>
              <Typography variant="subtitle1" fontWeight={600}>Позиция {index + 1}</Typography>
              <Box display="flex" alignItems="center" gap={2}>
                <Typography variant="subtitle1" data-testid={`position-sum-${index}`}>
                  Сумма позиции: <strong>{positionSum(position).toFixed(2)} ₽</strong>
                </Typography>
                <IconButton color="error" onClick={() => deletePosition(position.key)}>
                  <Delete />
                </IconButton>
              </Box>
            </Box>

            <Grid container spacing={2} alignItems="flex-start">
              <Grid item xs={12} md={4}>
                <FormControl fullWidth size="small">
                  <InputLabel>Материал</InputLabel>
                  <Select
                    value={position.materialId || ''}
                    label="Материал"
                    onChange={(e) => updatePosition(position.key, 'materialId', Number(e.target.value))}
                  >
                    <MenuItem value="">Выберите материал</MenuItem>
                    {allMaterials.map(mat => (
                      <MenuItem key={mat.id} value={mat.id}>{mat.name} — {Number(mat.price || 0).toFixed(2)} ₽</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={6} md={2}>
                <TextField
                  fullWidth
                  size="small"
                  label="Кол-во"
                  type="number"
                  value={position.quantity}
                  onChange={(e) => updatePosition(position.key, 'quantity', parseFloat(e.target.value) || 0)}
                  inputProps={{ step: 0.01, min: 0 }}
                />
              </Grid>
              <Grid item xs={6} md={2}>
                <TextField
                  fullWidth
                  size="small"
                  label="Коэф. отхода"
                  type="number"
                  value={position.wasteCoefficient}
                  onChange={(e) => updatePosition(position.key, 'wasteCoefficient', parseFloat(e.target.value) || 1)}
                  inputProps={{ step: 0.1, min: 0 }}
                />
              </Grid>
              <Grid item xs={6} md={2}>
                <Typography variant="caption" color="text.secondary">Ед. изм.</Typography>
                <Typography variant="body2">{position.unit || 'шт'}</Typography>
              </Grid>
            </Grid>

            {renderOperations(position)}
          </Paper>
        ))}

        <Grid
          container
          justifyContent="space-between"
          alignItems="center"
          sx={{ mt: 4, pt: 3, borderTop: '2px solid', borderColor: 'primary.main' }}
          flexDirection={{ xs: 'column', sm: 'row' }}
          gap={2}
        >
          <Typography variant="h4" sx={{ fontSize: { xs: '1.25rem', md: '2.125rem' } }} data-testid="constructor-total">
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
            Общая стоимость изделия: <strong>{grandTotal.toFixed(2)} ₽</strong>
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
