import { getStatusColor, getStatusLabel } from '../utils/orderUtils';
import {
  Box,
  Chip,
  Typography,
  Container,
  Paper,
  CircularProgress,
  Alert,
  Collapse,
  IconButton,
  Link
} from '@mui/material';
import { Person, ExpandMore, ExpandLess, AttachFile, Category, ArrowDropUp, ArrowDropDown } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const API_BASE = import.meta.env.VITE_API_URL || '';
const PAGE_SIZE = 50;

const downloadFile = async (fileUrl) => {
  const response = await api.get(fileUrl, { responseType: 'blob' });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', fileUrl.split('/').pop() || 'file');
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

const fetchOrders = async ({ pageParam = 0, queryKey }) => {
  const [, { status }] = queryKey;
  const searchParams = new URLSearchParams();
  searchParams.set('page', String(pageParam));
  searchParams.set('size', String(PAGE_SIZE));
  if (status) searchParams.set('status', status);
  const response = await api.get(`/api/v1/orders?${searchParams}`);
  return response.data;
};

const fetchOrderDetail = async (orderId) => {
  const response = await api.get(`/api/v1/orders/${orderId}`);
  return response.data;
};

const PositionRow = ({ position, order, index, onNavigate }) => {
  const [expanded, setExpanded] = useState(false);

  return (
    <Box>
      <Box
        onClick={() => setExpanded(!expanded)}
        sx={{
          display: 'flex',
          alignItems: 'center',
          p: 1.5,
          borderBottom: '1px solid',
          borderColor: 'divider',
          cursor: 'pointer',
          '&:hover': { bgcolor: 'action.hover' }
        }}
      >
        <Box sx={{ flex: 1, minWidth: 120 }}>
          <Typography variant="body2" fontWeight="medium">{order.orderNumber}</Typography>
        </Box>
        <Box sx={{ flex: 1, minWidth: 120 }}>
          <Typography variant="body2">{order.client?.name || '—'}</Typography>
        </Box>
        <Box sx={{ flex: 1, minWidth: 120 }} display="flex" alignItems="center" gap={0.5}>
          <Person fontSize="small" color="action" />
          <Typography variant="body2">{order.manager?.fullName || '—'}</Typography>
        </Box>
        <Box sx={{ flex: 1, minWidth: 120 }}>
          <Typography variant="body2">{position.name || `Позиция ${index + 1}`}</Typography>
        </Box>
        <Box sx={{ flex: 0.8, minWidth: 100 }}>
          <Chip
            label={getStatusLabel(order.status)}
            color={getStatusColor(order.status)}
            size="small"
          />
        </Box>
        <Box sx={{ flex: 0.8, minWidth: 90 }}>
          <Typography variant="body2">{order.dueDate || '—'}</Typography>
        </Box>
        <Box sx={{ flex: 1, minWidth: 140 }}>
          <Typography variant="body2">
            {order.updatedAt ? new Date(order.updatedAt).toLocaleString('ru-RU', {
              day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit'
            }) : ''}
          </Typography>
        </Box>
        <Box sx={{ flex: 0.8, minWidth: 100 }}>
          {position.fileUrl ? (
            <Link
              onClick={(e) => { e.stopPropagation(); downloadFile(position.fileUrl); }}
              sx={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}
            >
              <AttachFile fontSize="small" color="action" />
            </Link>
          ) : (
            <Typography variant="body2" color="text.secondary">—</Typography>
          )}
        </Box>
        <IconButton size="small">
          {expanded ? <ExpandLess /> : <ExpandMore />}
        </IconButton>
      </Box>

      <Collapse in={expanded}>
        <Box sx={{ p: 2, bgcolor: 'grey.50', borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="subtitle2" gutterBottom>
            <Category fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.5 }} />
            Операции для позиции «{position.name || `Позиция ${index + 1}`}»
          </Typography>
          {position.operations && position.operations.length > 0 ? (
            <Box component="ul" sx={{ pl: 2, m: 0 }}>
              {position.operations.map((op, opIndex) => (
                <Box component="li" key={opIndex} sx={{ mb: 0.5 }}>
                  <Typography variant="body2">
                    {op.name || op.operationName || `Операция ${opIndex + 1}`}
                  </Typography>
                </Box>
              ))}
            </Box>
          ) : (
            <Typography variant="body2" color="text.secondary">Нет операций</Typography>
          )}
          <Box mt={2}>
            <Typography
              variant="body2"
              color="primary"
              sx={{ cursor: 'pointer' }}
              onClick={(e) => { e.stopPropagation(); onNavigate(order.id); }}
            >
              Открыть заказ →
            </Typography>
          </Box>
        </Box>
      </Collapse>
    </Box>
  );
};

const ProductionOrdersPositionsList = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const loadingRef = useRef(false);
  const [orderDetails, setOrderDetails] = useState({});
  const [sortField, setSortField] = useState('updatedAt');
  const [sortDirection, setSortDirection] = useState('desc');

  const statusFilter = searchParams.get('status');

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  const SortIcon = ({ field }) => {
    if (sortField !== field) return null;
    return sortDirection === 'asc' ? <ArrowDropUp fontSize="small" /> : <ArrowDropDown fontSize="small" />;
  };

  const {
    data,
    isLoading,
    error,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ['productionOrdersPositions', { status: statusFilter }],
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

  // Загружаем детали для всех заказов
  useEffect(() => {
    allOrders.forEach(order => {
      if (!orderDetails[order.id]) {
        fetchOrderDetail(order.id).then(detail => {
          setOrderDetails(prev => ({ ...prev, [order.id]: detail }));
        });
      }
    });
  }, [allOrders]);

  // Собираем все позиции с информацией о заказе
  const allPositions = useMemo(() => {
    const positions = [];
    allOrders.forEach(order => {
      const detail = orderDetails[order.id];
      const displayOrder = detail || order;
      const items = displayOrder.items || [];
      items.forEach((position, index) => {
        positions.push({
          position,
          order,
          index,
          orderNumber: order.orderNumber || '',
          clientName: order.client?.name || '—',
          managerName: order.manager?.fullName || '—',
          positionName: position.name || `Позиция ${index + 1}`,
          status: order.status || '',
          dueDate: order.dueDate || '',
          updatedAt: order.updatedAt || '',
          fileUrl: position.fileUrl || null,
        });
      });
    });
    return positions;
  }, [allOrders, orderDetails]);

  // Сортировка позиций
  const sortedPositions = useMemo(() => {
    return [...allPositions].sort((a, b) => {
      let aVal = a[sortField];
      let bVal = b[sortField];

      if (sortField === 'updatedAt' || sortField === 'dueDate') {
        aVal = aVal ? new Date(aVal).getTime() : 0;
        bVal = bVal ? new Date(bVal).getTime() : 0;
      } else if (typeof aVal === 'string') {
        aVal = aVal.toLowerCase();
        bVal = bVal.toLowerCase();
      }

      if (aVal < bVal) return sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [allPositions, sortField, sortDirection]);

  const getTitle = () => {
    if (statusFilter) return `Позиции заказов: ${getStatusLabel(statusFilter)}`;
    return 'Позиции заказов';
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
            Позиций: {sortedPositions.length}
          </Typography>
        </Box>
      </Box>

      <Paper sx={{ flex: 1, minHeight: 0, width: '100%', overflow: 'auto' }}>
        <Box sx={{ display: 'flex', p: 1.5, borderBottom: '2px solid', borderColor: 'divider', bgcolor: 'grey.100' }}>
          <Box onClick={() => handleSort('orderNumber')} sx={{ flex: 1, minWidth: 120, cursor: 'pointer', display: 'flex', alignItems: 'center', userSelect: 'none' }}>
            <Typography variant="caption">№ заказа</Typography>
            <SortIcon field="orderNumber" />
          </Box>
          <Box onClick={() => handleSort('clientName')} sx={{ flex: 1, minWidth: 120, cursor: 'pointer', display: 'flex', alignItems: 'center', userSelect: 'none' }}>
            <Typography variant="caption">Клиент</Typography>
            <SortIcon field="clientName" />
          </Box>
          <Box onClick={() => handleSort('managerName')} sx={{ flex: 1, minWidth: 120, cursor: 'pointer', display: 'flex', alignItems: 'center', userSelect: 'none' }}>
            <Typography variant="caption">Менеджер</Typography>
            <SortIcon field="managerName" />
          </Box>
          <Box onClick={() => handleSort('positionName')} sx={{ flex: 1, minWidth: 120, cursor: 'pointer', display: 'flex', alignItems: 'center', userSelect: 'none' }}>
            <Typography variant="caption">Позиция</Typography>
            <SortIcon field="positionName" />
          </Box>
          <Box onClick={() => handleSort('status')} sx={{ flex: 0.8, minWidth: 100, cursor: 'pointer', display: 'flex', alignItems: 'center', userSelect: 'none' }}>
            <Typography variant="caption">Статус</Typography>
            <SortIcon field="status" />
          </Box>
          <Box onClick={() => handleSort('dueDate')} sx={{ flex: 0.8, minWidth: 90, cursor: 'pointer', display: 'flex', alignItems: 'center', userSelect: 'none' }}>
            <Typography variant="caption">Срок</Typography>
            <SortIcon field="dueDate" />
          </Box>
          <Box onClick={() => handleSort('updatedAt')} sx={{ flex: 1, minWidth: 140, cursor: 'pointer', display: 'flex', alignItems: 'center', userSelect: 'none' }}>
            <Typography variant="caption">Изменён</Typography>
            <SortIcon field="updatedAt" />
          </Box>
          <Typography variant="caption" sx={{ flex: 0.8, minWidth: 100 }}>Файл</Typography>
          <Box sx={{ width: 48 }} />
        </Box>

        {sortedPositions.length === 0 ? (
          <Box display="flex" justifyContent="center" alignItems="center" height={200}>
            <Typography color="text.secondary">Нет позиций</Typography>
          </Box>
        ) : (
          sortedPositions.map((item) => (
            <PositionRow
              key={`${item.order.id}-${item.index}`}
              position={item.position}
              order={item.order}
              index={item.index}
              onNavigate={(id) => navigate(`/orders/${id}`)}
            />
          ))
        )}

        {hasNextPage && (
          <Box display="flex" justifyContent="center" p={2}>
            <Typography
              variant="body2"
              color="primary"
              sx={{ cursor: 'pointer' }}
              onClick={() => fetchNextPage()}
            >
              {isFetchingNextPage ? 'Загрузка...' : 'Загрузить ещё'}
            </Typography>
          </Box>
        )}
      </Paper>
    </Container>
  );
};

export default ProductionOrdersPositionsList;
