/**
 * Utilities for orders: status, labels and unit handling.
 */

export const getStatusColor = (status) => {
  const colors = {
    DRAFT: 'default',
    IN_PROGRESS: 'primary',
    READY: 'success',
    CLOSED: 'error',
    NOT_STARTED: 'warning'
  };
  return colors[status] || 'default';
};

export const getStatusLabel = (status) => {
  const labels = {
    DRAFT: 'Черновик',
    IN_PROGRESS: 'В работе',
    READY: 'Готов',
    CLOSED: 'Закрыт',
    NOT_STARTED: 'Не запущен'
  };
  return labels[status] || status;
};

export const isM2 = (mat) => {
  if (!mat || !mat.unit) return false;
  const u = mat.unit.trim().toLowerCase().replace(/[.]/g, '').replace(/\s/g, '');
  return u === 'м2' || u === 'm2' || u === 'м²' || u === 'm²';
};

export const isLinearMeter = (mat) => {
  if (!mat || !mat.unit) return false;
  const u = mat.unit.trim().toLowerCase().replace(/\s/g, '');
  return u === 'м.п.' || u === 'м.п' || u === 'п.м.' || u === 'п.м' || u === 'мп' || u === 'пм';
};

export const isPiece = (mat) => {
  if (!mat || !mat.unit) return false;
  const u = mat.unit.trim().toLowerCase();
  return u === 'шт' || u === 'шт.';
};