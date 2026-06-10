import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { DataGrid, GridActionsCellItem } from '@mui/x-data-grid';
import {
  Box,
  Button,
  Chip,
  Typography,
  Container,
  Paper,
  CircularProgress,
  Alert
} from '@mui/material';
import { Add, Person } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { useState, useCallback, useEffect, useRef } from 'react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const STORAGE_KEY = 'ordersListColumnWidths';
const PAGE_SIZE = 50;

const loadColumnWidths = () => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? JSON.parse(saved) : {};
  } catch {
    return {};
  }
};

const saveColumnWidths = (widths) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(widths));
  } catch { /* ignore */ }
};

const fetchOrders = async ({ pageParam = 0, queryKey }) => {
  const [, { status, my, managerId }] = queryKey;
  const searchParams = new URLSearchParams();
  searchParams.set('page', String(pageParam));
  searchParams.set('size', String(PAGE_SIZE));
  if (status) searchParams.set('status', status);
  if (my && managerId) searchParams.set('managerId', managerId);
  const response = await api.get(`/api/v1/orders?${searchParams}`);
  return response.data;
};

const OrdersList = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const [paginationModel, setPaginationModel] = useState({ page: 0, pageSize: PAGE_SIZE });
  const loadingRef = useRef(false);

  const statusFilter = searchParams.get('status');
  const myOrders = searchParams.get('my');

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
      field: 'clientName',
      headerName: 'Клиент',
      flex: 1.5,
      minWidth: 120,
      resizable: true,
      renderCell: (params) => (
        <Typography variant="body2">{params.row?.client?.name || '—'}</Typography>
      )
    },
    {
      field: 'managerName',
      headerName: 'Менеджер',
      flex: 1.2,
      minWidth: 120,
      resizable: true,
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
      flex: 0.8,
      minWidth: 90,
      resizable: true,
      type: 'number',
      valueFormatter: ({ value }) => `${value?.toFixed(2)} ₽`
    },
    {
      field: 'paidAmount',
      headerName: 'Оплачено',
      flex: 0.8,
      minWidth: 90,
      resizable: true,
      type: 'number',
      valueFormatter: ({ value }) => `${value?.toFixed(2)} ₽`
    },
    {
      field: 'debtAmount',
      headerName: 'Долг',
      flex: 0.7,
      minWidth: 80,
      resizable: true,
      type: 'number',
      valueFormatter: ({ value }) => `${value?.toFixed(2)} ₽`
    },
    {
      field: 'status',
      headerName: 'Статус',
      flex: 1,
      minWidth: 100,
      resizable: true,
      renderCell: (params) => (
        <Chip
          label={getStatusLabel(params.value)}
          color={getStatusColor(params.value)}
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

      <Paper sx={{ flex: 1, minHeight: 0, width: '100%', position: 'relative' }}>
        {isFetchingNextPage && (
          <Box sx={{ position: 'absolute', top: 0, left: 0, right: 0, zIndex: 10, display: 'flex', justifyContent: 'center', py: 1 }}>
            <CircularProgress size={24} />
          </Box>
        )}
        <DataGrid
          rows={allOrders}
          columns={columns}
          rowCount={totalCount}
          loading={isLoading || isFetchingNextPage}
          paginationMode="server"
          paginationModel={paginationModel}
          onPaginationModelChange={(model) => {
            const needFetch = model.page > (data?.pages?.length ?? 0) - 1;
            if (needFetch && hasNextPage) {
              fetchNextPage();
            }
            setPaginationModel(model);
          }}
          pageSizeOptions={[PAGE_SIZE]}
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
