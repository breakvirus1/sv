import React, { useState, useEffect } from 'react';
import {
  Box, Typography, Paper, Button, Grid, IconButton, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, FormControl, InputLabel, Select, MenuItem, CircularProgress, Alert, useMediaQuery, Card, Divider, List, ListItem, ListItemText
} from '@mui/material';
import { Add, Delete, Save, ArrowBack } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '@mui/material/styles';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import { getApiErrorMessage } from '../utils/apiError';
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
  const [productPickerOpen, setProductPickerOpen] = useState(false);
  const [wizard, setWizard] = useState(null);
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });

  const { data: productsData = [] } = useQuery({
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

  const mapOperation = (operation) => ({
    operationId: operation.id,
    name: operation.name,
    pricePerUnit: Number(operation.pricePerUnit) || 0,
    unit: operation.unit || 'шт',
    quantity: Number(operation.quantity) || 1,
    coefficient: Number(operation.coefficient) || 1
  });

  const buildWizardPositions = (product) => {
    const sourcePositions = Array.isArray(product.positions) ? product.positions : [];
    if (sourcePositions.length > 0) {
      return sourcePositions.map(position => ({
        materialId: position.materialId,
        materialName: position.materialName || 'Материал',
        unit: position.unit || 'шт',
        price: Number(position.price) || 0,
        quantity: Number(position.quantity) || 1,
        wasteCoefficient: Number(position.wasteCoefficient) || 1,
        operations: (position.operations || []).map(mapOperation)
      }));
    }

    const materials = (product.materials || []).map(material => ({
      materialId: material.materialId,
      materialName: material.materialName || material.name || 'Материал',
      unit: material.unit || 'шт',
      price: Number(material.price) || 0,
      quantity: Number(material.quantity) || 1,
      wasteCoefficient: Number(material.wasteCoefficient) || 1,
      operations: []
    }));
    const operations = (product.operations || []).map(mapOperation);
    if (materials.length === 0) {
      return operations.length > 0 ? [{
        materialId: null,
        materialName: 'Работы',
        unit: 'шт',
        price: 0,
        quantity: 1,
        wasteCoefficient: 1,
        operations
      }] : [];
    }
    materials[0].operations = operations;
    return materials;
  };

  const startProductWizard = (product) => {
    const positions = buildWizardPositions(product);
    const steps = [];
    positions.forEach((position, positionIndex) => {
      steps.push({ kind: 'material', positionIndex });
      position.operations.forEach((operation, operationIndex) => {
        steps.push({ kind: 'operation', positionIndex, operationIndex });
      });
    });
    setProductPickerOpen(false);
    setWizard({ product, positions, steps, step: 0 });
  };

  const updateWizardMaterial = (positionIndex, field, value) => {
    setWizard(prev => {
      if (!prev) return prev;
      const positions = prev.positions.map((position, index) => (
        index === positionIndex ? { ...position, [field]: value } : position
      ));
      return { ...prev, positions };
    });
  };

  const updateWizardOperation = (positionIndex, operationIndex, field, value) => {
    setWizard(prev => {
      if (!prev) return prev;
      const positions = prev.positions.map((position, index) => {
        if (index !== positionIndex) return position;
        return {
          ...position,
          operations: position.operations.map((operation, opIndex) => (
            opIndex === operationIndex ? { ...operation, [field]: value } : operation
          ))
        };
      });
      return { ...prev, positions };
    });
  };

  const commitWizard = () => {
    if (!wizard) return;
    const newPositions = wizard.positions
      .filter(position => position.materialId)
      .map((position, index) => ({
        id: `${Date.now()}-${index}-${Math.random()}`,
        productId: wizard.product.id,
        productName: wizard.product.name,
        material: { ...position },
        operations: position.operations.map(operation => ({ ...operation }))
      }));
    setSelectedPositions(prev => [...prev, ...newPositions]);
    setWizard(null);
  };

  const nextWizardStep = () => {
    if (!wizard) return;
    if (wizard.step >= wizard.steps.length - 1) {
      commitWizard();
      return;
    }
    setWizard(prev => ({ ...prev, step: prev.step + 1 }));
  };

  const previousWizardStep = () => {
    setWizard(prev => (prev && prev.step > 0 ? { ...prev, step: prev.step - 1 } : prev));
  };

  const removePosition = (id) => {
    setSelectedPositions(selectedPositions.filter(p => p.id !== id));
  };

  const updateMaterialField = (positionId, field, value) => {
    setSelectedPositions(prev => prev.map(position => (
      position.id === positionId
        ? { ...position, material: { ...position.material, [field]: value } }
        : position
    )));
  };

  const updateOperationField = (positionId, operationIndex, field, value) => {
    setSelectedPositions(prev => prev.map(position => {
      if (position.id !== positionId) return position;
      return {
        ...position,
        operations: position.operations.map((operation, index) => (
          index === operationIndex ? { ...operation, [field]: value } : operation
        ))
      };
    }));
  };

  const getPositionSum = (position) => {
    const materialCost = (Number(position.material.price) || 0)
      * (Number(position.material.quantity) || 0)
      * (Number(position.material.wasteCoefficient) || 1);
    const operationsCost = position.operations.reduce((sum, operation) => (
      sum + (Number(operation.pricePerUnit) || 0)
        * (Number(operation.quantity) || 0)
        * (Number(operation.coefficient) || 1)
    ), 0);
    return Number((materialCost + operationsCost).toFixed(2));
  };

  const grandTotal = Number(selectedPositions.reduce((sum, position) => sum + getPositionSum(position), 0).toFixed(2));

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
      const productNames = [...new Set(selectedPositions.map(p => p.productName))].join(', ');
      const items = selectedPositions.map(position => ({
        materialId: position.material.materialId,
        fromProduct: true,
        quantity: position.material.quantity || 1,
        wasteCoefficient: position.material.wasteCoefficient || 1,
        unit: position.material.unit || 'шт',
        operations: position.operations.map(operation => ({
          operationId: operation.operationId,
          operationName: operation.name,
          pricePerUnit: operation.pricePerUnit,
          quantity: operation.quantity || 1,
          coefficient: operation.coefficient || 1,
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
      }));

      const payload = {
        clientId: Number(selectedClientId),
        description: productNames,
        orderDate: new Date().toISOString().split('T')[0],
        dueDate: null,
        managerId: null,
        priceplus: 0,
        items,
        totalAmount: grandTotal,
        clientTotalWithPriceplus: grandTotal,
      };

      await api.post('/api/v1/orders', payload);
      setNotification({ open: true, message: 'Заказ успешно создан', severity: 'success' });
      setTimeout(() => navigate(getOrdersRedirect()), 1000);
    } catch (err) {
      console.error('Failed to save order', err);
      setNotification({ open: true, message: 'Ошибка создания заказа: ' + (getApiErrorMessage(err)), severity: 'error' });
    }
  };

  const renderWizardDialog = () => {
    if (!wizard) return null;
    const step = wizard.steps[wizard.step];
    if (!step) return null;
    const position = wizard.positions[step.positionIndex];
    const operation = step.kind === 'operation' ? position.operations[step.operationIndex] : null;
    const isLast = wizard.step >= wizard.steps.length - 1;

    return (
      <Dialog open onClose={() => setWizard(null)} maxWidth="xs" fullWidth fullScreen={isMobile}>
        <DialogTitle>
          {step.kind === 'material' ? 'Параметры материала' : 'Параметры операции'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {wizard.product.name} · шаг {wizard.step + 1} из {wizard.steps.length}
          </Typography>
          <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
            {step.kind === 'material' ? position.materialName : operation.name}
          </Typography>
          {step.kind === 'material' ? (
            <Box display="flex" flexDirection="column" gap={2} sx={{ mt: 1 }}>
              <TextField
                label="Количество"
                type="number"
                value={position.quantity}
                onChange={(e) => updateWizardMaterial(step.positionIndex, 'quantity', parseFloat(e.target.value) || 0)}
                inputProps={{ step: 0.01, min: 0 }}
              />
              <TextField
                label="Коэф. отхода"
                type="number"
                value={position.wasteCoefficient}
                onChange={(e) => updateWizardMaterial(step.positionIndex, 'wasteCoefficient', parseFloat(e.target.value) || 1)}
                inputProps={{ step: 0.1, min: 0 }}
              />
            </Box>
          ) : (
            <Box display="flex" flexDirection="column" gap={2} sx={{ mt: 1 }}>
              <TextField
                label="Количество"
                type="number"
                value={operation.quantity}
                onChange={(e) => updateWizardOperation(step.positionIndex, step.operationIndex, 'quantity', parseFloat(e.target.value) || 0)}
                inputProps={{ step: 0.01, min: 0 }}
              />
              <TextField
                label="Коэф. кол-ва"
                type="number"
                value={operation.coefficient}
                onChange={(e) => updateWizardOperation(step.positionIndex, step.operationIndex, 'coefficient', parseFloat(e.target.value) || 1)}
                inputProps={{ step: 0.1, min: 0 }}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setWizard(null)}>Отмена</Button>
          <Button onClick={previousWizardStep} disabled={wizard.step === 0}>Назад</Button>
          <Button variant="contained" onClick={nextWizardStep}>
            {isLast ? 'Добавить в заказ' : 'Далее'}
          </Button>
        </DialogActions>
      </Dialog>
    );
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
              Нажмите "Добавить изделие", выберите изделие из конструктора и укажите параметры материалов и операций.
            </Typography>
          </Grid>
        </Grid>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2} flexDirection={{ xs: 'column', sm: 'row' }} gap={1}>
          <Typography variant="h6">Позиции заказа</Typography>
          <Box display="flex" alignItems="center" gap={2}>
            <Typography variant="h6" sx={{ color: '#1976d2' }} data-testid="order-grand-total">
              Итого: {grandTotal.toFixed(2)} ₽
            </Typography>
            <Button variant="contained" startIcon={<Add />} onClick={() => setProductPickerOpen(true)}>
              Добавить изделие
            </Button>
          </Box>
        </Box>

        {selectedPositions.length === 0 && (
          <Alert severity="info">Нажмите "Добавить изделие", чтобы выбрать изделие из конструктора</Alert>
        )}

        {selectedPositions.map((position) => (
          <Card key={position.id} variant="outlined" sx={{ mb: 2 }}>
            <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid', borderColor: 'divider' }} flexDirection={{ xs: 'column', sm: 'row' }} gap={1}>
              <Box>
                <Typography variant="subtitle1">{position.productName}</Typography>
                <Typography variant="body2" color="text.secondary">{position.material.materialName}</Typography>
              </Box>
              <Box display="flex" alignItems="center" gap={2}>
                <Typography variant="subtitle1" data-testid="position-total">
                  Сумма позиции: <strong>{getPositionSum(position).toFixed(2)} ₽</strong>
                </Typography>
                <IconButton color="error" onClick={() => removePosition(position.id)}>
                  <Delete />
                </IconButton>
              </Box>
            </Box>

            <Box sx={{ p: 2 }}>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Материал</Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexDirection: { xs: 'column', sm: 'row' } }}>
                <TextField
                  size="small"
                  label="Кол-во"
                  type="number"
                  value={position.material.quantity}
                  onChange={(e) => updateMaterialField(position.id, 'quantity', parseFloat(e.target.value) || 0)}
                  inputProps={{ step: 0.01, min: 0 }}
                  sx={{ width: 120 }}
                />
                <TextField
                  size="small"
                  label="Коэф. отхода"
                  type="number"
                  value={position.material.wasteCoefficient}
                  onChange={(e) => updateMaterialField(position.id, 'wasteCoefficient', parseFloat(e.target.value) || 1)}
                  inputProps={{ step: 0.1, min: 0 }}
                  sx={{ width: 140 }}
                />
                <Typography variant="body2" color="text.secondary">{position.material.unit}</Typography>
              </Box>

              {position.operations.length > 0 && (
                <>
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>Операции</Typography>
                  {position.operations.map((operation, index) => (
                    <Box key={`${operation.operationId}-${index}`} sx={{ mb: 1, display: 'flex', alignItems: 'center', gap: 2, flexDirection: { xs: 'column', sm: 'row' } }}>
                      <Typography variant="body2" sx={{ minWidth: 150 }}>{operation.name}</Typography>
                      <TextField
                        size="small"
                        label="Кол-во"
                        type="number"
                        value={operation.quantity}
                        onChange={(e) => updateOperationField(position.id, index, 'quantity', parseFloat(e.target.value) || 0)}
                        inputProps={{ step: 0.01, min: 0 }}
                        sx={{ width: 120 }}
                      />
                      <TextField
                        size="small"
                        label="Коэф. кол-ва"
                        type="number"
                        value={operation.coefficient}
                        onChange={(e) => updateOperationField(position.id, index, 'coefficient', parseFloat(e.target.value) || 1)}
                        inputProps={{ step: 0.1, min: 0 }}
                        sx={{ width: 140 }}
                      />
                      <Typography variant="body2" color="text.secondary">{operation.unit}</Typography>
                    </Box>
                  ))}
                </>
              )}
            </Box>
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

      <Dialog open={productPickerOpen} onClose={() => setProductPickerOpen(false)} maxWidth="sm" fullWidth fullScreen={isMobile}>
        <DialogTitle>Добавить изделие</DialogTitle>
        <DialogContent dividers>
          {products.length === 0 && <Alert severity="info">Нет изделий. Создайте изделие в конструкторе.</Alert>}
          <List disablePadding>
            {products.map((product) => {
              const positions = buildWizardPositions(product);
              const operationsCount = positions.reduce((sum, position) => sum + position.operations.length, 0);
              const productTotal = positions.reduce((sum, position) => {
                const materialCost = (Number(position.price) || 0) * (Number(position.quantity) || 0) * (Number(position.wasteCoefficient) || 1);
                const operationsCost = position.operations.reduce((opSum, operation) => (
                  opSum + (Number(operation.pricePerUnit) || 0) * (Number(operation.quantity) || 0) * (Number(operation.coefficient) || 1)
                ), 0);
                return sum + materialCost + operationsCost;
              }, 0);
              return (
                <ListItem
                  key={product.id}
                  divider
                  secondaryAction={<Button variant="outlined" onClick={() => startProductWizard(product)}>Выбрать</Button>}
                >
                  <ListItemText
                    primary={product.name}
                    secondary={`${positions.length} поз. · ${operationsCount} оп. · ${productTotal.toFixed(2)} ₽`}
                  />
                </ListItem>
              );
            })}
          </List>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setProductPickerOpen(false)}>Отмена</Button>
        </DialogActions>
      </Dialog>

      {renderWizardDialog()}

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
