import { useState } from 'react';
import {
  Box, Typography, Paper, Table, TableHead, TableRow, TableCell,
  TableBody, TableContainer, TextField, Button, CircularProgress, Alert
} from '@mui/material';
import { Refresh } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';

const StatisticsTab = () => {
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  const { data: rows = [], isLoading, refetch, error } = useQuery({
    queryKey: ['admin-statistics-material-expense', fromDate, toDate],
    queryFn: async () => {
      const params = {};
      if (fromDate) params.fromDate = fromDate;
      if (toDate) params.toDate = toDate;
      const r = await api.get('/api/v1/admin/statistics/material-expense', { params });
      return r.data || [];
    },
    enabled: true
  });

  const handleRefresh = () => {
    refetch();
  };

  const formatValue = (value) => {
    if (value === null || value === undefined) return '-';
    return Number(value).toFixed(2);
  };

  const formatInt = (value) => {
    if (value === null || value === undefined) return '-';
    return String(value);
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Статистика расхода материалов</Typography>
        <Box display="flex" gap={2} alignItems="center">
          <TextField
            label="Дата с"
            type="date"
            size="small"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
          <TextField
            label="Дата по"
            type="date"
            size="small"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
          <Button variant="outlined" startIcon={<Refresh />} onClick={handleRefresh} disabled={isLoading}>
            {isLoading ? <CircularProgress size={16} /> : 'Обновить'}
          </Button>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>Ошибка загрузки статистики</Alert>}

      {rows.length === 0 && !isLoading && (
        <Typography>Нет данных за выбранный период</Typography>
      )}

      {rows.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Тип</TableCell>
                <TableCell>Наименование</TableCell>
                <TableCell>Ед. изм.</TableCell>
                <TableCell align="right">Чистый расход (без отходов)</TableCell>
                <TableCell align="right">Кол-во с отходами</TableCell>
                <TableCell align="right">Штуки, шт</TableCell>
                <TableCell align="right">Погонные метры, п.м.</TableCell>
                <TableCell align="right">Кв. метры, м²</TableCell>
                <TableCell align="right">Люверсы, шт</TableCell>
                <TableCell align="right">Расход с добавкой клиента, ₽</TableCell>
                <TableCell align="right">Себестоимость материалов (без добавки), ₽</TableCell>
                <TableCell align="right">Стоимость материалов с добавкой из позиций, ₽</TableCell>
                <TableCell align="right">Выполненных операций, шт</TableCell>
                <TableCell align="right">Стоимость выполненных операций, ₽</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row, idx) => (
                <TableRow key={idx} hover>
                  <TableCell>{row.type === 'operation' ? 'Операция' : row.type === 'eyelet' ? 'Люверс' : 'Материал'}</TableCell>
                  <TableCell>
                    {row.type === 'operation' ? `${row.name}${row.materialName ? ` (${row.materialName})` : ''}` : row.name}
                  </TableCell>
                  <TableCell>{row.unit || '-'}</TableCell>
                  <TableCell align="right">{formatValue(row.netQuantity)}</TableCell>
                  <TableCell align="right">{formatValue(row.quantityWithWaste)}</TableCell>
                  <TableCell align="right">{formatInt(row.pieces)}</TableCell>
                  <TableCell align="right">{formatValue(row.linearMeters)}</TableCell>
                  <TableCell align="right">{formatValue(row.squareMeters)}</TableCell>
                  <TableCell align="right">{formatInt(row.eyeletPieces)}</TableCell>
                  <TableCell align="right">{formatValue(row.consumptionWithPriceplus)} ₽</TableCell>
                  <TableCell align="right">{formatValue(row.cost)} ₽</TableCell>
                  <TableCell align="right">{formatValue(row.consumptionFromOrderPositions)} ₽</TableCell>
                  <TableCell align="right">{formatInt(row.operationCount)}</TableCell>
                  <TableCell align="right">{formatValue(row.operationsTotalCost)} ₽</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
};

export default StatisticsTab;
