import { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Grid,
  CircularProgress,
  Alert
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';

const ClientInfo = ({ clientId }) => {
  const { data: client, isLoading, error } = useQuery({
    queryKey: ['client', clientId],
    queryFn: async () => {
      const response = await api.get(`/api/v1/clients/${clientId}`);
      return response.data;
    },
    enabled: !!clientId
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error">
        Ошибка загрузки данных клиента
      </Alert>
    );
  }

  if (!client) {
    return null;
  }

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Информация о клиенте
      </Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Наименование
          </Typography>
          <Typography variant="body1">{client.name}</Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Тип
          </Typography>
          <Typography variant="body1">
            {client.type === 'PRIVATE' ? 'Частник' : 'Компания'}
          </Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Контактное лицо
          </Typography>
          <Typography variant="body1">{client.contactPerson || '-'}</Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Телефон
          </Typography>
          <Typography variant="body1">{client.phone || '-'}</Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Email
          </Typography>
          <Typography variant="body1">{client.email || '-'}</Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            ИНН
          </Typography>
          <Typography variant="body1">{client.inn || '-'}</Typography>
        </Grid>
        <Grid item xs={12}>
          <Typography variant="body2" color="text.secondary">
            Адрес
          </Typography>
          <Typography variant="body1">{client.address || '-'}</Typography>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="body2" color="text.secondary">
            Процент добавки (priceplus)
          </Typography>
          <Typography variant="body1">
            {client.priceplus != null ? `${client.priceplus}%` : '-'}
          </Typography>
        </Grid>
      </Grid>
    </Paper>
  );
};

export default ClientInfo;