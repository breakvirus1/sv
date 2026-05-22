import { useMemo } from 'react';
import { Box, Paper, Typography } from '@mui/material';

const PositionsTab = ({ materials = [], items = [] }) => {
  const rawList = (materials && materials.length > 0) ? materials : items;

  const displayList = useMemo(() => {
    const map = new Map();

    rawList.forEach((entry) => {
      const materialId = entry.material?.id ?? entry.materialId;
      if (!materialId) return;

      const quantity = Number(entry.quantity) || 0;
      const cost = Number(entry.cost) || 0;

      if (map.has(materialId)) {
        const existing = map.get(materialId);
        existing.quantity += quantity;
        existing.cost += cost;

        if (entry.operations && entry.operations.length > 0) {
          existing.operations = existing.operations || [];
          entry.operations.forEach(op => {
            if (!existing.operations.some(e => e.operationName === op.operationName)) {
              existing.operations.push(op);
            }
          });
        }
      } else {
        map.set(materialId, {
          ...entry,
          materialId,
          quantity: quantity,
          cost: cost,
          material: entry.material || (entry.materialId ? { id: entry.materialId, name: entry.name, unit: entry.unit, price: entry.price } : null),
          operations: entry.operations || []
        });
      }
    });

    return Array.from(map.values());
  }, [rawList]);

  if (!displayList?.length) {
    return <Typography>Нет позиций в заказе</Typography>;
  }

  const isMaterial = displayList[0]?.material !== undefined;

  return (
    <Box>
      {displayList.map((entry) => {
        const name = isMaterial ? entry.material?.name : entry.name;
        const cost = entry.cost;
        const readyDate = entry.readyDate;

        return (
          <Paper key={entry.materialId || entry.id} sx={{ p: 2, mb: 2 }} variant="outlined">
            <Box display="flex" justifyContent="space-between" alignItems="center">
              <Typography variant="subtitle1">{name}</Typography>
              <Typography variant="h6">{cost?.toFixed(2)} ₽</Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Срок: {readyDate || '—'}
            </Typography>
            {((entry.operations && entry.operations.length > 0) || (entry.operations && entry.operations.length > 0)) && (
              <Box sx={{ mt: 2, pl: 2, borderLeft: '3px solid #e0e0e0' }}>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Операции:
                </Typography>
                {(entry.operations || []).map((op, idx) => (
                  <Box key={idx} sx={{ display: 'flex', justifyContent: 'space-between', pl: 1, flexWrap: 'wrap' }}>
                    <Typography variant="body2">
                      {op.operationName}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {op.subtotal?.toFixed(2)} ₽
                    </Typography>
                  </Box>
                ))}
              </Box>
            )}
          </Paper>
        );
      })}
    </Box>
  );
};

export default PositionsTab;
