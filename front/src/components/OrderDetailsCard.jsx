import { Box, Typography, Divider, Paper } from '@mui/material';

const OrderDetailsCard = ({ order }) => {
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
