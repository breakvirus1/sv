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

const OperationForm = ({
  template,
  initialDimensions = { width: '', height: '' },
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

  // Compute quantity based on dimensions if required, else default to 1
  const computedQuantity = (() => {
    if (!template) return 1;
    if (template.requiresDimensions) {
      const w = parseFloat(width) || 0;
      const h = parseFloat(height) || 0;
      if (template.unit === 'м²' || template.unit === 'м2') {
        // mm to m^2
        return (w / 1000) * (h / 1000);
      } else if (template.unit === 'пог.м' || template.unit === 'м.п.') {
        // perimeter in meters
        const perimeterMM = 2 * (w + h);
        return perimeterMM / 1000;
      } else {
        return 1;
      }
    } else {
      // Non-dimension based, quantity is 1 by default; could be provided as parameter?
      return 1;
    }
  })();

  const handleConfirm = () => {
    const additionalMaterialsMap = {};
    additionalMats.forEach(am => {
      additionalMaterialsMap[am.materialId] = am.quantity;
    });
    const opData = {
      quantity: computedQuantity,
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
              Количество (ед.): <strong>{computedQuantity.toFixed(4)}</strong> {template.unit}
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
                   <TextField
                     label={param.displayName}
                     type={param.type === 'NUMBER' ? 'number' : 'text'}
                     size="small"
                     value={parameters[param.paramKey] || ''}
                     onChange={(e) => {
                       const val = param.type === 'NUMBER' ? parseFloat(e.target.value) : e.target.value;
                       handleParamChange(param.paramKey, val);
                     }}
                     sx={{ minWidth: 200 }}
                   />
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
            <FormControl size="small" sx={{ minWidth: 200 }}>
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
            <TextField
              label="Кол-во"
              type="number"
              size="small"
              value={newAddMat.quantity}
              onChange={(e) => setNewAddMat({ ...newAddMat, quantity: parseFloat(e.target.value) || 0 })}
              sx={{ width: 100 }}
            />
            <Button variant="outlined" size="small" onClick={handleAddMaterial}>Добавить</Button>
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
            Кол-во: <strong>{computedQuantity.toFixed(4)}</strong> {template.unit}
          </Typography>
          <Typography variant="h6">
            Итого: <strong>{(template.basePrice * computedQuantity * (template.wasteCoefficient || 1)).toFixed(2)} ₽</strong>
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
