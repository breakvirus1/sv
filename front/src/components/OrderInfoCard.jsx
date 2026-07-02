import { Box, Typography, Divider, Paper, IconButton, Chip } from '@mui/material';
import { Info } from '@mui/icons-material';
import { getStatusLabel, getStatusColor } from '../utils/orderUtils';

const OrderInfoCard = ({ order, calculatedData, onClientInfoClick }) => {
  const displayTotalWithPriceplus = calculatedData?.totalWithPriceplus ?? order?.totalWithPriceplus;
  const statusKey = order?.productionStage || order?.status;

  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Информация о заказе</Typography>
        {statusKey && (
          <Chip label={getStatusLabel(statusKey)} color={getStatusColor(statusKey)} size="small" />
        )}
      </Box>
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
        <Box>
          <Typography variant="body2" color="text.secondary">Дата заказа</Typography>
          <Typography variant="body1">{order?.orderDate || '—'}</Typography>
        </Box>
        <Box>
          <Typography variant="body2" color="text.secondary">Срок сдачи</Typography>
          <Typography variant="body1">{order?.dueDate || '—'}</Typography>
        </Box>
        <Box>
          <Typography variant="body2" color="text.secondary">Ответственный менеджер</Typography>
          <Typography variant="body1">{order?.manager?.fullName || '—'}</Typography>
        </Box>
        <Divider />
        <Box>
          <Typography variant="body2" color="text.secondary">Сумма для клиента</Typography>
          <Typography variant="h6">{displayTotalWithPriceplus != null ? `${Number(displayTotalWithPriceplus).toFixed(2)} ₽` : '—'}</Typography>
        </Box>
        <Box>
          <Typography variant="body2" color="text.secondary">Оплачено</Typography>
          <Typography variant="body1" color="success.main">{order?.paidAmount != null ? `${Number(order.paidAmount).toFixed(2)} ₽` : '—'}</Typography>
        </Box>
        <Box>
          <Typography variant="body2" color="text.secondary">Долг</Typography>
          <Typography variant="body1" color="error.main">{order?.debtAmount != null ? `${Number(order.debtAmount).toFixed(2)} ₽` : '—'}</Typography>
        </Box>
      </Box>
    </Paper>
  );
};

export default OrderInfoCard;
