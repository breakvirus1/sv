import { Box, Typography, Divider, Paper } from '@mui/material';

const OrderDetailsCard = ({ order, calculatedData }) => {
  const displayPriceplus = calculatedData?.priceplus ?? order?.priceplus;
  const displayTotalWithPriceplus = calculatedData?.totalWithPriceplus ?? order?.totalWithPriceplus;
  
  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>Детали</Typography>
      <Box display="flex" flexDirection="column" gap={2}>
        <div>
          <Typography variant="body2" color="text.secondary">Дата заказа</Typography>
          <Typography variant="body1">{order?.orderDate || '—'}</Typography>
        </div>
        <div>
          <Typography variant="body2" color="text.secondary">Срок сдачи</Typography>
          <Typography variant="body1">{order?.dueDate || '—'}</Typography>
        </div>
        <div>
          <Typography variant="body2" color="text.secondary">Менеджер</Typography>
          <Typography variant="body1">{order?.manager?.fullName || '—'}</Typography>
        </div>
        <div>
          <Typography variant="body2" color="text.secondary">Наценка (priceplus)</Typography>
          <Typography variant="body1">{displayPriceplus != null ? `${displayPriceplus}%` : '—'}</Typography>
        </div>
        <div>
          <Typography variant="body2" color="text.secondary">Сумма без наценки</Typography>
          <Typography variant="body1">{order?.totalAmount != null ? `${Number(order.totalAmount).toFixed(2)} ₽` : '—'}</Typography>
        </div>
        <div>
          <Typography variant="body2" color="text.secondary">Сумма с наценкой</Typography>
          <Typography variant="body1" fontWeight={600}>{displayTotalWithPriceplus != null ? `${Number(displayTotalWithPriceplus).toFixed(2)} ₽` : (order?.totalAmount != null ? `${Number(order.totalAmount).toFixed(2)} ₽` : '—')}</Typography>
        </div>
        <Divider />
        <div>
          <Typography variant="body2" color="text.secondary">Изменен</Typography>
          <Typography variant="body1">
            {order?.updatedAt ? new Date(order.updatedAt).toLocaleString() : '—'}
          </Typography>
        </div>
      </Box>
    </Paper>
  );
};

export default OrderDetailsCard;
