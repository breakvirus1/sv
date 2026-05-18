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
