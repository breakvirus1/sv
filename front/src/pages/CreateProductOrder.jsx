import React, { useState, useEffect } from 'react';
import {
  Box, Typography, Paper, Button, Grid, IconButton, Dialog, DialogTitle, DialogContent, DialogActions,
  Table, TableHead, TableRow, TableCell, TableBody, TableContainer, TextField, FormControl, InputLabel, Select, MenuItem, CircularProgress, Alert, Snackbar, useMediaQuery, Card, CardContent, Divider
} from '@mui/material';
import { Add, Delete, Save, ArrowBack } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '@mui/material/styles';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const CreateProductOrder = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { user } = useAuth();
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');
  const isManager = user?.roles?.includes('ROLE_MANAGER');
  const getOrdersRedirect = () => (isManager && !isAdmin ? '/orders?my=true' : '/orders');
  const [products, setProducts] = useState([]);
  const [selectedPositions, setSelectedPositions] = useState([]);
  const [clients, setClients] = useState([]);
  const [selectedClientId, setSelectedClientId] = useState('');
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });

  const { data: productsData = [], refetch: refetchProducts } = useQuery({
    queryKey: ['products-for-order'],
    queryFn: async () => {
      const response = await api.get('/api/v1/products');
      return response.data || [];
    },
    enabled: true,
  });

  const { data: clientsData = [] } = useQuery({
    queryKey: ['clients-for-order'],
    queryFn: async () => {
      const response = await api.get('/api/v1/clients');
      return response.data?.content || [];
    },
    enabled: true,
  });

  useEffect(() => {
    if (productsData.length > 0) {
      setProducts(productsData);
    }
    if (clientsData.length > 0) {
      setClients(clientsData);
      if (clientsData.length > 0 && !selectedClientId) {
        setSelectedClientId(String(clientsData[0].id));
      }
    }
    setLoading(false);
  }, [productsData, clientsData]);

  const addProduct = (product) => {
    const newPositions = {
      id: Date.now() + Math.random(),
      productId: product.id,
      productName: product.name,
      materials: (product.materials || []).map(m => ({
        ...m,
        quantity: m.quantity || 1,
        coefficient: m.wasteCoefficient || 1,
        cost: m.price ? Number((m.price * (m.quantity || 1) * (m.wasteCoefficient || 1)).toFixed(2)) : 0,
      })),
      operations: (product.operations || []).map(o => ({
        ...o,
        quantity: o.quantity || 1,
        coefficient: o.coefficient || 1,
        cost: o.pricePerUnit ? Number((o.pricePerUnit * (o.quantity || 1) * (o.coefficient || 1)).toFixed(2)) : 0,
      })),
      coefficient: 1,
    };
    setSelectedPositions([...selectedPositions, newPositions]);
  };

  const removePosition = (id) => {
    setSelectedPositions(selectedPositions.filter(p => p.id !== id));
  };

  const updatePositionCoefficient = (id, coefficient) => {
    setSelectedPositions(selectedPositions.map(p => {
      if (p.id === id) {
        const updated = { ...p, coefficient: parseFloat(coefficient) || 1 };
        return updated;
      }
      return p;
    }));
  };

  const updatePositionMaterialCoefficient = (positionId, materialIndex, coefficient) => {
    setSelectedPositions(selectedPositions.map(p => {
      if (p.id === positionId) {
        const updatedMaterials = [...p.materials];
        updatedMaterials[materialIndex] = {
          ...updatedMaterials[materialIndex],
          coefficient: parseFloat(coefficient) || 1,
          cost: updatedMaterials[materialIndex].price ? Number((updatedMaterials[materialIndex].price * (updatedMaterials[materialIndex].quantity || 1) * (updatedMaterials[materialIndex].wasteCoefficient || 1) * (parseFloat(coefficient) || 1)).toFixed(2)) : 0,
        };
        return { ...p, materials: updatedMaterials };
      }
      return p;
    }));
  };

  const updatePositionOperationCoefficient = (positionId, operationIndex, coefficient) => {
    setSelectedPositions(selectedPositions.map(p => {
      if (p.id === positionId) {
        const updatedOperations = [...p.operations];
        updatedOperations[operationIndex] = {
          ...updatedOperations[operationIndex],
          coefficient: parseFloat(coefficient) || 1,
          cost: updatedOperations[operationIndex].pricePerUnit ? Number((updatedOperations[operationIndex].pricePerUnit * (updatedOperations[operationIndex].quantity || 1) * (parseFloat(coefficient) || 1)).toFixed(2)) : 0,
        };
        return { ...p, operations: updatedOperations };
      }
      return p;
    }));
  };

  const getPositionTotal = (position) => {
    const materialsTotal = position.materials.reduce((sum, m) => sum + (m.cost || 0), 0);
    const operationsTotal = position.operations.reduce((sum, op) => sum + (op.cost || 0), 0);
    return Number((materialsTotal + operationsTotal).toFixed(2));
  };

  const grandTotal = selectedPositions.reduce((sum, p) => sum + getPositionTotal(p), 0);

  const handleSaveOrder = async () => {
    if (selectedPositions.length === 0) {
      setNotification({ open: true, message: 'Добавьте хотя бы одну позицию', severity: 'error' });
      return;
    }
    if (!selectedClientId) {
      setNotification({ open: true, message: 'Выберите клиента', severity: 'error' });
      return;
    }

    try {
      const productNames = selectedPositions.map(p => p.productName).join(', ');
      const items = [];
      for (const position of selectedPositions) {
        for (const m of position.materials) {
          items.push({
            materialId: m.materialId,
            fromProduct: true,
            quantity: m.quantity || 1,
            wasteCoefficient: m.wasteCoefficient || 1,
            unit: m.unit || 'шт',
              operations: position.operations.map(op => ({
              operationId: op.id,
              operationName: op.name,
              pricePerUnit: op.pricePerUnit,
              quantity: op.quantity || 1,
              widthM: null,
              heightM: null,
            })),
            widthM: null,
            heightM: null,
            readyDate: null,
            eyeletId: null,
            eyeletStepCm: null,
            podvorotMmHorizontal: null,
            podvorotMmVertical: null,
            podvorotCountPerSide: null,
            manualFilmSelectionValue: null,
          });
        }
      }

      const payload = {
        clientId: Number(selectedClientId),
        description: productNames,
        orderDate: new Date().toISOString().split('T')[0],
        dueDate: null,
        managerId: null,
        priceplus: 0,
        items: items,
        totalAmount: grandTotal,
        clientTotalWithPriceplus: grandTotal,
      };

      await api.post('/api/v1/orders', payload);
      setNotification({ open: true, message: 'Заказ успешно создан', severity: 'success' });
      setTimeout(() => navigate(getOrdersRedirect()), 1000);
    } catch (err) {
      console.error('Failed to save order', err);
      setNotification({ open: true, message: 'Ошибка создания заказа: ' + (err.response?.data?.message || err.message), severity: 'error' });
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: { xs: 1, sm: 2, md: 3 } }}>
      <Button startIcon={<ArrowBack />} onClick={() => navigate('/orders')} sx={{ mb: 2 }}>
        Назад
      </Button>
      <Typography variant="h4" gutterBottom sx={{ fontSize: { xs: '1.5rem', md: '2.125rem' } }}>Создать заказ из изделия</Typography>
      <Paper sx={{ p: 2, mb: 2 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={4}>
            <FormControl fullWidth size="small">
              <InputLabel>Клиент</InputLabel>
              <Select value={selectedClientId} label="Клиент" onChange={(e) => setSelectedClientId(e.target.value)}>
                {clients.map(c => <MenuItem key={c.id} value={String(c.id)}>{c.name}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} md={8}>
            <Typography variant="body2" color="text.secondary">
              Выберите изделия из списка слева, нажмите "+" чтобы добавить в заказ. Настройте коэффициенты и количество для каждой позиции.
            </Typography>
          </Grid>
        </Grid>
      </Paper>
      <Grid container spacing={2}>
        {/* Left column - Products list */}
        <Grid item xs={12} md={5}>
          <Paper sx={{ p: 2, height: '100%' }}>
            <Typography variant="h6" gutterBottom>Изделия</Typography>
            {products.length === 0 && <Alert severity="info">Нет изделий. Создайте изделие в конструкторе.</Alert>}
            <Box sx={{ maxHeight: '70vh', overflow: 'auto' }}>
              {products.map((product) => (
                <Card key={product.id} sx={{ mb: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <CardContent sx={{ flex: 1, py: 1, '&:last-child': { pb: 1 } }}>
                    <Typography variant="subtitle1">{product.name}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {product.materials?.length || 0} материалов, {product.operations?.length || 0} операций
                    </Typography>
                  </CardContent>
                  <IconButton color="primary" onClick={() => addProduct(product)} sx={{ mr: 1 }}>
                    <Add />
                  </IconButton>
                </Card>
              ))}
            </Box>
          </Paper>
        </Grid>

        {/* Right column - Selected positions */}
        <Grid item xs={12} md={7}>
          <Paper sx={{ p: 2, height: '100%' }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h6">Позиции заказа</Typography>
              <Typography variant="h6" sx={{ color: '#1976d2' }}>
                Итого: {grandTotal.toFixed(2)} ₽
              </Typography>
            </Box>

            {selectedPositions.length === 0 && (
              <Alert severity="info">Нажмите "+" рядом с изделием, чтобы добавить его в заказ</Alert>
            )}

            {selectedPositions.map((position) => (
              <Card key={position.id} sx={{ mb: 2 }}>
                <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid', borderColor: 'divider' }}>
                  <Box>
                    <Typography variant="subtitle1">{position.productName}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      Сумма: {getPositionTotal(position).toFixed(2)} ₽
                    </Typography>
                  </Box>
                  <Box display="flex" alignItems="center" gap={1}>
                    <TextField
                      size="small"
                      label="Коэф. позиции"
                      type="number"
                      value={position.coefficient}
                      onChange={(e) => updatePositionCoefficient(position.id, e.target.value)}
                      inputProps={{ step: 0.1, min: 0 }}
                      sx={{ width: 120 }}
                    />
                    <IconButton color="error" onClick={() => removePosition(position.id)}>
                      <Delete />
                    </IconButton>
                  </Box>
                </Box>

                {position.materials.length > 0 && (
                  <Box sx={{ p: 2, pt: 1 }}>
                    <Typography variant="subtitle2" sx={{ mb: 1 }}>Материалы</Typography>
                    {position.materials.map((m, idx) => (
                      <Box key={m.id || idx} sx={{ mb: 1, display: 'flex', alignItems: 'center', gap: 1, flexDirection: { xs: 'column', sm: 'row' } }}>
                        <Typography variant="body2" sx={{ minWidth: 150 }}>{m.materialName || m.name || 'Материал'}</Typography>
                        <TextField
                          size="small"
                          label="Кол-во"
                          type="number"
                          value={m.quantity}
                          onChange={(e) => {
                            const newQty = parseFloat(e.target.value) || 0;
                            const updatedMaterials = [...position.materials];
                            updatedMaterials[idx] = { ...updatedMaterials[idx], quantity: newQty };
                            const newMaterials = updatedMaterials.map(mat => ({
                              ...mat,
                              cost: mat.price ? Number((mat.price * newQty * (mat.wasteCoefficient || 1) * (mat.coefficient || 1)).toFixed(2)) : 0,
                            }));
                            setSelectedPositions(selectedPositions.map(p => p.id === position.id ? { ...p, materials: newMaterials } : p));
                          }}
                          inputProps={{ step: 0.01, min: 0 }}
                          sx={{ width: 100 }}
                        />
                        <TextField
                          size="small"
                          label="Коэф. кол-ва"
                          type="number"
                          value={m.coefficient}
                          onChange={(e) => updatePositionMaterialCoefficient(position.id, idx, e.target.value)}
                          inputProps={{ step: 0.1, min: 0 }}
                          sx={{ width: 120 }}
                        />
                         <Typography variant="body2" sx={{ minWidth: 80 }}>{m.cost?.toFixed(2) || '0.00'} ₽</Typography>
                      </Box>
                    ))}
                  </Box>
                )}

                {position.operations.length > 0 && (
                  <Box sx={{ p: 2, pt: 0 }}>
                    <Typography variant="subtitle2" sx={{ mb: 1 }}>Операции</Typography>
                    {position.operations.map((op, idx) => (
                      <Box key={op.id || idx} sx={{ mb: 1, display: 'flex', alignItems: 'center', gap: 1, flexDirection: { xs: 'column', sm: 'row' } }}>
                        <Typography variant="body2" sx={{ minWidth: 150 }}>{op.name}</Typography>
                        <TextField
                          size="small"
                          label="Кол-во"
                          type="number"
                          value={op.quantity}
                          onChange={(e) => {
                            const newQty = parseFloat(e.target.value) || 0;
                            const updatedOperations = [...position.operations];
                            updatedOperations[idx] = { ...updatedOperations[idx], quantity: newQty };
                            const newOperations = updatedOperations.map(operation => ({
                              ...operation,
                              cost: operation.pricePerUnit ? Number((operation.pricePerUnit * newQty * (operation.coefficient || 1)).toFixed(2)) : 0,
                            }));
                            setSelectedPositions(selectedPositions.map(p => p.id === position.id ? { ...p, operations: newOperations } : p));
                          }}
                          inputProps={{ step: 0.01, min: 0 }}
                          sx={{ width: 100 }}
                        />
                        <TextField
                          size="small"
                          label="Коэф. кол-ва"
                          type="number"
                          value={op.coefficient}
                          onChange={(e) => updatePositionOperationCoefficient(position.id, idx, e.target.value)}
                          inputProps={{ step: 0.1, min: 0 }}
                          sx={{ width: 120 }}
                        />
                         <Typography variant="body2" sx={{ minWidth: 80 }}>{op.cost?.toFixed(2) || '0.00'} ₽</Typography>
                      </Box>
                    ))}
                  </Box>
                )}
              </Card>
            ))}

            {selectedPositions.length > 0 && (
              <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                <Button variant="contained" color="success" size="large" startIcon={<Save />} onClick={handleSaveOrder}>
                  Сохранить заказ
                </Button>
              </Box>
            )}
          </Paper>
        </Grid>
      </Grid>

      <Dialog open={notification.open} onClose={() => setNotification({ ...notification, open: false })} maxWidth="xs" fullWidth fullScreen={isMobile}>
        <DialogTitle>{notification.severity === 'error' ? 'Ошибка' : 'Успех'}</DialogTitle>
        <DialogContent>
          <Typography>{notification.message}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNotification({ ...notification, open: false })}>OK</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CreateProductOrder;

