import { Box, Paper, Typography } from '@mui/material';

const PaymentsTab = ({ payments }) => {
  if (!payments?.length) {
    return <Typography>Нет оплат</Typography>;
  }

  return (
    <Box>
      {payments.map((payment) => (
        <Paper key={payment.id} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Box>
              <Typography variant="subtitle1">{payment.paymentType}</Typography>
              <Typography variant="body2" color="text.secondary">
                {payment.paymentDate}
              </Typography>
            </Box>
            <Typography variant="h6">{payment.amount?.toFixed(2)} ₽</Typography>
          </Box>
          {payment.details && (
            <Typography variant="body2" color="text.secondary" mt={1}>
              {payment.details}
            </Typography>
          )}
        </Paper>
      ))}
    </Box>
  );
};

export default PaymentsTab;
