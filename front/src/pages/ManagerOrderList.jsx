import { getStatusColor, getStatusLabel } from '../utils/orderUtils';
import { DataGrid } from '@mui/x-data-grid';
import { useEffect } from 'react';
import {
  Box,
  Button,
  Chip,
  IconButton,
  Typography,
  Container,
  Paper,
  CircularProgress,
  Alert
} from '@mui/material';
import { Visibility, Add, Person } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const fetchOrders = async (params) => {
  const searchParams = new URLSearchParams();
  searchParams.set('size', '50');
  if (params.status) searchParams.set('status', params.status);
  if (params.my && params.managerId) searchParams.set('managerId', params.managerId);
  const response = await api.get(`/api/v1/orders?${searchParams}`);
  return response.data.content || [];
};

const ManagerOrderList = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const statusFilter = searchParams.get('status');
  const myOrders = searchParams.get('my');

  useEffect(() => {
    if (!statusFilter && !myOrders) {
      setSearchParams({ my: 'true' });
    }
  }, []);

  const { data: currentEmployee } = useQuery({
    queryKey: ['currentEmployee', user?.username],
    queryFn: async () => {
      if (!user?.username) return null;
      const response = await api.get(`/api/v1/employees?size=1&q=${user.username}`);
      const data = response.data.content || [];
      return data.length > 0 ? data[0] : null;
    },
    enabled: !!user?.username,
  });

  const managerId = currentEmployee?.id;

  const { data: orders = [], isLoading, error } = useQuery({
    queryKey: ['managerOrders', { status: statusFilter, my: myOrders, managerId }],
    queryFn: () => fetchOrders({ status: statusFilter, my: myOrders, managerId }),
    enabled: !!user,
    refetchInterval: 30000,
    retry: 1,
    retryDelay: 1000,
  });

  const columns = [
    { field: 'orderNumber', headerName: '№ заказа', width: 130 },
    {
      field: 'clientName',
      headerName: 'Клиент',
      width: 200,
      renderCell: (params) => (
        <Typography variant="body2">{params.row?.client?.name || '—'}</Typography>
      )
    },
    {
      field: 'managerName',
      headerName: 'Менеджер',
      width: 180,
      renderCell: (params) => (
        <Box display="flex" alignItems="center" gap={0.5}>
          <Person fontSize="small" color="action" />
          <Typography variant="body2">{params.row?.manager?.fullName || '—'}</Typography>
        </Box>
      )
    },
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
          label={getStatusLabel(params.value)}
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

  const getTitle = () => {
    if (myOrders) return 'Мои заказы';
    if (statusFilter) return `Заказы: ${getStatusLabel(statusFilter)}`;
    return 'Заказы';
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Container maxWidth="xl" sx={{ mt: 4, px: 2.5 }}>
        <Alert severity="error">Ошибка загрузки заказов: {error.message}</Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ mt: 4, px: 2.5 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">{getTitle()}</Typography>
        <Typography variant="body2" color="text.secondary">
          Заказов: {orders.length}
        </Typography>
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

export default ManagerOrderList;
