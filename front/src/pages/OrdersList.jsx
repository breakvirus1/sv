import { getStatusColor, getStatusLabel } from '../utils/orderUtils';
import { DataGrid } from '@mui/x-data-grid';
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
import { Add, Person, Close } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useInfiniteQuery, useQuery, useMutation } from '@tanstack/react-query';
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
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');
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

  const managerId = currentEmployee?.id;

  const {
    data,
    isLoading,
    error,
    fetchNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ['orders', { status: statusFilter, my: myOrders, managerId }],
    queryFn: fetchOrders,
    enabled: !!user,
    getNextPageParam: (lastPage, allPages) => {
      return lastPage.last ? undefined : allPages.length;
    },
    refetchInterval: 30000,
    retry: 1,
    retryDelay: 1000,
  });

  const allOrders = data?.pages?.flatMap(page => page.content) ?? [];
  const totalCount = data?.pages?.[0]?.totalElements ?? 0;

  useEffect(() => {
    setPaginationModel(prev => ({ ...prev, page: 0 }));
  }, [statusFilter, myOrders, managerId]);

  const [columnWidths, setColumnWidths] = useState(loadColumnWidths);

  const prefetchPages = useCallback(async (targetPage) => {
    if (loadingRef.current) return;
    loadingRef.current = true;
    try {
      const loadedPages = data?.pages?.length ?? 0;
      if (targetPage >= loadedPages) {
        const pagesToFetch = targetPage - loadedPages + 1;
        for (let i = 0; i < pagesToFetch; i++) {
          await fetchNextPage();
        }
      }
    } finally {
      loadingRef.current = false;
    }
  }, [data?.pages?.length, fetchNextPage]);

  const handleColumnWidthChange = useCallback((params) => {
    setColumnWidths(prev => {
      const next = { ...prev, [params.colDef.field]: params.width };
      saveColumnWidths(next);
      return next;
    });
  }, []);

  const columns = [
    { field: 'orderNumber', headerName: '№ заказа', flex: 0.8, minWidth: 100, resizable: true },
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
        />
      )
    },
    {
      field: 'updatedAt',
      headerName: 'Изменён',
      flex: 1,
      minWidth: 140,
      resizable: true,
      valueFormatter: ({ value }) => {
        if (!value) return '';
        const d = new Date(value);
        return d.toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' });
      }
    },
    {
      field: 'dueDate',
      headerName: 'Срок',
      flex: 0.8,
      minWidth: 90,
      resizable: true,
      type: 'date',
      valueFormatter: ({ value }) => value || ''
    },
    {
      field: 'workshopId',
      headerName: 'Цех',
      flex: 0.8,
      minWidth: 80,
      resizable: true,
      renderCell: (params) => (
        <Typography variant="body2">{params.value || '—'}</Typography>
      )
    },
    ...(isAdmin ? [{
      field: 'actions',
      headerName: 'Действия',
      flex: 0.8,
      minWidth: 100,
      sortable: false,
      renderCell: (params) => (
        params.row?.status !== 'CLOSED' ? (
          <Button
            size="small"
            color="error"
            startIcon={<Close />}
            onClick={(e) => { e.stopPropagation(); handleCloseOrder(params.row.id); }}
          >
            Закрыть
          </Button>
        ) : null
      )
    }] : []),
  ];

  const closeOrderMutation = useMutation({
    mutationFn: (orderId) => api.put(`/api/v1/orders/${orderId}/close`),
    onSuccess: () => {
      fetchNextPage();
    },
  });

  const handleCloseOrder = (orderId) => {
    closeOrderMutation.mutate(orderId);
  };

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
    <Container maxWidth="xl" sx={{ mt: 4, px: 2.5, height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2} flexShrink={0}>
        <Typography variant="h4">{getTitle()}</Typography>
        <Box display="flex" alignItems="center" gap={2}>
          <Typography variant="body2" color="text.secondary">
            Заказов: {totalCount}
          </Typography>
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={() => navigate('/orders/new')}
          >
            Новый заказ
          </Button>
        </Box>
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
          onPaginationModelChange={async (model) => {
            await prefetchPages(model.page);
            setPaginationModel(model);
          }}
          pageSizeOptions={[PAGE_SIZE]}
          disableRowSelectionOnClick
          columnBuffer={8}
          density="compact"
          sx={{
            height: '100%',
            border: 'none',
            '& .MuiDataGrid-cell:hover': { cursor: 'pointer' },
            '& .MuiDataGrid-columnSeparator': { visibility: 'visible', resize: 'horizontal' },
            '& .MuiDataGrid-virtualScroller': { overflowX: 'auto' },
          }}
          onRowClick={(params) => navigate(`/orders/${params.id}`)}
          onColumnWidthChange={handleColumnWidthChange}
          slots={{
            noRowsOverlay: () => (
              <Box display="flex" justifyContent="center" alignItems="center" height="100%">
                <Typography color="text.secondary">Нет заказов</Typography>
              </Box>
            ),
          }}
          initialState={{
            sorting: {
              sortModel: [{ field: 'updatedAt', sort: 'desc' }]
            },
            columns: {
              columnVisibilityModel: {},
              dimensions: Object.entries(columnWidths).reduce((acc, [field, width]) => {
                acc[field] = { width };
                return acc;
              }, {})
            }
          }}
        />
      </Paper>
    </Container>
  );
};

export default OrdersList;
