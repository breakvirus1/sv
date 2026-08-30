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
    enabled: false
  });

  const handleRefresh = () => {
    refetch();
  };

  const formatValue = (value) => {
    if (value === null || value === undefined) return '-';
    return Number(value).toFixed(2);
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
                <TableCell>Материал</TableCell>
                <TableCell align="right">Чистый расход (м²/п.м./шт)</TableCell>
                <TableCell align="right">Расход с процентом клиента</TableCell>
                <TableCell align="right">Расход без наценки (₽)</TableCell>
                <TableCell align="right">Расход из позиций заказов (₽)</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row, idx) => (
                <TableRow key={idx} hover>
                  <TableCell>{row.materialName}</TableCell>
                  <TableCell align="right">{formatValue(row.netQuantity)}</TableCell>
                  <TableCell align="right">{formatValue(row.consumptionWithPriceplus)} ₽</TableCell>
                  <TableCell align="right">{formatValue(row.cost)} ₽</TableCell>
                  <TableCell align="right">{formatValue(row.consumptionFromOrderPositions)} ₽</TableCell>
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
