import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
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

  // Fetch full order details for composition column (materials + items)
  const orderIds = useMemo(() => orders.map(o => o.id), [orders]);
  const { data: ordersFullMap = {} } = useQuery({
    queryKey: ['ordersFull', orderIds.join(',')],
    queryFn: async () => {
      if (!orders.length) return {};
      const entries = await Promise.all(
        orders.map(async order => {
          try {
            const { data } = await api.get(`/api/v1/orders/${order.id}`);
            return [order.id, data];
          } catch (e) {
            return [order.id, null];
          }
        })
      );
      return Object.fromEntries(entries.filter(([_, d]) => d != null));
    },
    enabled: orders.length > 0,
    staleTime: 60_000,
  });

  const columns = [
    { field: 'orderNumber', headerName: '№ заказа', width: 130 },
    { field: 'client.name', headerName: 'Клиент', width: 200, valueGetter: (value, row) => row?.client?.name || '' },
    { field: 'description', headerName: 'Описание', width: 300 },
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
    { field: 'manager.fullName', headerName: 'Менеджер', width: 150, valueGetter: (value, row) => row?.manager?.fullName || '' },
    {
      field: 'composition',
      headerName: 'Состав',
      width: 350,
      sortable: false,
      renderCell: (params) => {
        const orderDetails = ordersFullMap[params.row.id];
        if (!orderDetails) {
          return <Typography variant="body2" color="text.secondary">—</Typography>;
        }
        const materialNames = (orderDetails.materials || [])
          .map(m => m.material?.name)
          .filter(Boolean)
          .join(', ');
        const workNames = (orderDetails.items || [])
          .map(i => i.name)
          .join(', ');
        return (
          <Box sx={{ whiteSpace: 'normal', overflow: 'visible' }}>
            {materialNames && (
              <Typography variant="body2" color="text.secondary">
                Материалы: {materialNames}
              </Typography>
            )}
            {workNames && (
              <Typography variant="body2" color="text.secondary">
                Работы: {workNames}
              </Typography>
            )}
            {!materialNames && !workNames && (
              <Typography variant="body2" color="text.secondary">—</Typography>
            )}
          </Box>
        );
      }
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
          onClick={(e) => {
            e.stopPropagation();
            navigate(`/orders/${params.row.id}`);
          }}
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
    <Box sx={{ mt: 4, width: '1900px', mx: 'auto', overflowX: 'auto' }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
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
          rowHeight={80}
          columnBuffer={3}
          columnThreshold={3}
          sx={{
            '& .MuiDataGrid-cell:hover': {
              cursor: 'pointer',
            },
            '& .MuiDataGrid-columnHeaders': {
              cursor: 'col-resize',
            },
          }}
          initialState={{
            pagination: {
              paginationModel: { pageSize: 25 }
            },
            sorting: {
              sortModel: [{ field: 'createdAt', sort: 'desc' }]
            },
            columns: {
              columnVisibilityModel: {}
            }
          }}
        />
      </Paper>
    </Box>
  );
};

export default OrdersList;
