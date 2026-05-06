import React, { useState, useEffect } from 'react';
import {
  Box, Typography, Paper, Button, Grid, Tabs, Tab, IconButton, Dialog, DialogTitle, DialogContent, Table, TableHead, TableRow, TableCell, TableBody, CircularProgress, Tooltip
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import SaveIcon from '@mui/icons-material/Save';
import { DataGrid } from '@mui/x-data-grid';

const EstimateCalculator = ({ orderItemId, productId, initialData, onSave }) => {
  const [tab, setTab] = useState(0);
  const [materials, setMaterials] = useState([]);
  const [operations, setOperations] = useState([]);
  const [materialSelectorOpen, setMaterialSelectorOpen] = useState(false);

  // Initialize data from props
  useEffect(() => {
    if (initialData) {
      setMaterials(initialData.materials || []);
      setOperations(initialData.operations || []);
    } else if (productId) {
      fetch(`/api/v1/products/${productId}/estimate`)
        .then(res => res.json())
        .then(data => {
          const mats = (data.materials || []).map(m => ({
            id: Date.now() + Math.random(),
            name: m.materialName,
            materialId: m.materialId,
            price: Number(m.price),
            quantity: Number(m.quantity),
            unit: m.unit || 'шт',
            wasteCoef: Number(m.wasteCoefficient),
            cost: Number(m.cost)
          }));
          setMaterials(mats);
          const ops = (data.operations || []).map(op => ({
            id: Date.now() + Math.random(),
            name: op.name,
            pricePerUnit: Number(op.pricePerUnit),
            normTime: op.normTime || '',
            quantity: 1,
            cost: Number(op.pricePerUnit)
          }));
          setOperations(ops);
        })
        .catch(err => console.error('Failed to fetch product estimate', err));
    }
  }, [productId, initialData]);

  const addMaterialRow = () => {
    setMaterials([...materials, {
      id: Date.now() + Math.random(),
      name: '',
      materialId: null,
      price: 0,
      quantity: 1,
      unit: 'шт',
      wasteCoef: 1,
      cost: 0
    }]);
  };

  const updateMaterial = (id, field, value) => {
    setMaterials(materials.map(row => {
      if (row.id === id) {
        const updated = { ...row, [field]: value };
        if (['price', 'quantity', 'wasteCoef'].includes(field)) {
          updated.cost = Number((updated.price * updated.quantity * (updated.wasteCoef || 1)).toFixed(2));
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
      name: '',
      pricePerUnit: 0,
      quantity: 1,
      normTime: '',
      cost: 0
    }]);
  };

  const updateOperation = (id, field, value) => {
    setOperations(operations.map(row => {
      if (row.id === id) {
        const updated = { ...row, [field]: value };
        if (['pricePerUnit', 'quantity'].includes(field)) {
          updated.cost = Number((updated.pricePerUnit * updated.quantity).toFixed(2));
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

  const columnsMaterials = [
    {
      field: 'name',
      headerName: 'Наименование',
      flex: 2,
      editable: true,
      renderHeader: () => (
        <Tooltip title="Название материала, например: Сталь, Алюминий, Пластик">
          <span>Наименование</span>
        </Tooltip>
      )
    },
    {
      field: 'price',
      headerName: 'Цена',
      type: 'number',
      width: 120,
      editable: true,
      valueFormatter: params => `${params.value} ₽`,
      renderHeader: () => (
        <Tooltip title="Цена за единицу материала в рублях">
          <span>Цена</span>
        </Tooltip>
      )
    },
    {
      field: 'quantity',
      headerName: 'Кол-во',
      type: 'number',
      width: 100,
      editable: true,
      renderHeader: () => (
        <Tooltip title="Количество материалов, требуется для производства">
          <span>Кол-во</span>
        </Tooltip>
      )
    },
    {
      field: 'unit',
      headerName: 'Ед.изм.',
      width: 80,
      editable: true,
      renderHeader: () => (
        <Tooltip title="Единица измерения: шт, м, м2, м.п., кг и т.д.">
          <span>Ед.изм.</span>
        </Tooltip>
      )
    },
    {
      field: 'wasteCoef',
      headerName: 'Коэф.отх.',
      type: 'number',
      width: 110,
      editable: true,
      renderHeader: () => (
        <Tooltip title="Коэффициент отходов: 1 = 0% отходов, 1.1 = 10% отходов. Учитывает потери при раскрое">
          <span>Коэф.отх.</span>
        </Tooltip>
      )
    },
    {
      field: 'cost',
      headerName: 'Стоимость',
      type: 'number',
      width: 130,
      valueFormatter: params => `${params.value} ₽`,
      renderHeader: () => (
        <Tooltip title="Итоговая стоимость: цена × кол-во × коэффициент отходов">
          <span>Стоимость</span>
        </Tooltip>
      )
    },
    {
      field: 'actions',
      headerName: '',
      width: 60,
      renderCell: (params) => (
        <IconButton color="error" onClick={() => deleteMaterial(params.row.id)}>
          <DeleteIcon />
        </IconButton>
      )
    }
  ];

  const columnsOperations = [
    {
      field: 'name',
      headerName: 'Наименование',
      flex: 2,
      editable: true,
      renderHeader: () => (
        <Tooltip title="Название операции или вида работы, например: Резка, гибка, сварка">
          <span>Наименование</span>
        </Tooltip>
      )
    },
    {
      field: 'pricePerUnit',
      headerName: 'Цена за ед.',
      type: 'number',
      width: 140,
      editable: true,
      valueFormatter: params => `${params.value} ₽`,
      renderHeader: () => (
        <Tooltip title="Стоимость одной единицы работы (например, за метр, за штуку, за час)">
          <span>Цена за ед.</span>
        </Tooltip>
      )
    },
    {
      field: 'quantity',
      headerName: 'Кол-во',
      type: 'number',
      width: 100,
      editable: true,
      renderHeader: () => (
        <Tooltip title="Количество единиц работы (метров, штук, часов)">
          <span>Кол-во</span>
        </Tooltip>
      )
    },
    {
      field: 'normTime',
      headerName: 'Норма времени',
      width: 120,
      editable: true,
      renderHeader: () => (
        <Tooltip title="Нормативное время на выполнение операции в часах или минутах">
          <span>Норма времени</span>
        </Tooltip>
      )
    },
    {
      field: 'cost',
      headerName: 'Стоимость',
      type: 'number',
      width: 130,
      valueFormatter: params => `${params.value} ₽`,
      renderHeader: () => (
        <Tooltip title="Общая стоимость работы: цена за ед. × кол-во">
          <span>Стоимость</span>
        </Tooltip>
      )
    },
    {
      field: 'actions',
      headerName: '',
      width: 60,
      renderCell: (params) => (
        <IconButton color="error" onClick={() => deleteOperation(params.row.id)}>
          <DeleteIcon />
        </IconButton>
      )
    }
  ];

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>Смета по калькулятору</Typography>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tab label="Материалы" />
        <Tab label="Работы" />
      </Tabs>

      {tab === 0 && (
        <Box>
          <Box display="flex" justifyContent="space-between" mb={2}>
            <Typography variant="h6">Материалы</Typography>
            <Box>
              <Button variant="outlined" startIcon={<AddIcon />} onClick={() => setMaterialSelectorOpen(true)} sx={{ mr: 1 }}>
                Из базы
              </Button>
              <Button variant="contained" startIcon={<AddIcon />} onClick={addMaterialRow}>
                Вручную
              </Button>
            </Box>
          </Box>

          <DataGrid
            rows={materials}
            columns={columnsMaterials}
            autoHeight
            disableRowSelectionOnClick
            onRowEditCommit={(params) => updateMaterial(params.id, params.field, params.value)}
          />

          <Typography variant="h6" align="right" sx={{ mt: 2 }}>
            Итого материалы: <strong>{totalMaterials.toFixed(2)} ₽</strong>
          </Typography>
        </Box>
      )}

      {tab === 1 && (
        <Box>
          <Box display="flex" justifyContent="space-between" mb={2}>
            <Typography variant="h6">Работы / Операции</Typography>
            <Button variant="contained" startIcon={<AddIcon />} onClick={addOperationRow}>
              Добавить работу
            </Button>
          </Box>
          <DataGrid
            rows={operations}
            columns={columnsOperations}
            autoHeight
            disableRowSelectionOnClick
            onRowEditCommit={(params) => updateOperation(params.id, params.field, params.value)}
          />
          <Typography variant="h6" align="right" sx={{ mt: 2 }}>
            Итого работы: <strong>{totalOperations.toFixed(2)} ₽</strong>
          </Typography>
        </Box>
      )}

      <Grid container justifyContent="space-between" alignItems="center" sx={{ mt: 4, pt: 3, borderTop: '2px solid #1976d2' }}>
        <Typography variant="h4">
          Общий итог: <strong style={{ color: '#1976d2' }}>{grandTotal.toFixed(2)} ₽</strong>
        </Typography>

        <Button
          variant="contained"
          color="success"
          size="large"
          startIcon={<SaveIcon />}
          onClick={() => onSave && onSave({ materials, operations, productId: productId, total: grandTotal })}
        >
          Сохранить смету
        </Button>
      </Grid>

      <MaterialSelector
        open={materialSelectorOpen}
        onClose={() => setMaterialSelectorOpen(false)}
        onSelect={(material) => {
          const newMat = {
            id: Date.now() + Math.random(),
            materialId: material.id,
            name: material.name,
            price: Number(material.price),
            quantity: 1,
            unit: material.unit || 'шт',
            wasteCoef: 1.0,
            cost: Number(material.price)
          };
          setMaterials([...materials, newMat]);
        }}
      />
    </Paper>
  );
};

export default EstimateCalculator;
