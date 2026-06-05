import { Box, Paper, Typography, Chip, Link, Table, TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import { Download } from '@mui/icons-material';

const PositionsTab = ({ materials = [], items = [], orderId, calculatedData }) => {
  const positions = calculatedData?.materials || ((materials && materials.length > 0) ? materials : items);

  if (!positions || positions.length === 0) {
    return <Typography>Нет позиций в заказе</Typography>;
  }

  const isCalculated = !!calculatedData;
  const isMaterialBased = (materials && materials.length > 0) || isCalculated;

  const fmt = (v) => v != null ? `${Number(v).toFixed(2)} ₽` : '—';

  return (
    <Box>
      {positions.map((pos) => {
        const mat = isMaterialBased && !isCalculated ? pos.material : null;
        const name = isCalculated ? pos.materialName : (mat?.name || pos.name || pos.materialName || '—');
        const unit = isCalculated ? '' : (mat?.unit || '');
        const widthM = isCalculated ? pos.widthM : (pos.widthM != null ? pos.widthM : null);
        const heightM = isCalculated ? pos.heightM : (pos.heightM != null ? pos.heightM : null);
        const quantity = pos.quantity != null ? pos.quantity : (pos.widthM != null ? `${pos.widthM} м` : '—');
        const cost = isCalculated ? pos.costPriceplus : pos.cost;
        const readyDate = pos.readyDate || '—';
        const operations = pos.operations || [];
        const operationsTotal = pos.operationsTotalPriceplus != null ? pos.operationsTotalPriceplus : (isCalculated ? pos.operationsTotal : null);
        const posTotal = cost != null && operationsTotal != null
          ? (Number(cost) + Number(operationsTotal)).toFixed(2)
          : null;

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
              <Typography variant="h6">{cost != null ? fmt(cost) : '—'}</Typography>
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
                <Typography variant="caption" color="text.secondary" gutterBottom display="block">
                  Операции:
                </Typography>
                <Table size="small" sx={{ mt: 0.5 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ py: 0.5, px: 1, fontWeight: 600 }}>Операция</TableCell>
                      <TableCell sx={{ py: 0.5, px: 1, fontWeight: 600 }}>Цена за ед.</TableCell>
                      <TableCell sx={{ py: 0.5, px: 1, fontWeight: 600 }}>Кол-во</TableCell>
                      <TableCell sx={{ py: 0.5, px: 1, fontWeight: 600, textAlign: 'right' }}>Сумма</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {operations.map((op, idx) => (
                      <TableRow key={idx}>
                        <TableCell sx={{ py: 0.5, px: 1 }}>{op.operationName}</TableCell>
                        <TableCell sx={{ py: 0.5, px: 1 }}>{op.pricePerUnit != null ? fmt(op.pricePerUnit) : '—'}</TableCell>
                        <TableCell sx={{ py: 0.5, px: 1 }}>{op.calculatedQuantity != null ? op.calculatedQuantity : '—'}</TableCell>
                        <TableCell sx={{ py: 0.5, px: 1, textAlign: 'right' }}>{op.subtotal != null ? fmt(op.subtotal) : '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
                {operationsTotal != null && (
                  <Box sx={{ mt: 1, display: 'flex', justifyContent: 'flex-end' }}>
                    <Typography variant="body2" fontWeight={600}>
                      Итого по операциям: {fmt(operationsTotal)}
                    </Typography>
                  </Box>
                )}
              </Box>
            )}

            {posTotal != null && (
              <Box sx={{ mt: 1, pt: 1, borderTop: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'flex-end' }}>
                <Typography variant="subtitle2" fontWeight={700}>
                  Итого по позиции: {posTotal} ₽
                </Typography>
              </Box>
            )}

            {pos.fileUrl && (
              <Box sx={{ mt: 1.5, pt: 1.5, borderTop: '1px solid', borderColor: 'divider' }}>
                <Link
                  href={pos.fileUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}
                >
                  <Download fontSize="small" />
                  {pos.fileOriginalName || 'Скачать файл'}
                </Link>
              </Box>
            )}
          </Paper>
        );
      })}
    </Box>
  );
};

export default PositionsTab;