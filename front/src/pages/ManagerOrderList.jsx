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
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  InputAdornment
} from '@mui/material';
import { Add, Person, Close, Notifications, Search, ArrowUpward, ArrowDownward } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useInfiniteQuery, useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useEffect, useMemo } from 'react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const PAGE_SIZE = 150;
const MAX_PAGES = 20;

const loadColumnWidths = () => {
  try {
    const saved = localStorage.getItem('managerOrderListColumnWidths');
    return saved ? JSON.parse(saved) : {};
  } catch {
    return {};
  }
};

const saveColumnWidths = (widths) => {
  try {
    localStorage.setItem('managerOrderListColumnWidths', JSON.stringify(widths));
  } catch { /* ignore */ }
};

const fetchOrders = async ({ pageParam = 0, queryKey }) => {
  const [{ status, my, managerId, clientId, workshopId, q }] = queryKey;
  const searchParams = new URLSearchParams();
  searchParams.set('page', String(pageParam));
  searchParams.set('size', String(PAGE_SIZE));
  if (status) searchParams.set('status', status);
  if (my && managerId) searchParams.set('managerId', managerId);
  if (clientId) searchParams.set('clientId', clientId);
  if (workshopId) searchParams.set('workshopId', workshopId);
  if (q) searchParams.set('q', q);
  const response = await api.get(`/api/v1/orders?${searchParams}`);
  return response.data;
};

