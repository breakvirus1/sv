import { useState, useEffect } from 'react';
import { Box, Paper, Typography, TextField, Button, IconButton, Collapse } from '@mui/material';
import { Send } from '@mui/icons-material';
import api from '../services/api';

const CommentsTab = ({ orderId }) => {
  const [comments, setComments] = useState([]);
  const [showCommentForm, setShowCommentForm] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [replyForms, setReplyForms] = useState({});
  const [replyTexts, setReplyTexts] = useState({});
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

  const fetchComments = async () => {
    const res = await api.get(`/api/v1/comments/order/${orderId}`);
    setComments(res.data);
  };

  const handleAddComment = async () => {
    if (!newComment.trim()) return;
    await api.post(`/api/v1/comments/order/${orderId}`, { body: newComment });
    setNewComment('');
    setShowCommentForm(false);
    await fetchComments();
  };

  const handleAddReply = async (parentId, parentType = 'comment') => {
    const key = parentType === 'comment' ? parentId : `reply-${parentId}`;
    const text = replyTexts[key];
    if (!text?.trim()) return;
    await api.post(`/api/v1/comment-replies`, { parentCommentId: parentId, body: text });
    setReplyTexts(prev => ({ ...prev, [key]: '' }));
    setReplyForms(prev => ({ ...prev, [key]: false }));
    await fetchComments();
  };

  const toggleReplyForm = (id, parentType = 'comment') => {
    const key = parentType === 'comment' ? id : `reply-${id}`;
    setReplyForms(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const toggleReplies = (commentId) => {
    setExpandedReplies(prev => ({ ...prev, [commentId]: !prev[commentId] }));
  };

  const renderReply = (reply, level = 1) => {
    const replyKey = `reply-${reply.id}`;
    const showReplyForm = replyForms[replyKey] || false;
    const indent = level * 2;

    return (
      <Box key={reply.id} sx={{ ml: indent, mt: 1.5 }}>
        <Paper sx={{ p: 1.5 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={0.5}>
            <Typography variant="subtitle2">Сотрудник #{reply.employeeId}</Typography>
            <Typography variant="caption" color="text.secondary">
              {reply.createdAt ? new Date(reply.createdAt).toLocaleString() : ''}
            </Typography>
          </Box>
          <Typography variant="body2" mb={1}>{reply.body}</Typography>
          <Box display="flex" alignItems="center" gap={1}>
            <Button size="small" onClick={() => toggleReplyForm(reply.id, 'reply')}>
              {showReplyForm ? 'Отмена' : 'Ответить'}
            </Button>
          </Box>
          {showReplyForm && (
            <Box sx={{ mt: 1, display: 'flex', gap: 1, alignItems: 'flex-start' }}>
              <TextField
                size="small"
                fullWidth
                multiline
                minRows={1}
                placeholder="Ваш ответ..."
                value={replyTexts[replyKey] || ''}
                onChange={e => setReplyTexts(prev => ({ ...prev, [replyKey]: e.target.value }))}
              />
              <IconButton size="small" onClick={() => handleAddReply(reply.id, 'reply')} sx={{ mt: 0.5 }}>
                <Send fontSize="small" />
              </IconButton>
            </Box>
          )}
        </Paper>
        {reply.replies?.length > 0 && (
          <Box mt={1}>
            {reply.replies.map(r => renderReply(r, level + 1))}
          </Box>
        )}
      </Box>
    );
  };

  if (loading) return <Typography>Загрузка комментариев...</Typography>;

  return (
    <Box>
      {comments.map(comment => {
        const showReplyForm = replyForms[comment.id] || false;
        return (
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
              <Button size="small" onClick={() => toggleReplyForm(comment.id, 'comment')}>
                {showReplyForm ? 'Отмена' : 'Ответить'}
              </Button>
            </Box>
            {showReplyForm && (
              <Box sx={{ mt: 1.5, display: 'flex', gap: 1, alignItems: 'flex-start' }}>
                <TextField
                  size="small"
                  fullWidth
                  multiline
                  minRows={2}
                  placeholder="Ваш ответ..."
                  value={replyTexts[comment.id] || ''}
                  onChange={e => setReplyTexts(prev => ({ ...prev, [comment.id]: e.target.value }))}
                />
                <IconButton size="small" onClick={() => handleAddReply(comment.id, 'comment')} sx={{ mt: 0.5 }}>
                  <Send />
                </IconButton>
              </Box>
            )}
            <Collapse in={expandedReplies[comment.id]}>
              <Box mt={1}>
                {comment.replies?.map(reply => renderReply(reply, 1))}
              </Box>
            </Collapse>
          </Paper>
        );
      })}
      {!comments.length && !showCommentForm && (
        <Button variant="outlined" onClick={() => setShowCommentForm(true)} sx={{ mt: 1 }}>
          Добавить комментарий
        </Button>
      )}
      {(showCommentForm || comments.length > 0) && (
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
          <Box sx={{ mt: 1, display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
            {showCommentForm && (
              <Button onClick={() => { setShowCommentForm(false); setNewComment(''); }}>
                Отмена
              </Button>
            )}
            <Button variant="contained" onClick={handleAddComment} disabled={!newComment.trim()}>
              Отправить
            </Button>
          </Box>
        </Paper>
      )}
    </Box>
  );
};

export default CommentsTab;

