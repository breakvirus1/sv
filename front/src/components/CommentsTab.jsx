import { Box, Paper, Typography, Chip } from '@mui/material';

const CommentsTab = ({ comments }) => {
  if (!comments?.length) {
    return <Typography>Нет комментариев</Typography>;
  }

  return (
    <Box>
      {comments.map((comment) => (
        <Paper key={comment.id} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
            <Typography variant="subtitle1">{comment.author?.fullName || 'Система'}</Typography>
            <Typography variant="caption" color="text.secondary">
              {new Date(comment.timestamp).toLocaleString()}
            </Typography>
          </Box>
          <Typography variant="body1">{comment.message}</Typography>
          {comment.isInternal && (
            <Chip label="Внутренний" size="small" sx={{ mt: 1 }} />
          )}
        </Paper>
      ))}
    </Box>
  );
};

export default CommentsTab;
