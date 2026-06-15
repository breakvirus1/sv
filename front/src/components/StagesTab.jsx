import { Box, Paper, Typography, Chip } from '@mui/material';

const StagesTab = ({ stages }) => {
  if (!stages?.length) {
    return <Typography>Этапы производства не заданы</Typography>;
  }

  return (
    <Box>
      {stages.map((stage) => (
        <Paper key={stage.id} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="subtitle1">{stage.workshop?.name}</Typography>
            <Chip
              label={stage.status}
              color={stage.status === 'DONE' ? 'success' : stage.status === 'IN_PROGRESS' ? 'primary' : 'default'}
              size="small"
            />
          </Box>
          <Box mt={1}>
            <Typography variant="body2" color="text.secondary">
              Срок: {stage.dueDate || '—'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Ждать предыдущие: {stage.waitPrevious ? 'Да' : 'Нет'}
            </Typography>
            {stage.note && (
              <Typography variant="body2" color="text.secondary">
                Примечание: {stage.note}
              </Typography>
            )}
          </Box>
        </Paper>
      ))}
    </Box>
  );
};

export default StagesTab;
