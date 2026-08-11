import { useState, useEffect } from 'react';
import { Box, Paper, Typography, Button, IconButton, Collapse, Container, Chip } from '@mui/material';
import { Notifications, Close, CheckCircle, OpenInNew } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';

const NotificationsList = () => {
  const [expandedId, setExpandedId] = useState(null);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: async () => {
      const response = await api.get('/api/v1/notifications');
      return response.data || [];
    },
    enabled: true,
  });

  const markAsReadMutation = useMutation({
    mutationFn: (id) => api.patch(`/api/v1/notifications/${id}/read`),
    onSuccess: () => {
      queryClient.invalidateQueries(['notifications']);
      queryClient.invalidateQueries(['unreadNotifications']);
    },
  });

  const markAllAsReadMutation = useMutation({
    mutationFn: () => api.patch('/api/v1/notifications/read-all'),
    onSuccess: () => {
      queryClient.invalidateQueries(['notifications']);
      queryClient.invalidateQueries(['unreadNotifications']);
    },
  });

  const handleNotificationClick = (notification) => {
    if (!notification.readed) {
      markAsReadMutation.mutate(notification.id);
    }
    if (notification.referenceType && notification.referenceId) {
      if (notification.referenceType === 'COMMENT' || notification.referenceType === 'REPLY' || notification.referenceType === 'ORDER') {
        navigate(`/orders/${notification.referenceId}`);
      } else {
        navigate('/orders');
      }
    } else {
      navigate('/orders');
    }
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'COMMENT':
        return <CheckCircle fontSize="small" color="primary" />;
      case 'REPLY':
        return <OpenInNew fontSize="small" color="secondary" />;
      case 'ORDER_READY':
        return <CheckCircle fontSize="small" color="success" />;
      default:
        return <Notifications fontSize="small" />;
    }
  };

  const getNotificationLabel = (type) => {
    switch (type) {
      case 'COMMENT':
        return 'Комментарий';
      case 'REPLY':
        return 'Ответ';
      case 'ORDER_READY':
        return 'Заказ готов';
      default:
        return 'Уведомление';
    }
  };

  if (isLoading) {
    return (
      <Container sx={{ maxWidth: 1600, mx: 'auto', mt: 4, px: 2.5 }}>
        <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2, justifyContent: 'center', alignItems: 'center' }}>
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  return (
    <Container sx={{ maxWidth: 1600, mx: 'auto', mt: 4, px: 2.5 }}>
      <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2} flexShrink={0}>
          <Typography variant="h4">Уведомления</Typography>
          {notifications.some(n => !n.readed) && (
            <Button variant="outlined" onClick={() => markAllAsReadMutation.mutate()}>
              Отметить все как прочитанные
            </Button>
          )}
        </Box>

        {notifications.length === 0 ? (
          <Paper sx={{ p: 4, textAlign: 'center' }}>
            <Notifications sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
            <Typography color="text.secondary">Нет уведомлений</Typography>
          </Paper>
        ) : (
          <Paper sx={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            {notifications.map((notification) => (
              <Box
                key={notification.id}
                sx={{
                  p: 2,
                  borderBottom: '1px solid',
                  borderColor: 'divider',
                  cursor: 'pointer',
                  '&:hover': { bgcolor: 'action.hover' },
                  bgcolor: notification.readed ? 'background.paper' : 'action.hover',
                }}
                onClick={() => handleNotificationClick(notification)}
              >
                <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                  <Box flex={1}>
                    <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                      {getNotificationIcon(notification.type)}
                      <Chip 
                        label={getNotificationLabel(notification.type)} 
                        size="small" 
                        color={notification.readed ? "default" : "primary"}
                        variant="outlined"
                      />
                      {!notification.readed && <CheckCircle fontSize="small" color="primary" />}
                    </Box>
                    <Typography
                      variant="body1"
                      fontWeight={notification.readed ? 'normal' : 'bold'}
                      mb={0.5}
                    >
                      {notification.message}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {notification.createdAt ? new Date(notification.createdAt).toLocaleString() : ''}
                    </Typography>
                  </Box>
                  <Box display="flex" gap={0.5}>
                    <IconButton
                      size="small"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleNotificationClick(notification);
                      }}
                    >
                      <OpenInNew fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={(e) => {
                        e.stopPropagation();
                        if (!notification.readed) {
                          markAsReadMutation.mutate(notification.id);
                        }
                      }}
                    >
                      <CheckCircle fontSize="small" />
                    </IconButton>
                  </Box>
                </Box>
              </Box>
            ))}
          </Paper>
        )}
      </Box>
    </Container>
  );
};

export default NotificationsList;
