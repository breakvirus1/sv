import { getStatusColor, getStatusLabel } from '../utils/orderUtils';
import InfiniteScroll from 'react-infinite-scroll-component';
import {
  Box,
  Button,
  Chip,
  Typography,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions
} from '@mui/material';
import { Add, Person, Close, Notifications } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useInfiniteQuery, useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useEffect, useRef } from 'react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const PAGE_SIZE = 50;

const OrdersList = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');
  const [searchParams] = useSearchParams();

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
  const ordersScrollRef = useRef(null);
  const [ordersScrollTarget, setOrdersScrollTarget] = useState(null);

  const setOrdersScrollRef = useCallback((node) => {
    ordersScrollRef.current = node;
    if (node) {
      setOrdersScrollTarget(node);
    }
  }, []);

  useEffect(() => {
    if (hasUnreadNotifications) {
      setShowNotificationDialog(true);
    }
  }, [hasUnreadNotifications]);

  const buildOrdersParams = (pageNumber) => {
    const params = new URLSearchParams();
    params.set('page', String(pageNumber));
    params.set('size', String(PAGE_SIZE));
    if (statusFilter) params.set('status', statusFilter);
    if (myOrders && managerId) params.set('managerId', managerId);
    return params.toString();
  };

  const queryClient = useQueryClient();
  const ordersQueryKey = [
    'orders',
    {
      status: statusFilter,
      my: myOrders,
      managerId: myOrders ? managerId : undefined,
    },
  ];

  const {
    data,
    isLoading,
    refetch,
    fetchNextPage,
    isFetchingNextPage,
    hasNextPage,
  } = useInfiniteQuery({
    queryKey: ordersQueryKey,
    queryFn: ({ pageParam = 0 }) => api
      .get(`/api/v1/orders?${buildOrdersParams(pageParam)}`)
      .then((response) => response.data),
    initialPageParam: 0,
    enabled: !!user && (!myOrders || !!managerId),
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.content?.length) return undefined;
      const totalLoaded = allPages.reduce((sum, page) => sum + (page.content?.length || 0), 0);
      const totalCount = allPages[0]?.totalElements;
      if (totalCount != null && totalLoaded >= totalCount) return undefined;
      if (lastPage.content.length < PAGE_SIZE) return undefined;
      return allPages.length;
    },
    retry: 1,
    retryDelay: 1000,
  });

  const allOrders = data?.pages?.flatMap((page) => page.content ?? []) ?? [];
  const totalCount = data?.pages?.[0]?.totalElements ?? 0;

  const fetchNext = useCallback(async () => {
    if (isFetchingNextPage) return;
    await fetchNextPage();
  }, [fetchNextPage, isFetchingNextPage]);

  const closeOrderMutation = useMutation({
    mutationFn: (orderId) => api.put(`/api/v1/orders/${orderId}/close`),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ordersQueryKey });
      void refetch();
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
       <Box sx={{ maxWidth: 1900, mx: 'auto', px: 0, height: 'calc(100vh - 74px)', display: 'flex', flexDirection: 'column', py: 2, justifyContent: 'center', alignItems: 'center' }}>
         <CircularProgress />
       </Box>
     );
   }

     return (
     <Box sx={{ maxWidth: 1900, mx: 'auto', px: 0, height: 'calc(100vh - 74px)', display: 'flex', flexDirection: 'column', py: 2 }}>
       <Box display="flex" justifyContent="space-between" alignItems="center" flexShrink={0}>
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

        <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', mt: 1 }}>
          {!allOrders.length ? (
            <Box display="flex" justifyContent="center" alignItems="center" height="100%">
              <Typography color="text.secondary">Нет заказов</Typography>
            </Box>
          ) : (
            <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
              <Box
                sx={{
                  display: 'table',
                  width: '100%',
                  borderCollapse: 'collapse',
                  bgcolor: 'background.paper',
                  flexShrink: 0,
                }}
              >
                <Box
                  sx={{
                    display: 'table-header-group',
                    '& > *': {
                      borderBottom: '1px solid',
                      borderBottomColor: 'divider',
                      fontWeight: 600,
                      fontSize: '0.875rem',
                      color: 'text.secondary',
                    },
                  }}
                >
                  <Box sx={{ display: 'table-row' }}>
                    <Box sx={{ display: 'table-cell', padding: '12px 8px' }}>№ заказа</Box>
                    <Box sx={{ display: 'table-cell', padding: '12px 8px' }}>Клиент</Box>
                    <Box sx={{ display: 'table-cell', padding: '12px 8px' }}>Менеджер</Box>
                    <Box sx={{ display: 'table-cell', padding: '12px 8px', textAlign: 'right' }}>Сумма</Box>
                    <Box sx={{ display: 'table-cell', padding: '12px 8px', textAlign: 'right' }}>Оплачено</Box>
                    <Box sx={{ display: 'table-cell', padding: '12px 8px', textAlign: 'right' }}>Долг</Box>
                    <Box sx={{ display: 'table-cell', padding: '12px 8px' }}>Статус</Box>
                    <Box sx={{ display: 'table-cell', padding: '12px 8px' }}>Изменён</Box>
                    <Box sx={{ display: 'table-cell', padding: '12px 8px' }}>Срок</Box>
                    <Box sx={{ display: 'table-cell', padding: '12px 8px' }}>Цех</Box>
                    {isAdmin && <Box sx={{ display: 'table-cell', padding: '12px 8px' }}>Действия</Box>}
                  </Box>
                </Box>
              </Box>
                 <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto' }} ref={setOrdersScrollRef}>
                   {ordersScrollTarget ? (
                     <InfiniteScroll
                      dataLength={allOrders.length}
                      next={fetchNext}
                      hasMore={!!hasNextPage}
                      scrollableTarget={ordersScrollTarget}
                      loader={
                        <Box display="flex" justifyContent="center" alignItems="center" py={2}>
                          <CircularProgress size={24} />
                        </Box>
                      }
                      endMessage={
                        <Box display="flex" justifyContent="center" alignItems="center" py={2}>
                          <Typography variant="body2" color="text.secondary">
                            Все заказы загружены
                          </Typography>
                        </Box>
                      }
                    >
                <Box
                  sx={{
                    display: 'table',
                    width: '100%',
                    borderCollapse: 'collapse',
                  }}
                >
                  <Box
                    sx={{
                      display: 'table-row-group',
                    }}
                  >
                    {allOrders.map((order) => (
                      <Box
                        key={order.id}
                        onClick={() => navigate(`/orders/${order.id}`)}
                        sx={{
                          display: 'table-row',
                          cursor: 'pointer',
                          '&:hover': {
                            bgcolor: 'action.hover',
                          },
                          '& > *': {
                            borderBottom: '1px solid',
                            borderBottomColor: 'divider',
                            padding: '10px 8px',
                            fontSize: '0.875rem',
                          },
                        }}
                      >
                        <Box sx={{ display: 'table-cell' }}>{order.orderNumber}</Box>
                        <Box sx={{ display: 'table-cell' }}>{order.client?.name || '—'}</Box>
                        <Box sx={{ display: 'table-cell' }}>
                          <Box display="flex" alignItems="center" gap={0.5}>
                            <Person fontSize="small" color="action" />
                            {order.manager?.fullName || '—'}
                          </Box>
                        </Box>
                        <Box sx={{ display: 'table-cell', textAlign: 'right' }}>{order.totalAmount?.toFixed(2)} ₽</Box>
                        <Box sx={{ display: 'table-cell', textAlign: 'right' }}>{order.paidAmount?.toFixed(2)} ₽</Box>
                        <Box sx={{ display: 'table-cell', textAlign: 'right' }}>{order.debtAmount?.toFixed(2)} ₽</Box>
                        <Box sx={{ display: 'table-cell' }}>
                          <Chip
                            label={getStatusLabel(order.status)}
                            color={getStatusColor(order.status)}
                            size="small"
                          />
                        </Box>
                        <Box sx={{ display: 'table-cell' }}>
                          {order.updatedAt ? new Date(order.updatedAt).toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' }) : ''}
                        </Box>
                        <Box sx={{ display: 'table-cell' }}>{order.dueDate || ''}</Box>
                        <Box sx={{ display: 'table-cell' }}>{order.workshopId || '—'}</Box>
                        {isAdmin && (
                          <Box sx={{ display: 'table-cell' }} onClick={(e) => e.stopPropagation()}>
                            {order.status !== 'CLOSED' && (
                              <Button
                                size="small"
                                color="error"
                                startIcon={<Close />}
                                onClick={(e) => { e.stopPropagation(); handleCloseOrder(order.id); }}
                              >
                                Закрыть
                              </Button>
                            )}
                          </Box>
                        )}
                      </Box>
                    ))}
                  </Box>
                </Box>
              </InfiniteScroll>
             ) : null}
             </Box>
             </Box>
           )}
        </Box>
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
