import { Box, Select, MenuItem, FormControl, InputLabel, Button } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';

const OrderFilters = ({ filters, onFilterChange }) => {
  const { data: clients = [] } = useQuery({
    queryKey: ['clients'],
    queryFn: async () => {
      const response = await api.get('/api/v1/clients?size=100');
      return response.data.content || [];
    },
  });

  const { data: employees = [] } = useQuery({
    queryKey: ['employees'],
    queryFn: async () => {
      const response = await api.get('/api/v1/employees?size=100');
      return response.data.content || [];
    },
  });

  const handleChange = (field) => (event) => {
    const value = event.target.value;
    onFilterChange({ ...filters, [field]: value === '' ? null : value });
  };

  const clearFilters = () => {
    onFilterChange({ clientId: null, managerId: null, status: '' });
  };

  return (
    <Box display="flex" gap={2} mb={2} flexWrap="wrap" alignItems="center">
      <FormControl size="small" sx={{ minWidth: 200 }}>
        <InputLabel>Клиент</InputLabel>
        <Select
          value={filters.clientId || ''}
          onChange={handleChange('clientId')}
          label="Клиент"
        >
          <MenuItem value="">Все</MenuItem>
          {clients.map((client) => (
            <MenuItem key={client.id} value={client.id}>
              {client.name}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <FormControl size="small" sx={{ minWidth: 200 }}>
        <InputLabel>Менеджер</InputLabel>
        <Select
          value={filters.managerId || ''}
          onChange={handleChange('managerId')}
          label="Менеджер"
        >
          <MenuItem value="">Все</MenuItem>
          {employees.map((emp) => (
            <MenuItem key={emp.id} value={emp.id}>
              {emp.fullName}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <FormControl size="small" sx={{ minWidth: 180 }}>
        <InputLabel>Статус</InputLabel>
        <Select
          value={filters.status || ''}
          onChange={handleChange('status')}
          label="Статус"
        >
          <MenuItem value="">Все</MenuItem>
          <MenuItem value="WAITING">Ожидание</MenuItem>
          <MenuItem value="LAUNCHED">Запущен</MenuItem>
          <MenuItem value="IN_PROGRESS">В работе</MenuItem>
          <MenuItem value="READY">Готов</MenuItem>
          <MenuItem value="ACCEPTED">Принят</MenuItem>
          <MenuItem value="CLOSED">Закрыт</MenuItem>
        </Select>
      </FormControl>

      <Button variant="outlined" onClick={clearFilters}>Сброс</Button>
    </Box>
  );
};

export default OrderFilters;
