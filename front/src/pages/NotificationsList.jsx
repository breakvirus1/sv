import { useState, useEffect } from 'react';
import { Box, Paper, Typography, Button, IconButton, Collapse } from '@mui/material';
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
      switch (notification.referenceType) {
        case 'COMMENT':
          navigate(`/orders/${notification.referenceId}`);
          break;
        case 'REPLY':
          navigate(`/orders/${notification.referenceId}`);
          break;
        case 'ORDER':
          navigate(`/orders/${notification.referenceId}`);
          break;
        default:
          navigate('/orders');
      }
    } else {
      navigate('/orders');
    }
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <Typography>Загрузка уведомлений...</Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
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
        <Box>
          {notifications.map((notification) => (
            <Paper
              key={notification.id}
              sx={{
                p: 2,
                mb: 2,
                cursor: 'pointer',
                border: notification.readed ? 'none' : '1px solid',
                borderColor: 'primary.main',
                bgcolor: notification.readed ? 'background.paper' : 'action.hover',
              }}
              onClick={() => handleNotificationClick(notification)}
            >
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box flex={1}>
                  <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                    {!notification.readed && <CheckCircle fontSize="small" color="primary" />}
                    <Typography
                      variant="body1"
                      fontWeight={notification.readed ? 'normal' : 'bold'}
                    >
                      {notification.message}
                    </Typography>
                  </Box>
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
            </Paper>
          ))}
        </Box>
      )}
    </Box>
  );
};

export default NotificationsList;
