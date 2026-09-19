import { Box, Paper, Typography } from '@mui/material';

const HistoryTab = ({ history }) => {
  if (!history) {
    return <Typography>Нет истории изменений</Typography>;
  }

  const entries = history.split('\n').filter(entry => entry.trim());

  return (
    <Box>
      {entries.map((entry, idx) => (
        <Paper key={idx} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Typography variant="body2" component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: '0.85rem', margin: 0 }}>
            {entry}
          </Typography>
        </Paper>
      ))}
    </Box>
  );
};

export default HistoryTab;
