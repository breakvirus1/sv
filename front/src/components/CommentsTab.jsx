import { useState, useEffect, useRef } from 'react';
import { Box, Paper, Typography, TextField, Button, IconButton, Collapse, Input, Chip } from '@mui/material';
import { Send, Image as ImageIcon, Close } from '@mui/icons-material';
import api from '../services/api';

const CommentsTab = ({ orderId, highlightCommentId, highlightReplyId }) => {
  const [comments, setComments] = useState([]);
  const [showCommentForm, setShowCommentForm] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [replyForms, setReplyForms] = useState({});
  const [replyTexts, setReplyTexts] = useState({});
  const [replyImages, setReplyImages] = useState({});
  const [expandedReplies, setExpandedReplies] = useState({});
  const [loading, setLoading] = useState(false);
  const [commentImage, setCommentImage] = useState(null);
  const [commentImagePreview, setCommentImagePreview] = useState(null);
  const fileInputRef = useRef(null);
  const replyFileInputRefs = useRef({});

  useEffect(() => {
    if (!orderId) return;
    setLoading(true);
    api.get(`/api/v1/comments/order/${orderId}`)
      .then(res => setComments(res.data))
      .catch(() => setComments([]))
      .finally(() => setLoading(false));
  }, [orderId]);

  useEffect(() => {
    if (!highlightReplyId) return;
    const parentCommentId = findParentCommentIdForReply(comments, highlightReplyId);
    if (parentCommentId != null) {
      setExpandedReplies(prev => ({ ...prev, [parentCommentId]: true }));
    }
  }, [highlightReplyId, comments]);

  useEffect(() => {
    if (!highlightCommentId && !highlightReplyId) return;
    const timer = setTimeout(() => {
      const el = document.getElementById(`comment-${highlightCommentId}`) || document.getElementById(`reply-${highlightReplyId}`);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        el.style.transition = 'background-color 0.5s';
        el.style.backgroundColor = 'rgba(25, 118, 210, 0.15)';
        setTimeout(() => {
          el.style.backgroundColor = '';
        }, 2000);
      }
    }, 600);
    return () => clearTimeout(timer);
  }, [highlightCommentId, highlightReplyId, comments, expandedReplies]);

  const findReplyById = (replies, replyId) => {
    for (const reply of replies || []) {
      if (reply.id === replyId) {
        return reply;
      }
      const found = findReplyById(reply.replies, replyId);
      if (found) return found;
    }
    return null;
  };

  const findParentCommentIdForReply = (comments, replyId) => {
    for (const comment of comments) {
      if (comment.replies?.some(r => r.id === replyId)) {
        return comment.id;
      }
      const nested = findReplyById(comment.replies, replyId);
      if (nested) {
        return comment.id;
      }
    }
    return null;
  };

  const fetchComments = async () => {
    const res = await api.get(`/api/v1/comments/order/${orderId}`);
    setComments(res.data);
  };

  const handleImageUpload = (file, isReply = false, parentId = null) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onloadend = () => {
      const base64 = reader.result;
      if (isReply) {
        const key = parentId ? `reply-${parentId}` : parentId;
        setReplyImages(prev => ({ ...prev, [key]: base64 }));
      } else {
        setCommentImage(base64);
        setCommentImagePreview(base64);
      }
    };
    reader.readAsDataURL(file);
  };

  const handlePaste = (e, isReply = false, parentId = null) => {
    const items = e.clipboardData?.items;
    if (!items) return;
    for (const item of items) {
      if (item.type.startsWith('image/')) {
        const file = item.getAsFile();
        handleImageUpload(file, isReply, parentId);
        break;
      }
    }
  };

  const handleAddComment = async () => {
    if (!newComment.trim() && !commentImage) return;
    await api.post(`/api/v1/comments/order/${orderId}`, { 
      body: newComment, 
      imageUrl: commentImage 
    });
    setNewComment('');
    setCommentImage(null);
    setCommentImagePreview(null);
    setShowCommentForm(false);
    await fetchComments();
  };

  const handleAddReply = async (parentId, parentType = 'comment') => {
    const key = parentType === 'comment' ? parentId : `reply-${parentId}`;
    const text = replyTexts[key];
    const image = replyImages[key];
    if (!text?.trim() && !image) return;

    const payload = { 
      parentCommentId: parentId, 
      body: text,
      imageUrl: image
    };

    if (parentType === 'reply') {
      payload.parentReplyId = parentId;
    }

    await api.post(`/api/v1/comment-replies`, payload);
    setReplyTexts(prev => ({ ...prev, [key]: '' }));
    setReplyImages(prev => ({ ...prev, [key]: null }));
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
        <Paper id={`reply-${reply.id}`} sx={{ p: 1.5 }} variant="outlined">
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={0.5}>
            <Typography variant="subtitle2">{reply.employeeName || `Сотрудник #${reply.employeeId}`}</Typography>
            <Typography variant="caption" color="text.secondary">
              {reply.createdAt ? new Date(reply.createdAt).toLocaleString() : ''}
            </Typography>
          </Box>
          <Typography variant="body2" mb={1}>{reply.body}</Typography>
          {reply.imageUrl && (
            <Box mb={1}>
              <img src={reply.imageUrl} alt="Прикреплённое изображение" style={{ maxWidth: '100%', maxHeight: 200, borderRadius: 4 }} />
            </Box>
          )}
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
                onPaste={(e) => handlePaste(e, true, reply.id)}
              />
              <input
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                ref={el => replyFileInputRefs.current[replyKey] = el}
                onChange={(e) => handleImageUpload(e.target.files[0], true, reply.id)}
              />
              <IconButton 
                size="small" 
                onClick={() => replyFileInputRefs.current[replyKey]?.click()}
                sx={{ mt: 0.5 }}
              >
                <ImageIcon fontSize="small" />
              </IconButton>
              <IconButton size="small" onClick={() => handleAddReply(reply.id, 'reply')} sx={{ mt: 0.5 }}>
                <Send fontSize="small" />
              </IconButton>
            </Box>
          )}
          {replyImages[replyKey] && (
            <Box sx={{ mt: 1, position: 'relative', display: 'inline-block' }}>
              <img src={replyImages[replyKey]} alt="Preview" style={{ maxWidth: 100, maxHeight: 100, borderRadius: 4 }} />
              <IconButton
                size="small"
                onClick={() => setReplyImages(prev => ({ ...prev, [replyKey]: null }))}
                sx={{ position: 'absolute', top: -8, right: -8, bgcolor: 'background.paper' }}
              >
                <Close fontSize="small" />
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
          <Paper id={`comment-${comment.id}`} key={comment.id} sx={{ p: 2, mb: 2 }} variant="outlined">
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="subtitle1">{comment.employeeName || `Сотрудник #${comment.employeeId}`}</Typography>
              <Typography variant="caption" color="text.secondary">
                {comment.createdAt ? new Date(comment.createdAt).toLocaleString() : ''}
              </Typography>
            </Box>
            <Typography variant="body1" mb={1}>{comment.body}</Typography>
            {comment.imageUrl && (
              <Box mb={1}>
                <img src={comment.imageUrl} alt="Прикреплённое изображение" style={{ maxWidth: '100%', maxHeight: 200, borderRadius: 4 }} />
              </Box>
            )}
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
                  fullWidth
                  multiline
                  minRows={2}
                  placeholder="Ваш ответ..."
                  value={replyTexts[comment.id] || ''}
                  onChange={e => setReplyTexts(prev => ({ ...prev, [comment.id]: e.target.value }))}
                  onPaste={(e) => handlePaste(e, true, comment.id)}
                />
                <input
                  type="file"
                  accept="image/*"
                  style={{ display: 'none' }}
                  ref={el => replyFileInputRefs.current[comment.id] = el}
                  onChange={(e) => handleImageUpload(e.target.files[0], true, comment.id)}
                />
                <IconButton 
                  size="small" 
                  onClick={() => replyFileInputRefs.current[comment.id]?.click()}
                  sx={{ mt: 0.5 }}
                >
                  <ImageIcon />
                </IconButton>
                <IconButton size="small" onClick={() => handleAddReply(comment.id, 'comment')} sx={{ mt: 0.5 }}>
                  <Send />
                </IconButton>
              </Box>
            )}
            {replyImages[comment.id] && (
              <Box sx={{ mt: 1, position: 'relative', display: 'inline-block' }}>
                <img src={replyImages[comment.id]} alt="Preview" style={{ maxWidth: 100, maxHeight: 100, borderRadius: 4 }} />
                <IconButton
                  size="small"
                  onClick={() => setReplyImages(prev => ({ ...prev, [comment.id]: null }))}
                  sx={{ position: 'absolute', top: -8, right: -8, bgcolor: 'background.paper' }}
                >
                  <Close fontSize="small" />
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
            onPaste={(e) => handlePaste(e, false)}
          />
          <input
            type="file"
            accept="image/*"
            style={{ display: 'none' }}
            ref={fileInputRef}
            onChange={(e) => handleImageUpload(e.target.files[0], false)}
          />
          {commentImagePreview && (
            <Box sx={{ mt: 1, position: 'relative', display: 'inline-block' }}>
              <img src={commentImagePreview} alt="Preview" style={{ maxWidth: 100, maxHeight: 100, borderRadius: 4 }} />
              <IconButton
                size="small"
                onClick={() => { setCommentImage(null); setCommentImagePreview(null); }}
                sx={{ position: 'absolute', top: -8, right: -8, bgcolor: 'background.paper' }}
              >
                <Close fontSize="small" />
              </IconButton>
            </Box>
          )}
          <Box sx={{ mt: 1, display: 'flex', gap: 1, justifyContent: 'space-between', alignItems: 'center' }}>
            <Box>
              <input
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                id="comment-image-upload"
                onChange={(e) => handleImageUpload(e.target.files[0], false)}
              />
              <label htmlFor="comment-image-upload">
                <Button size="small" component="span" variant="outlined" startIcon={<ImageIcon />}>
                  Изображение
                </Button>
              </label>
            </Box>
            <Box>
              {showCommentForm && (
                <Button onClick={() => { setShowCommentForm(false); setNewComment(''); setCommentImage(null); setCommentImagePreview(null); }} sx={{ mr: 1 }}>
                  Отмена
                </Button>
              )}
              <Button variant="contained" onClick={handleAddComment} disabled={!newComment.trim() && !commentImage}>
                Отправить
              </Button>
            </Box>
          </Box>
        </Paper>
      )}
    </Box>
  );
};

export default CommentsTab;
