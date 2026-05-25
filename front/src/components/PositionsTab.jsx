import { Box, Paper, Typography, Chip } from '@mui/material';

const PositionsTab = ({ materials = [], items = [], orderId, calculatedData }) => {
  console.log('=== PositionsTab render ===');
  console.log('orderId:', orderId);
  console.log('calculatedData available:', !!calculatedData);
  console.log('calculatedData materials count:', calculatedData?.materials?.length);
  console.log('materials count:', materials?.length);
  console.log('items count:', items?.length);
  
  // Используем расчетные данные если доступны (с наценкой), иначе берем напрямую
  const positions = calculatedData?.materials || (materials && materials.length > 0) ? materials : items;

  if (!positions || positions.length === 0) {
    return <Typography>Нет позиций в заказе</Typography>;
  }

  const isCalculated = !!calculatedData;
  const isMaterialBased = (materials && materials.length > 0) || isCalculated;

  return (
    <Box>
      {positions.map((pos) => {
        const mat = isMaterialBased && !isCalculated ? pos.material : null;
        const name = isCalculated ? pos.materialName : (mat?.name || pos.name || '—');
        const unit = isCalculated ? '' : (mat?.unit || '');
        const widthM = isCalculated ? pos.widthM : (pos.widthM != null ? pos.widthM : null);
        const heightM = isCalculated ? pos.heightM : (pos.heightM != null ? pos.heightM : null);
        const quantity = pos.quantity != null ? pos.quantity : (pos.widthM != null ? `${pos.widthM} м` : '—');
        // Используем costPriceplus если доступен (расчитанный), иначе используем cost
        const cost = isCalculated ? pos.costPriceplus : pos.cost;
        const readyDate = pos.readyDate || '—';
        const operations = pos.operations || [];

        let sizeStr = '—';
        if (widthM != null && heightM != null) {
          sizeStr = `${widthM} × ${heightM} м`;
        } else if (widthM != null) {
          sizeStr = `${widthM} м`;
        }

        return (
          <Paper key={pos.id} sx={{ p: 2, mb: 2 }} variant="outlined">
            <Box display="flex" justifyContent="space-between" alignItems="center">
              <Box>
                <Typography variant="subtitle1">{name}</Typography>
                {unit && (
                  <Typography variant="caption" color="text.secondary">{unit}</Typography>
                )}
              </Box>
              <Typography variant="h6">{cost != null ? `${Number(cost).toFixed(2)} ₽` : '—'}</Typography>
            </Box>

            <Box sx={{ mt: 1, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
              <Typography variant="body2" color="text.secondary">
                Размер: {sizeStr}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Кол-во: {quantity}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Срок: {readyDate}
              </Typography>
            </Box>

            {operations.length > 0 && (
              <Box sx={{ mt: 1.5, pt: 1.5, borderTop: '1px solid', borderColor: 'divider' }}>
                <Typography variant="caption" color="text.secondary" gutterBottom>
                  Операции:
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 0.5 }}>
                  {operations.map((op, idx) => (
                    <Chip
                      key={idx}
                      label={`${op.operationName}${op.subtotal != null ? ` — ${Number(op.subtotal).toFixed(2)} ₽` : ''}`}
                      size="small"
                      variant="outlined"
                    />
                  ))}
                </Box>
              </Box>
            )}
          </Paper>
        );
      })}
    </Box>
  );
};

export default PositionsTab;
