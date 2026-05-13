import { Box, Typography, Divider, Paper } from '@mui/material';

const OrderInfoCard = ({ order }) => {
  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Typography variant="h6" gutterBottom>Информация о заказе</Typography>
      <Box display="flex" flexDirection="column" gap={2}>
        <Box>
          <Typography variant="body2" color="text.secondary">Клиент</Typography>
          <Typography variant="body1">{order?.client?.name}</Typography>
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
