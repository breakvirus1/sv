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
import { Person, ExpandMore, ExpandLess, AttachFile } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { useState, useEffect, useRef } from 'react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const API_BASE = import.meta.env.VITE_API_URL || '';

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

const PAGE_SIZE = 50;

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

const OrderRow = ({ order, onNavigate }) => {
  const [expanded, setExpanded] = useState(false);

  const { data: orderDetail, isLoading: detailLoading } = useQuery({
    queryKey: ['orderDetail', order.id],
    queryFn: () => fetchOrderDetail(order.id),
    enabled: expanded,
    staleTime: 30000,
  });

  const displayOrder = orderDetail || order;

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
        <Box sx={{ flex: 0.8, minWidth: 100 }}>
          <Typography variant="body2" fontWeight="medium">{order.orderNumber}</Typography>
        </Box>
        <Box sx={{ flex: 1.5, minWidth: 120 }}>
          <Typography variant="body2">{order.client?.name || '—'}</Typography>
        </Box>
        <Box sx={{ flex: 1.2, minWidth: 120 }} display="flex" alignItems="center" gap={0.5}>
          <Person fontSize="small" color="action" />
          <Typography variant="body2">{order.manager?.fullName || '—'}</Typography>
        </Box>
        <Box sx={{ flex: 1, minWidth: 100 }}>
          <Chip
            label={getStatusLabel(order.status)}
            color={getStatusColor(order.status)}
            size="small"
          />
        </Box>
        <Box sx={{ flex: 1, minWidth: 140 }}>
          <Typography variant="body2">
            {order.updatedAt ? new Date(order.updatedAt).toLocaleString('ru-RU', {
              day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit'
            }) : ''}
          </Typography>
        </Box>
        <Box sx={{ flex: 0.8, minWidth: 90 }}>
          <Typography variant="body2">{order.dueDate || '—'}</Typography>
        </Box>
        <IconButton size="small">
          {expanded ? <ExpandLess /> : <ExpandMore />}
        </IconButton>
      </Box>

      <Collapse in={expanded}>
        <Box sx={{ p: 2, bgcolor: 'grey.50', borderBottom: '1px solid', borderColor: 'divider' }}>
          {detailLoading ? (
            <Box display="flex" justifyContent="center" p={2}>
              <CircularProgress size={24} />
            </Box>
          ) : (
            <>
          <Box display="flex" gap={3}>
            {/* Позиции заказа */}
            <Box flex={1}>
              <Typography variant="subtitle2" gutterBottom>Позиции</Typography>
              {displayOrder.items && displayOrder.items.length > 0 ? (
                <Box component="ul" sx={{ pl: 2, m: 0 }}>
                  {displayOrder.items.map((item, index) => (
                    <Box component="li" key={index} sx={{ mb: 0.5 }}>
                      <Typography variant="body2">
                        {item.name || `Позиция ${index + 1}`}
                        {item.quantity && ` — ${item.quantity} шт`}
                      </Typography>
                    </Box>
                  ))}
                </Box>
              ) : (
                <Typography variant="body2" color="text.secondary">Нет позиций</Typography>
              )}

              {/* Операции из позиций */}
              {displayOrder.items && displayOrder.items.some(item => item.operations && item.operations.length > 0) && (
                <Box mt={2}>
                  <Typography variant="subtitle2" gutterBottom>Операции</Typography>
                  <Box component="ul" sx={{ pl: 2, m: 0 }}>
                    {displayOrder.items.flatMap((item, itemIndex) => 
                      (item.operations || []).map((op, opIndex) => (
                        <Box component="li" key={`${itemIndex}-${opIndex}`} sx={{ mb: 0.5 }}>
                          <Typography variant="body2">
                            {op.name || op.operationName || `Операция ${opIndex + 1}`}
                          </Typography>
                        </Box>
                      ))
                    )}
                  </Box>
                </Box>
              )}
            </Box>

            {/* Файлы */}
            <Box flex={1}>
              <Typography variant="subtitle2" gutterBottom>Файлы</Typography>
              {displayOrder.items && displayOrder.items.some(item => item.fileUrl) ? (
                <Box display="flex" flexDirection="column" gap={1}>
                  {displayOrder.items.map((item, index) => (
                    item.fileUrl && (
                      <Box key={index} display="flex" alignItems="center" gap={1}>
                        <AttachFile fontSize="small" color="action" />
                        <Link
                          onClick={(e) => { e.stopPropagation(); downloadFile(item.fileUrl); }}
                          sx={{ cursor: 'pointer' }}
                          rel="noopener noreferrer"
                          sx={{ cursor: 'pointer' }}
                          onClick={(e) => e.stopPropagation()}
                        >
                          {item.name || `Файл ${index + 1}`}
                        </Link>
                      </Box>
                    )
                  ))}
                </Box>
              ) : (
                <Typography variant="body2" color="text.secondary">Нет макетов</Typography>
              )}
            </Box>
          </Box>

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
            </>
          )}
        </Box>
      </Collapse>
    </Box>
  );
};

const ProductionOrderList = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const loadingRef = useRef(false);

  const statusFilter = searchParams.get('status');

  const {
    data,
    isLoading,
    error,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ['productionOrders', { status: statusFilter }],
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

  const getTitle = () => {
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
        </Box>
      </Box>

      <Paper sx={{ flex: 1, minHeight: 0, width: '100%', overflow: 'auto' }}>
        <Box sx={{ display: 'flex', p: 1.5, borderBottom: '2px solid', borderColor: 'divider', bgcolor: 'grey.100' }}>
          <Typography variant="caption" sx={{ flex: 0.8, minWidth: 100 }}>№ заказа</Typography>
          <Typography variant="caption" sx={{ flex: 1.5, minWidth: 120 }}>Клиент</Typography>
          <Typography variant="caption" sx={{ flex: 1.2, minWidth: 120 }}>Менеджер</Typography>
          <Typography variant="caption" sx={{ flex: 1, minWidth: 100 }}>Статус</Typography>
          <Typography variant="caption" sx={{ flex: 1, minWidth: 140 }}>Изменён</Typography>
          <Typography variant="caption" sx={{ flex: 0.8, minWidth: 90 }}>Срок</Typography>
          <Box sx={{ width: 48 }} />
        </Box>

        {allOrders.length === 0 ? (
          <Box display="flex" justifyContent="center" alignItems="center" height={200}>
            <Typography color="text.secondary">Нет заказов</Typography>
          </Box>
        ) : (
          allOrders.map(order => (
            <OrderRow
              key={order.id}
              order={order}
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

export default ProductionOrderList;
