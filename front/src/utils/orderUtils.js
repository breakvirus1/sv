/**
 * Utilities for orders: status, labels and unit handling.
 */

export const getStatusColor = (status) => {
  const colors = {
    DRAFT: 'default',
    APPROVAL: 'warning',
    IN_PROGRESS: 'primary',
    READY: 'success'
  };
  return colors[status] || 'default';
};

export const getStatusLabel = (status) => {
  const labels = {
    DRAFT: 'Черновик',
    APPROVAL: 'Согласование',
    IN_PROGRESS: 'В работе',
    READY: 'Готов'
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