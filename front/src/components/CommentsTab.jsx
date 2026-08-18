import { useState, useEffect, useLayoutEffect, useRef } from 'react';
import { Box, Paper, Typography, TextField, Button, IconButton, Collapse, Input, Chip } from '@mui/material';
import { Send, Image as ImageIcon, Close } from '@mui/icons-material';
import api, { API_BASE_URL } from '../services/api';

const CommentsTab = ({ orderId, highlightCommentId, highlightReplyId }) => {
  const getImageUrl = (url) => {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return `${API_BASE_URL}${url}`;
  };

  const [comments, setComments] = useState([]);
  const [showCommentForm, setShowCommentForm] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [replyForms, setReplyForms] = useState({});
  const [replyTexts, setReplyTexts] = useState({});
  const [expandedReplies, setExpandedReplies] = useState({});
  const [loading, setLoading] = useState(false);
  const [commentImages, setCommentImages] = useState([]);
  const [replyImagesMap, setReplyImagesMap] = useState({});
  const fileInputRef = useRef(null);
  const replyFileInputRefs = useRef({});
  const highlightAppliedRef = useRef(null);

  useEffect(() => {
    if (!orderId) return;
    setLoading(true);
    api.get(`/api/v1/comments/order/${orderId}`)
      .then(res => setComments(res.data))
      .catch(() => setComments([]))
      .finally(() => setLoading(false));
  }, [orderId]);

  useEffect(() => {
    if (!highlightCommentId && !highlightReplyId) return;
    if (!comments.length) return;

    const targetId = highlightCommentId || highlightReplyId;
    if (!targetId) return;

    const isReply = !!highlightReplyId;
    const parentCommentId = isReply ? findParentCommentIdForReply(comments, highlightReplyId) : Number(highlightCommentId);
    if (isReply && !parentCommentId) return;

    const commentIdToExpand = isReply ? parentCommentId : Number(highlightCommentId);
    setExpandedReplies(prev => ({ ...prev, [commentIdToExpand]: true }));
  }, [highlightCommentId, highlightReplyId, comments]);

  useLayoutEffect(() => {
    if (!highlightCommentId && !highlightReplyId) return;

    const currentTarget = highlightCommentId || highlightReplyId;
    if (highlightAppliedRef.current === currentTarget) return;
    highlightAppliedRef.current = currentTarget;

    const tryScroll = (attempt = 1) => {
      const el = document.getElementById(`comment-${highlightCommentId}`) || document.getElementById(`reply-${highlightReplyId}`);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        el.style.transition = 'background-color 0.5s';
        el.style.backgroundColor = 'rgba(25, 118, 210, 0.15)';
        setTimeout(() => {
          el.style.backgroundColor = '';
        }, 2000);
        return;
      }
      if (attempt < 20) {
        setTimeout(() => tryScroll(attempt + 1), 150);
      }
    };

    const timer = setTimeout(() => tryScroll(), 300);
    return () => clearTimeout(timer);
  }, [highlightCommentId, highlightReplyId, comments, expandedReplies]);

  const findReplyById = (replies, replyId) => {
    const targetId = Number(replyId);
    for (const reply of replies || []) {
      if (Number(reply.id) === targetId) {
        return reply;
      }
      const found = findReplyById(reply.replies, targetId);
      if (found) return found;
    }
    return null;
  };

  const findParentCommentIdForReply = (comments, replyId) => {
    const targetId = Number(replyId);
    for (const comment of comments) {
      if (comment.replies?.some(r => Number(r.id) === targetId)) {
        return Number(comment.id);
      }
      const found = findReplyById(comment.replies, targetId);
      if (found) {
        return Number(comment.id);
      }
    }
    return null;
  };

  const fetchComments = async () => {
    const res = await api.get(`/api/v1/comments/order/${orderId}`);
    setComments(res.data);
  };

  const uploadImages = async (files) => {
    if (!files || files.length === 0) return [];
    const formData = new FormData();
    Array.from(files).forEach(file => formData.append('files', file));
    const response = await api.post('/api/v1/images/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data || [];
  };

  const handleImageUpload = async (file, isReply = false, parentId = null) => {
    if (!file) return;
    try {
      const uploaded = await uploadImages([file]);
      if (!uploaded.length) return;
      if (isReply) {
        const key = parentId ? `reply-${parentId}` : parentId;
        setReplyImagesMap(prev => ({
          ...prev,
          [key]: [...(prev[key] || []), ...uploaded],
        }));
      } else {
        setCommentImages(prev => [...prev, ...uploaded]);
      }
    } catch (e) {
      console.error('Failed to upload image', e);
    }
  };

  const handlePaste = async (e, isReply = false, parentId = null) => {
    const items = e.clipboardData?.items;
    if (!items) return;
    const imageFiles = [];
    for (const item of items) {
      if (item.type.startsWith('image/')) {
        const file = item.getAsFile();
        if (file) imageFiles.push(file);
      }
    }
    if (imageFiles.length === 0) return;
    try {
      const uploaded = await uploadImages(imageFiles);
      if (!uploaded.length) return;
      if (isReply) {
        const key = parentId ? `reply-${parentId}` : parentId;
        setReplyImagesMap(prev => ({
          ...prev,
          [key]: [...(prev[key] || []), ...uploaded],
        }));
      } else {
        setCommentImages(prev => [...prev, ...uploaded]);
      }
    } catch (e) {
      console.error('Failed to upload pasted images', e);
    }
  };

  const handleAddComment = async () => {
    if (!newComment.trim() && !commentImages.length) return;
    const imageIds = commentImages.map(img => img.id);
    await api.post(`/api/v1/comments/order/${orderId}`, {
      body: newComment,
      imageIds,
    });
    setNewComment('');
    setCommentImages([]);
    setShowCommentForm(false);
    await fetchComments();
  };

  const handleAddReply = async (parentId, parentType = 'comment') => {
    const key = parentType === 'comment' ? parentId : `reply-${parentId}`;
    const text = replyTexts[key];
    const images = replyImagesMap[key] || [];
    if (!text?.trim() && !images.length) return;

    const payload = {
      parentCommentId: parentId,
      body: text,
      imageIds: images.map(img => img.id),
    };

    if (parentType === 'reply') {
      payload.parentReplyId = parentId;
    }

    await api.post(`/api/v1/comment-replies`, payload);
    setReplyTexts(prev => ({ ...prev, [key]: '' }));
    setReplyImagesMap(prev => ({ ...prev, [key]: [] }));
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
          {reply.images?.length > 0 && (
            <Box mb={1} display="flex" gap={1} flexWrap="wrap">
              {reply.images.map(img => (
                <img key={img.id} src={getImageUrl(img.url)} alt={img.originalName} style={{ maxWidth: 200, maxHeight: 200, borderRadius: 4 }} />
              ))}
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
                multiple
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
          {(replyImagesMap[replyKey] || []).length > 0 && (
            <Box sx={{ mt: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              {(replyImagesMap[replyKey] || []).map(img => (
                <Box key={img.id} sx={{ position: 'relative', display: 'inline-block' }}>
                  <img src={getImageUrl(img.url)} alt={img.originalName} style={{ maxWidth: 100, maxHeight: 100, borderRadius: 4 }} />
                  <IconButton
                    size="small"
                    onClick={() => setReplyImagesMap(prev => ({ ...prev, [replyKey]: prev[replyKey].filter(i => i.id !== img.id) }))}
                    sx={{ position: 'absolute', top: -8, right: -8, bgcolor: 'background.paper' }}
                  >
                    <Close fontSize="small" />
                  </IconButton>
                </Box>
              ))}
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
            {comment.images?.length > 0 && (
              <Box mb={1} display="flex" gap={1} flexWrap="wrap">
                {comment.images.map(img => (
                  <img key={img.id} src={getImageUrl(img.url)} alt={img.originalName} style={{ maxWidth: 200, maxHeight: 200, borderRadius: 4 }} />
                ))}
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
                  multiple
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
            {(replyImagesMap[comment.id] || []).length > 0 && (
              <Box sx={{ mt: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                {(replyImagesMap[comment.id] || []).map(img => (
                  <Box key={img.id} sx={{ position: 'relative', display: 'inline-block' }}>
                    <img src={getImageUrl(img.url)} alt={img.originalName} style={{ maxWidth: 100, maxHeight: 100, borderRadius: 4 }} />
                    <IconButton
                      size="small"
                      onClick={() => setReplyImagesMap(prev => ({ ...prev, [comment.id]: prev[comment.id].filter(i => i.id !== img.id) }))}
                      sx={{ position: 'absolute', top: -8, right: -8, bgcolor: 'background.paper' }}
                    >
                      <Close fontSize="small" />
                    </IconButton>
                  </Box>
                ))}
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
            multiple
            style={{ display: 'none' }}
            ref={fileInputRef}
            onChange={(e) => handleImageUpload(e.target.files, false)}
          />
          {commentImages.length > 0 && (
            <Box sx={{ mt: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              {commentImages.map(img => (
                <Box key={img.id} sx={{ position: 'relative', display: 'inline-block' }}>
                  <img src={getImageUrl(img.url)} alt={img.originalName} style={{ maxWidth: 100, maxHeight: 100, borderRadius: 4 }} />
                  <IconButton
                    size="small"
                    onClick={() => setCommentImages(prev => prev.filter(i => i.id !== img.id))}
                    sx={{ position: 'absolute', top: -8, right: -8, bgcolor: 'background.paper' }}
                  >
                    <Close fontSize="small" />
                  </IconButton>
                </Box>
              ))}
            </Box>
          )}
          <Box sx={{ mt: 1, display: 'flex', gap: 1, justifyContent: 'space-between', alignItems: 'center' }}>
            <Box>
              <input
                type="file"
                accept="image/*"
                multiple
                style={{ display: 'none' }}
                id="comment-image-upload"
                onChange={(e) => handleImageUpload(e.target.files, false)}
              />
              <label htmlFor="comment-image-upload">
                <Button size="small" component="span" variant="outlined" startIcon={<ImageIcon />}>
                  Изображения
                </Button>
              </label>
            </Box>
            <Box>
              {showCommentForm && (
                <Button onClick={() => { setShowCommentForm(false); setNewComment(''); setCommentImages([]); }} sx={{ mr: 1 }}>
                  Отмена
                </Button>
              )}
              <Button variant="contained" onClick={handleAddComment} disabled={!newComment.trim() && !commentImages.length}>
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
