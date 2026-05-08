import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Grid,
  Divider,
  Paper,
  Tooltip,
  IconButton,
  Checkbox,
  FormControlLabel
} from '@mui/material';
import { ArrowBack, Payment, InfoOutlined } from '@mui/icons-material';
import api from '../services/api';

const OperationForm = ({
  template,
  initialDimensions = { width: '', height: '' },
  itemQuantity = 1,
  materials = [],
  onConfirm,
  onCancel
}) => {
  const [width, setWidth] = useState(initialDimensions.width || '');
  const [height, setHeight] = useState(initialDimensions.height || '');
  const [parameters, setParameters] = useState({});
  const [additionalMats, setAdditionalMats] = useState([]);
  const [newAddMat, setNewAddMat] = useState({ materialId: '', quantity: 1 });

  // Initialize parameters from template defaults
  useEffect(() => {
    if (template && template.parameters) {
      const initParams = {};
      template.parameters.forEach(p => {
        initParams[p.paramKey] = p.defaultValue || (p.type === 'NUMBER' ? 0 : '');
      });
      setParameters(initParams);
    }
  }, [template]);

  // Calculated quantity from backend
  const [calculatedQty, setCalculatedQty] = useState(1);
  const [calcLoading, setCalcLoading] = useState(false);

  // Fetch calculated quantity from backend when dimensions or parameters change
  useEffect(() => {
    if (!template) return;

    const fetchQuantity = async () => {
      setCalcLoading(true);
      try {
        // Use current width/height from state (may be edited by user)
        const widthMetres = (parseFloat(width) || 0) / 1000;
        const heightMetres = (parseFloat(height) || 0) / 1000;
        const response = await api.post(
          `/api/v1/materials/${template.materialId}/operations/${template.id}/calculate-quantity`,
          {
            width: widthMetres,
            height: heightMetres,
            itemCount: itemQuantity,
            parameters: parameters
          }
        );
        setCalculatedQty(Number(response.data) || 1);
      } catch (error) {
        console.error('Failed to calculate operation quantity', error);
        setCalculatedQty(1);
      } finally {
        setCalcLoading(false);
      }
    };

    fetchQuantity();
  }, [template, width, height, itemQuantity, parameters]);

  const handleParamChange = (key, value) => {
    setParameters(prev => ({ ...prev, [key]: value }));
  };

  const handleAddMaterial = () => {
    if (newAddMat.materialId && newAddMat.quantity > 0) {
      setAdditionalMats(prev => [...prev, { ...newAddMat }]);
      setNewAddMat({ materialId: '', quantity: 1 });
    }
  };

  const removeAddMat = (idx) => {
    setAdditionalMats(prev => prev.filter((_, i) => i !== idx));
  };

  const handleConfirm = () => {
    const additionalMaterialsMap = {};
    additionalMats.forEach(am => {
      additionalMaterialsMap[am.materialId] = am.quantity;
    });
    const opData = {
      quantity: calcLoading ? 1 : calculatedQty,
      parameters: { ...parameters },
      additionalMaterials: additionalMaterialsMap
    };
    onConfirm(opData);
  };

  if (!template) return null;

  return (
    <Paper sx={{ p: 2, mt: 2 }}>
      {/* Operation name */}
      <Typography variant="subtitle1" gutterBottom>{template.name}</Typography>

      {/* Dimensions if required */}
      {template.requiresDimensions && (
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={6}>
            <Typography variant="caption" display="block" sx={{ fontSize: 10, mb: 0.5 }}>
              Ширина изделия в миллиметрах
            </Typography>
            <TextField
              label="Ширина (мм)"
              type="number"
              fullWidth
              size="small"
              value={width}
              onChange={(e) => setWidth(e.target.value)}
            />
          </Grid>
          <Grid item xs={6}>
            <Typography variant="caption" display="block" sx={{ fontSize: 10, mb: 0.5 }}>
              Высота изделия в миллиметрах
            </Typography>
            <TextField
              label="Высота (мм)"
              type="number"
              fullWidth
              size="small"
              value={height}
              onChange={(e) => setHeight(e.target.value)}
            />
          </Grid>
          <Grid item xs={12}>
            <Typography variant="body2">
              Количество (ед.): <strong>{(calcLoading ? 1 : calculatedQty).toFixed(4)}</strong> {template.unit}
            </Typography>
          </Grid>
        </Grid>
      )}

      {/* Dynamic Parameters */}
      {template.parameters && template.parameters.length > 0 && (
        <Box sx={{ mb: 2 }}>
          <Typography variant="subtitle2" gutterBottom>Параметры</Typography>
          {template.parameters.map(param => (
            <Box key={param.paramKey} mb={1} display="flex" alignItems="center" gap={1}>
              {param.type === 'CHECKBOX' ? (
                <>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={!!parameters[param.paramKey]}
                        onChange={(e) => handleParamChange(param.paramKey, e.target.checked)}
                      />
                    }
                    label={param.displayName}
                  />
                  {param.description && (
                    <Tooltip title={param.description} arrow>
                      <InfoOutlined sx={{ fontSize: 16, color: 'text.secondary', cursor: 'help' }} />
                    </Tooltip>
                  )}
                </>
              ) : (
                <>
                  <Box sx={{ minWidth: 200 }}>
                    <Typography variant="caption" display="block" sx={{ fontSize: 10, mb: 0.5 }}>
                      {param.description || param.displayName}
                    </Typography>
                    <TextField
                      label={param.displayName}
                      type={param.type === 'NUMBER' ? 'number' : 'text'}
                      size="small"
                      value={parameters[param.paramKey] || ''}
                      onChange={(e) => {
                        const val = param.type === 'NUMBER' ? parseFloat(e.target.value) : e.target.value;
                        handleParamChange(param.paramKey, val);
                      }}
                    />
                  </Box>
                  {param.unit && <Typography variant="body2">{param.unit}</Typography>}
                  {param.description && (
                    <Tooltip title={param.description} arrow>
                      <InfoOutlined sx={{ fontSize: 16, color: 'text.secondary', cursor: 'help' }} />
                    </Tooltip>
                  )}
                </>
              )}
            </Box>
          ))}
        </Box>
      )}

      {/* Additional Materials */}
      {template.allowsAdditionalMaterials && (
        <Box sx={{ mb: 2 }}>
          <Typography variant="subtitle2" gutterBottom>Дополнительные материалы</Typography>
          {additionalMats.map((am, idx) => {
            const mat = materials.find(m => m.id === parseInt(am.materialId));
            return (
              <Box key={idx} display="flex" gap={1} mb={1} alignItems="center">
                <FormControl size="small" sx={{ minWidth: 200 }}>
                  <InputLabel>Материал</InputLabel>
                  <Select
                    value={am.materialId}
                    label="Материал"
                    onChange={(e) => {
                      const arr = [...additionalMats];
                      arr[idx] = { ...arr[idx], materialId: e.target.value };
                      setAdditionalMats(arr);
                    }}
                  >
                    <MenuItem value="">Выберите</MenuItem>
                    {materials.map(m => (
                      <MenuItem key={m.id} value={m.id}>{m.name} ({m.unit})</MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <TextField
                  label="Кол-во"
                  type="number"
                  size="small"
                  value={am.quantity}
                  onChange={(e) => {
                    const arr = [...additionalMats];
                    arr[idx] = { ...arr[idx], quantity: parseFloat(e.target.value) || 0 };
                    setAdditionalMats(arr);
                  }}
                  sx={{ width: 100 }}
                />
                <Button size="small" color="error" onClick={() => removeAddMat(idx)}>Удалить</Button>
                {mat && (
                  <Typography variant="body2" sx={{ width: 100 }}>
                    Цена: {mat.price?.toFixed(2)} ₽
                  </Typography>
                )}
              </Box>
            );
          })}
          <Box display="flex" gap={1} mt={1}>
            <Box sx={{ minWidth: 200 }}>
              <Typography variant="caption" display="block" sx={{ fontSize: 10, mb: 0.5 }}>
                Материал
              </Typography>
              <FormControl size="small" fullWidth>
                <InputLabel>Материал</InputLabel>
                <Select
                  value={newAddMat.materialId}
                  label="Материал"
                  onChange={(e) => setNewAddMat({ ...newAddMat, materialId: e.target.value })}
                >
                  <MenuItem value="">Выберите</MenuItem>
                  {materials.map(m => (
                    <MenuItem key={m.id} value={m.id}>{m.name} ({m.unit})</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
            <Box sx={{ width: 100 }}>
              <Typography variant="caption" display="block" sx={{ fontSize: 10, mb: 0.5 }}>
                Количество
              </Typography>
              <TextField
                label="Кол-во"
                type="number"
                size="small"
                fullWidth
                value={newAddMat.quantity}
                onChange={(e) => setNewAddMat({ ...newAddMat, quantity: parseFloat(e.target.value) || 0 })}
              />
            </Box>
            <Button variant="outlined" size="small" onClick={handleAddMaterial} sx={{ alignSelf: 'flex-end', mb: 1 }}>
              Добавить
            </Button>
          </Box>
        </Box>
      )}

      <Divider sx={{ my: 2 }} />
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Box>
          <Typography variant="body1">
            Операция: <strong>{template.basePrice?.toFixed(2)} ₽</strong> за {template.unit}
          </Typography>
          <Typography variant="body2">
            Кол-во: <strong>{(calcLoading ? 1 : calculatedQty).toFixed(4)}</strong> {template.unit}
          </Typography>
          <Typography variant="h6">
            Итого: <strong>{(template.basePrice * (calcLoading ? 1 : calculatedQty) * (template.wasteCoefficient || 1)).toFixed(2)} ₽</strong>
          </Typography>
        </Box>
        <Box>
          <Button onClick={onCancel}>Отмена</Button>
          <Button variant="contained" onClick={handleConfirm}>Добавить</Button>
        </Box>
      </Box>
    </Paper>
  );
};

export default OperationForm;
