import { useState, useEffect } from 'react';
import { Box, Paper, Typography, TextField, Button, IconButton, Collapse } from '@mui/material';
import { ExpandMore, ExpandLess, Send } from '@mui/icons-material';
import api from '../services/api';

const CommentsTab = ({ orderId }) => {
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [replyText, setReplyText] = useState({});
  const [expandedReplies, setExpandedReplies] = useState({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!orderId) return;
    setLoading(true);
    api.get(`/api/v1/comments/order/${orderId}`)
      .then(res => setComments(res.data))
      .catch(() => setComments([]))
      .finally(() => setLoading(false));
  }, [orderId]);

  const handleAddComment = async () => {
    if (!newComment.trim()) return;
    await api.post(`/api/v1/comments/order/${orderId}`, { body: newComment });
    setNewComment('');
    const res = await api.get(`/api/v1/comments/order/${orderId}`);
    setComments(res.data);
  };

  const handleAddReply = async (parentCommentId) => {
    const text = replyText[parentCommentId];
    if (!text?.trim()) return;
    await api.post(`/api/v1/comment-replies`, { parentCommentId, body: text });
    setReplyText(prev => ({ ...prev, [parentCommentId]: '' }));
    const res = await api.get(`/api/v1/comments/order/${orderId}`);
    setComments(res.data);
  };

  const toggleReplies = (commentId) => {
    setExpandedReplies(prev => ({ ...prev, [commentId]: !prev[commentId] }));
  };

  if (loading) return <Typography>Загрузка комментариев...</Typography>;
  if (!comments.length) return <Typography>Нет комментариев</Typography>;

  return (
    <Box>
      {comments.map(comment => (
        <Paper key={comment.id} sx={{ p: 2, mb: 2 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
            <Typography variant="subtitle1">Сотрудник #{comment.employeeId}</Typography>
            <Typography variant="caption" color="text.secondary">
              {comment.createdAt ? new Date(comment.createdAt).toLocaleString() : ''}
            </Typography>
          </Box>
          <Typography variant="body1" mb={1}>{comment.body}</Typography>
          <Box display="flex" alignItems="center" gap={1}>
            <Button size="small" onClick={() => toggleReplies(comment.id)}>
              {expandedReplies[comment.id] ? 'Скрыть ответы' : `Ответы (${comment.replies?.length || 0})`}
            </Button>
            <TextField
              size="small"
              placeholder="Ответить..."
              value={replyText[comment.id] || ''}
              onChange={e => setReplyText(prev => ({ ...prev, [comment.id]: e.target.value }))}
            />
            <IconButton size="small" onClick={() => handleAddReply(comment.id)}>
              <Send />
            </IconButton>
          </Box>
          <Collapse in={expandedReplies[comment.id]}>
            <Box ml={4} mt={1}>
              {comment.replies?.map(reply => (
                <Paper key={reply.id} sx={{ p: 1, mb: 1 }} variant="outlined">
                  <Typography variant="subtitle2">Сотрудник #{reply.employeeId}</Typography>
                  <Typography variant="body2">{reply.body}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {reply.createdAt ? new Date(reply.createdAt).toLocaleString() : ''}
                  </Typography>
                </Paper>
              ))}
            </Box>
          </Collapse>
        </Paper>
      ))}
      <Paper sx={{ p: 2, mt: 2 }} variant="outlined">
        <Typography variant="subtitle2" mb={1}>Новый комментарий</Typography>
        <TextField
          fullWidth
          multiline
          rows={2}
          value={newComment}
          onChange={e => setNewComment(e.target.value)}
          placeholder="Введите комментарий..."
        />
        <Button variant="contained" onClick={handleAddComment} sx={{ mt: 1 }}>Отправить</Button>
      </Paper>
    </Box>
  );
};

export default CommentsTab;