const ManagerOrderList = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');
  const isManager = user?.roles?.includes('ROLE_MANAGER') || user?.roles?.includes('ROLE_ADMIN');
  const [searchParams, setSearchParams] = useSearchParams();

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

  const { data: filterOptions } = useQuery({
    queryKey: ['orderFilterOptions'],
    queryFn: async () => {
      const response = await api.get('/api/v1/orders/filter-options');
      return response.data;
    },
  });

  const clients = filterOptions?.clients ?? [];
  const workshops = filterOptions?.workshops ?? [];

  const [selectedClientId, setSelectedClientId] = useState('');
  const [selectedWorkshopId, setSelectedWorkshopId] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [sortField, setSortField] = useState('updatedAt');
  const [sortDirection, setSortDirection] = useState('desc');

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchQuery.trim());
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  const queryClient = useQueryClient();
  const ordersQueryKey = [
    'managerOrders',
    {
      status: statusFilter,
      my: myOrders,
      managerId,
      clientId: selectedClientId,
      workshopId: selectedWorkshopId,
      q: debouncedSearch,
    },
  ];

  const {
    data,
    isLoading,
    error,
    fetchNextPage,
    isFetchingNextPage,
    hasNextPage,
  } = useInfiniteQuery({
    queryKey: ordersQueryKey,
    queryFn: fetchOrders,
    enabled: !!user && !!managerId,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.content?.length) return undefined;
      const totalLoaded = allPages.reduce((sum, page) => sum + (page.content?.length || 0), 0);
      const totalCount = allPages[0]?.totalElements;
      if (totalCount != null && totalLoaded >= totalCount) return undefined;
      if (lastPage.content.length < PAGE_SIZE) return undefined;
      if (allPages.length >= MAX_PAGES) return undefined;
      return allPages.length;
    },
    retry: 1,
    retryDelay: 1000,
  });

  const allOrders = data?.pages?.flatMap(page => page.content ?? []) ?? [];
  const totalCount = data?.pages?.[0]?.totalElements ?? 0;

  const sortedOrders = useMemo(() => {
    const orders = [...allOrders];
    orders.sort((a, b) => {
      let aVal = a[sortField];
      let bVal = b[sortField];

      if (sortField === 'client' && a.client && b.client) {
        aVal = a.client.name || '';
        bVal = b.client.name || '';
      } else if (sortField === 'manager' && a.manager && b.manager) {
        aVal = a.manager.fullName || '';
        bVal = b.manager.fullName || '';
      }

      if (aVal == null) aVal = '';
      if (bVal == null) bVal = '';

      if (typeof aVal === 'string') aVal = aVal.toLowerCase();
      if (typeof bVal === 'string') bVal = bVal.toLowerCase();

      if (aVal < bVal) return sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
    return orders;
  }, [allOrders, sortField, sortDirection]);

  const fetchNext = useCallback(async () => {
    if (isFetchingNextPage) return;
    if (!hasNextPage) return;
    await fetchNextPage();
  }, [fetchNextPage, isFetchingNextPage, hasNextPage]);

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };


  const [columnWidths, setColumnWidths] = useState(loadColumnWidths);

  const handleColumnWidthChange = useCallback((params) => {
    setColumnWidths(prev => {
      const next = { ...prev, [params.colDef.field]: params.width };
      saveColumnWidths(next);
      return next;
    });
  }, []);

  const getTitle = () => {
    if (myOrders) return 'Мои заказы';
    if (statusFilter) return `Заказы: ${getStatusLabel(statusFilter)}`;
    return 'Заказы';
  };

  if (isLoading) {
    return (
      <Box sx={{ maxWidth: 1900, mx: 'auto', mt: 4, px: 0, height: 'calc(100vh - 74px)', display: 'flex', flexDirection: 'column', py: 2, justifyContent: 'center', alignItems: 'center' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ maxWidth: 1900, mx: 'auto', mt: 4, px: 0, height: 'calc(100vh - 74px)', display: 'flex', flexDirection: 'column', py: 2 }}>
        <Alert severity="error">Ошибка загрузки заказов: {error.message}</Alert>
      </Box>
    );
  }

  return (
    <>
      <Box sx={{ maxWidth: 1900, mx: 'auto', px: 0, height: 'calc(100vh - 74px)', display: 'flex', flexDirection: 'column', py: 2 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2} flexShrink={0}>
          <Box display="flex" alignItems="center" gap={2} flexWrap="wrap">
            <Box>
              <Typography variant="h4">{getTitle()}</Typography>
              <Typography variant="body2" color="text.secondary">
                Заказов: {totalCount}
              </Typography>
            </Box>
            <Box display="flex" alignItems="center" gap={1} flexWrap="wrap">
              <FormControl size="small" sx={{ minWidth: 140 }}>
                <InputLabel>Клиент</InputLabel>
                <Select
                  value={selectedClientId}
                  onChange={(e) => setSelectedClientId(e.target.value)}
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

              <FormControl size="small" sx={{ minWidth: 130 }}>
                <InputLabel>Статус</InputLabel>
                <Select
                  value={statusFilter || ''}
                  onChange={(e) => {
                    const value = e.target.value;
                    if (value) {
                      setSearchParams({ status: value });
                    } else {
                      setSearchParams({});
                    }
                  }}
                  label="Статус"
                >
                  <MenuItem value="">Все</MenuItem>
                  <MenuItem value="DRAFT">Черновик</MenuItem>
                  <MenuItem value="IN_PROGRESS">В работе</MenuItem>
                  <MenuItem value="READY">Готов</MenuItem>
                  <MenuItem value="CLOSED">Закрыт</MenuItem>
                </Select>
              </FormControl>

              <FormControl size="small" sx={{ minWidth: 130 }}>
                <InputLabel>Цех</InputLabel>
                <Select
                  value={selectedWorkshopId}
                  onChange={(e) => setSelectedWorkshopId(e.target.value)}
                  label="Цех"
                >
                  <MenuItem value="">Все</MenuItem>
                  {workshops.map((workshop) => (
                    <MenuItem key={workshop.id} value={workshop.id}>
                      {workshop.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <TextField
                size="small"
                placeholder="Поиск по номеру или клиенту"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                sx={{ minWidth: 220 }}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Search fontSize="small" />
                    </InputAdornment>
                  ),
                }}
              />
            </Box>
          </Box>
          <Box>
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={() => navigate('/orders/new')}
            >
              Новый заказ
            </Button>
          </Box>
        </Box>

        {!sortedOrders.length ? (
          <Box display="flex" justifyContent="center" alignItems="center" flex={1}>
            <Typography color="text.secondary">Нет заказов</Typography>
          </Box>
        ) : (
          <Box id="manager-order-list-scroll" sx={{ flex: 1, overflow: 'auto' }}>
            <InfiniteScroll
              dataLength={sortedOrders.length}
              next={fetchNext}
              hasMore={!!hasNextPage}
              threshold={300}
              scrollToTopOnChange={false}
              initialLoaderHeight={80}
              scrollableTarget="manager-order-list-scroll"
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
                  display: 'table-header-group',
                  bgcolor: 'background.paper',
                  position: 'sticky',
                  top: 0,
                  zIndex: 1,
                }}
              >
                <Box
                  sx={{
                    display: 'table-row',
                    '& > *': {
                      borderBottom: '1px solid',
                      borderBottomColor: 'divider',
                      fontWeight: 600,
                      fontSize: '0.875rem',
                      color: 'text.secondary',
                    },
                  }}
                >
                  <Box sx={{ display: 'table-cell', padding: '12px 8px', cursor: 'pointer', userSelect: 'none' }} onClick={() => handleSort('orderNumber')}>
                    № заказа {sortField === 'orderNumber' && (sortDirection === 'asc' ? <ArrowUpward fontSize="small" /> : <ArrowDownward fontSize="small" />)}
                  </Box>
                  <Box sx={{ display: 'table-cell', padding: '12px 8px', cursor: 'pointer', userSelect: 'none' }} onClick={() => handleSort('client')}>
                    Клиент {sortField === 'client' && (sortDirection === 'asc' ? <ArrowUpward fontSize="small" /> : <ArrowDownward fontSize="small" />)}
                  </Box>
                  <Box sx={{ display: 'table-cell', padding: '12px 8px', cursor: 'pointer', userSelect: 'none' }} onClick={() => handleSort('manager')}>
                    Менеджер {sortField === 'manager' && (sortDirection === 'asc' ? <ArrowUpward fontSize="small" /> : <ArrowDownward fontSize="small" />)}
                  </Box>
                  <Box sx={{ display: 'table-cell', padding: '12px 8px', textAlign: 'right', cursor: 'pointer', userSelect: 'none' }} onClick={() => handleSort('totalAmount')}>
                    Сумма {sortField === 'totalAmount' && (sortDirection === 'asc' ? <ArrowUpward fontSize="small" /> : <ArrowDownward fontSize="small" />)}
                  </Box>
                  <Box sx={{ display: 'table-cell', padding: '12px 8px', textAlign: 'right', cursor: 'pointer', userSelect: 'none' }} onClick={() => handleSort('paidAmount')}>
                    Оплачено {sortField === 'paidAmount' && (sortDirection === 'asc' ? <ArrowUpward fontSize="small" /> : <ArrowDownward fontSize="small" />)}
                  </Box>
                  <Box sx={{ display: 'table-cell', padding: '12px 8px', textAlign: 'right', cursor: 'pointer', userSelect: 'none' }} onClick={() => handleSort('debtAmount')}>
                    Долг {sortField === 'debtAmount' && (sortDirection === 'asc' ? <ArrowUpward fontSize="small" /> : <ArrowDownward fontSize="small" />)}
                  </Box>
                  <Box sx={{ display: 'table-cell', padding: '12px 8px', cursor: 'pointer', userSelect: 'none' }} onClick={() => handleSort('status')}>
                    Статус {sortField === 'status' && (sortDirection === 'asc' ? <ArrowUpward fontSize="small" /> : <ArrowDownward fontSize="small" />)}
                  </Box>
                  <Box sx={{ display: 'table-cell', padding: '12px 8px', cursor: 'pointer', userSelect: 'none' }} onClick={() => handleSort('updatedAt')}>
                    Изменён {sortField === 'updatedAt' && (sortDirection === 'asc' ? <ArrowUpward fontSize="small" /> : <ArrowDownward fontSize="small" />)}
                  </Box>
                  <Box sx={{ display: 'table-cell', padding: '12px 8px', cursor: 'pointer', userSelect: 'none' }} onClick={() => handleSort('dueDate')}>
                    Срок {sortField === 'dueDate' && (sortDirection === 'asc' ? <ArrowUpward fontSize="small" /> : <ArrowDownward fontSize="small" />)}
                  </Box>
                </Box>
              </Box>

              <Box
                sx={{
                  display: 'table-row-group',
                }}
              >
                {sortedOrders.map((order) => (
                  <Box
                    key={order.id}
                    onClick={() => navigate(`/manager/orders/${order.id}`)}
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
                  </Box>
                ))}
              </Box>
            </Box>
           </InfiniteScroll>
         </Box>
       )}
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
    </>
  );
};

export default ManagerOrderList;
