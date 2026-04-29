import { useQuery } from '@tanstack/react-query';
import { DataGrid, GridActionsCellItem } from '@mui/x-data-grid';
import {
  Box,
  Button,
  Chip,
  IconButton,
  Typography,
  Toolbar,
  Container,
  Paper,
  CircularProgress,
  Alert
} from '@mui/material';
import { Visibility, Add } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const fetchOrders = async () => {
  const response = await api.get('/api/v1/orders?size=50');
  return response.data.content || [];
};

const getStatusColor = (status) => {
  const colors = {
    WAITING: 'warning',
    LAUNCHED: 'info',
    IN_PROGRESS: 'primary',
    READY: 'success',
    ACCEPTED: 'success',
    CLOSED: 'default'
  };
  return colors[status] || 'default';
};

const OrdersList = () => {
  const navigate = useNavigate();
  const { data: orders = [], isLoading, error } = useQuery({
    queryKey: ['orders', { size: 50 }],
    queryFn: fetchOrders,
    refetchInterval: 30000, // опрос каждые 30 сек
  });

  const columns = [
    { field: 'orderNumber', headerName: '№ заказа', width: 130 },
    { field: 'client.name', headerName: 'Клиент', flex: 1, valueGetter: (value, row) => row.client?.name || '' },
    { field: 'description', headerName: 'Описание', flex: 2 },
    {
      field: 'totalAmount',
      headerName: 'Сумма',
      width: 120,
      type: 'number',
      valueFormatter: ({ value }) => `${value?.toFixed(2)} ₽`
    },
    {
      field: 'paidAmount',
      headerName: 'Оплачено',
      width: 120,
      type: 'number',
      valueFormatter: ({ value }) => `${value?.toFixed(2)} ₽`
    },
    {
      field: 'debtAmount',
      headerName: 'Долг',
      width: 100,
      type: 'number',
      valueFormatter: ({ value }) => `${value?.toFixed(2)} ₽`
    },
    {
      field: 'status',
      headerName: 'Статус',
      width: 140,
      renderCell: (params) => (
        <Chip
          label={params.value}
          color={getStatusColor(params.value)}
          size="small"
        />
      )
    },
    {
      field: 'dueDate',
      headerName: 'Срок',
      width: 120,
      type: 'date',
      valueFormatter: ({ value }) => value || ''
    },
    { field: 'manager.fullName', headerName: 'Менеджер', width: 150, valueGetter: (value, row) => row.manager?.fullName || '' },
    {
      field: 'actions',
      type: 'actions',
      headerName: 'Действия',
      width: 100,
      getActions: (params) => [
        <IconButton
          key="view"
          size="small"
          onClick={() => navigate(`/orders/${params.row.id}`)}
        >
          <Visibility />
        </IconButton>,
      ],
    },
  ];

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Container maxWidth="xl" sx={{ mt: 4 }}>
        <Alert severity="error">Ошибка загрузки заказов: {error.message}</Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ mt: 4 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Заказы</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => navigate('/orders/new')}
        >
          Новый заказ
        </Button>
      </Box>

      <Paper sx={{ height: 600, width: '100%' }}>
        <DataGrid
          rows={orders}
          columns={columns}
          pageSizeOptions={[25, 50, 100]}
          disableRowSelectionOnClick
          sx={{
            '& .MuiDataGrid-cell:hover': {
              cursor: 'pointer',
            },
          }}
          onRowClick={(params) => navigate(`/orders/${params.id}`)}
          initialState={{
            pagination: {
              paginationModel: { pageSize: 25 }
            },
            sorting: {
              sortModel: [{ field: 'createdAt', sort: 'desc' }]
            }
          }}
        />
      </Paper>
    </Container>
  );
};

export default OrdersList;
