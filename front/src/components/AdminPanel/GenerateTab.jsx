import { Box, Typography, Grid, Button, Alert } from '@mui/material';
import { Refresh } from '@mui/icons-material';

const GenerateTab = ({ generating, onGenerate }) => {
  return (
    <Box>
      <Typography variant="h6" gutterBottom>Генерация тестовых данных</Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <Button
            variant="contained"
            fullWidth
            startIcon={<Refresh />}
            onClick={() => onGenerate('clients')}
            disabled={generating.clients}
          >
            {generating.clients ? 'Генерация...' : 'Сгенерировать клиентов (20)'}
          </Button>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Button
            variant="contained"
            fullWidth
            startIcon={<Refresh />}
            onClick={() => onGenerate('materials')}
            disabled={generating.materials}
          >
            {generating.materials ? 'Генерация...' : 'Сгенерировать материалы (20)'}
          </Button>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Button
            variant="contained"
            fullWidth
            startIcon={<Refresh />}
            onClick={() => onGenerate('orders')}
            disabled={generating.orders}
          >
            {generating.orders ? 'Генерация...' : 'Сгенерировать заказы (20)'}
          </Button>
        </Grid>
      </Grid>
      <Alert severity="info" sx={{ mt: 3 }}>
        При генерации заказов необходимо иметь хотя бы одного клиента и сотрудника. Рекомендуется сначала сгенерировать их.
      </Alert>
    </Box>
  );
};

export default GenerateTab;
