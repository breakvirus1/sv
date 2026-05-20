import { useState } from 'react';
import { Box, Typography, Divider, Paper, IconButton } from '@mui/material';
import { Info } from '@mui/icons-material';

const OrderInfoCard = ({ order, onClientInfoClick }) => {
  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Typography variant="h6" gutterBottom>Информация о заказе</Typography>
      <Box display="flex" flexDirection="column" gap={2}>
        <Box display="flex" alignItems="center" gap={1}>
          <Box flex={1}>
            <Typography variant="body2" color="text.secondary">Клиент</Typography>
            <Typography variant="body1">{order?.client?.name}</Typography>
          </Box>
          {onClientInfoClick && order?.client?.id && (
            <IconButton
              size="small"
              onClick={() => onClientInfoClick(order.client.id)}
              sx={{ mt: 1.5 }}
            >
              <Info fontSize="small" />
            </IconButton>
          )}
        </Box>
        <Box>
          <Typography variant="body2" color="text.secondary">Описание</Typography>
          <Typography variant="body1">{order?.description || '—'}</Typography>
        </Box>
        <Divider />
        <Box>
          <Typography variant="body2" color="text.secondary">Сумма</Typography>
          <Typography variant="h6">{order?.totalAmount?.toFixed(2)} ₽</Typography>
        </Box>
        <Box>
          <Typography variant="body2" color="text.secondary">Оплачено</Typography>
          <Typography variant="body1" color="success.main">{order?.paidAmount?.toFixed(2)} ₽</Typography>
        </Box>
        <Box>
          <Typography variant="body2" color="text.secondary">Долг</Typography>
          <Typography variant="body1" color="error.main">{order?.debtAmount?.toFixed(2)} ₽</Typography>
        </Box>
      </Box>
    </Paper>
  );
};

export default OrderInfoCard;
