import { getStatusColor, getStatusLabel } from '../utils/orderUtils';
import { DataGrid } from '@mui/x-data-grid';
import { useGridApiRef } from '@mui/x-data-grid';
import {
  Box,
  Button,
  Chip,
  Typography,
  Paper,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions
} from '@mui/material';
import { Add, Person, Close, Notifications } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
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

const OrdersList = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');
  const [searchParams] = useSearchParams();
  const apiRef = useGridApiRef();
  const [currPage, setCurrPage] = useState(1);
  const [prevPage, setPrevPage] = useState(0);
  const [allOrders, setAllOrders] = useState([]);
  const [wasLastList, setWasLastList] = useState(false);
  const [fetching, setFetching] = useState(false);
  const [totalCount, setTotalCount] = useState(0);

  const fetchingRef = useRef(fetching);
  const wasLastListRef = useRef(wasLastList);
  const fetchingDataRef = useRef(false);

  useEffect(() => {
    console.log('OrdersList mounted');
  }, []);

   useEffect(() => {
     fetchingRef.current = fetching;
   }, [fetching]);

   useEffect(() => {
     wasLastListRef.current = wasLastList;
   }, [wasLastList]);

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

  const { data: unreadNotifications = [] } = useQuery({
    queryKey: ['notifications'],
    queryFn: async () => {
      const response = await api.get('/api/v1/notifications');
      return response.data || [];
    },
    enabled: !!user,
    retry: 1,
    retryDelay: 1000,
    refetchInterval: 5000,
  });

  const hasUnreadNotifications = unreadNotifications.some(n => !n.readed);
  const [showNotificationDialog, setShowNotificationDialog] = useState(false);

  useEffect(() => {
    if (hasUnreadNotifications) {
      setShowNotificationDialog(true);
    }
  }, [hasUnreadNotifications]);

  const buildOrdersParams = (pageNumber) => {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(pageNumber));
    searchParams.set('size', String(PAGE_SIZE));
    if (statusFilter) searchParams.set('status', statusFilter);
    if (myOrders && managerId) searchParams.set('managerId', managerId);
    return searchParams.toString();
  };

   useEffect(() => {
     let cancelled = false;
     const fetchData = async () => {
       if (fetchingDataRef.current) return;
       fetchingDataRef.current = true;
       setFetching(true);
       try {
         const response = await api.get(`/api/v1/orders?${buildOrdersParams(currPage - 1)}`);
         const pageData = response.data;
         console.log('Orders fetch page', currPage - 1, pageData);
         if (!pageData.content || pageData.content.length === 0) {
           setWasLastList(true);
           return;
         }
         if (!cancelled) {
           setPrevPage(currPage);
           setAllOrders((prev) => [...prev, ...pageData.content]);
           setTotalCount(pageData.totalElements ?? 0);
         }
       } catch (err) {
         console.error('Failed to load orders:', err);
       } finally {
         setFetching(false);
         fetchingDataRef.current = false;
       }
     };

     if (!wasLastList && prevPage !== currPage) {
       fetchData();
     }

     return () => {
       cancelled = true;
     };
   }, [currPage, wasLastList, prevPage, statusFilter, myOrders, managerId]);

   useEffect(() => {
     console.log('OrdersList mount effect, allOrders.length=', allOrders.length);
     const SCROLL_SELECTOR = '.MuiDataGrid-virtualScroller';
     const scrollerRef = { current: apiRef.current?.virtualScrollerRef?.current ?? document.querySelector(SCROLL_SELECTOR) };
     const attachedRef = { current: false };

      const onScroll = () => {
        console.log('onScroll fired');
        if (fetchingRef.current || wasLastListRef.current) return;
        const current = apiRef.current?.virtualScrollerRef?.current || document.querySelector(SCROLL_SELECTOR) || scrollerRef.current;
        if (!current) return;
        const { scrollTop, scrollHeight, clientHeight } = current;
        console.log('Scroll values', { scrollTop, scrollHeight, clientHeight, delta: scrollHeight - (scrollTop + clientHeight) });
        if (scrollHeight - (scrollTop + clientHeight) < 120) {
          console.log('Scroll end detected, next page', currPage + 1);
          setCurrPage((prev) => prev + 1);
        }
      };

     const tryAttach = () => {
       scrollerRef.current = apiRef.current?.virtualScrollerRef?.current || document.querySelector(SCROLL_SELECTOR);
       console.log('Infinite scroll tryAttach, scroller=', !!scrollerRef.current, 'apiRef.virtualScrollerRef=', !!apiRef.current?.virtualScrollerRef?.current);
       if (!scrollerRef.current) return false;
       if (attachedRef.current) return true;
       scrollerRef.current.addEventListener('scroll', onScroll);
       attachedRef.current = true;
       console.log('Infinite scroll attached');
       return true;
     };

     const timeout = setTimeout(() => {
       const interval = setInterval(() => {
         if (tryAttach()) {
           clearInterval(interval);
         }
       }, 300);
       scrollerRef.current._infiniteInterval = interval;
     }, 100);

     return () => {
       clearTimeout(timeout);
       if (scrollerRef.current && scrollerRef.current._infiniteInterval) {
         clearInterval(scrollerRef.current._infiniteInterval);
       }
       if (scrollerRef.current) {
         scrollerRef.current.removeEventListener('scroll', onScroll);
       }
       attachedRef.current = false;
     };
   }, [allOrders.length]);

   useEffect(() => {
     setAllOrders([]);
     setCurrPage(1);
     setPrevPage(0);
     setWasLastList(false);
     setTotalCount(0);
     setFetching(false);
     fetchingDataRef.current = false;
   }, [statusFilter, myOrders, managerId]);

  const [columnWidths, setColumnWidths] = useState(loadColumnWidths);

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
      setAllOrders([]);
      setCurrPage(1);
      setPrevPage(0);
      setWasLastList(false);
      setTotalCount(0);
      setFetching(false);
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

  if (fetching && allOrders.length === 0) {
    return (
      <Box sx={{ maxWidth: 1900, mx: 'auto', mt: 4, px: 0, height: '100%', display: 'flex', flexDirection: 'column', py: 2, justifyContent: 'center', alignItems: 'center' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 1900, mx: 'auto', mt: 4, px: 0, height: '100%', display: 'flex', flexDirection: 'column', py: 2 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2} flexShrink={0}>
      <Box>
        <Typography variant="h4">{getTitle()}</Typography>
        <Typography variant="body2" color="text.secondary">
          Заказов: {totalCount}
        </Typography>
      </Box>
      <Box display="flex" alignItems="center" gap={2}>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => navigate('/orders/new')}
        >
          Новый заказ
        </Button>
      </Box>
    </Box>

    <Paper sx={{ m: 0, p: 0 }}>
      <DataGrid
        apiRef={apiRef}
        rows={allOrders}
        columns={columns}
        loading={fetching && allOrders.length === 0}
        pagination={false}
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
      {fetching && (
        <Box display="flex" justifyContent="center" alignItems="center" p={1}>
          <CircularProgress size={20} sx={{ mr: 1 }} />
          <Typography variant="body2">Загрузка...</Typography>
        </Box>
      )}
      {!fetching && allOrders.length > 0 && allOrders.length === totalCount && (
        <Box display="flex" justifyContent="center" alignItems="center" p={1}>
          <Typography variant="body2" color="text.secondary">Все данные загружены</Typography>
        </Box>
      )}
    </Paper>
    <Dialog
      open={showNotificationDialog}
      onClose={() => setShowNotificationDialog(false)}
      aria-labelledby="notification-dialog-title"
      aria-describedby="notification-dialog-description"
      PaperProps={{
        sx: {
          position: 'fixed',
          bottom: 24,
          right: 24,
          m: 0,
          width: 320,
        }
      }}
    >
      <DialogTitle id="notification-dialog-title">
        <Box display="flex" alignItems="center" gap={1}>
          <Notifications color="primary" />
          Уведомление
        </Box>
      </DialogTitle>
      <DialogContent>
        <Typography>Проверь уведомления</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={() => setShowNotificationDialog(false)} autoFocus>
          Закрыть
        </Button>
      </DialogActions>
    </Dialog>
  </Box>
  );
};

export default OrdersList;
