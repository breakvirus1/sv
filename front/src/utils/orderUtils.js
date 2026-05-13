export const getStatusColor = (status) => {
  const colors = {
    WAITING: 'warning',
    LAUNCHED: 'info',
    IN_PROGRESS: 'primary',
    READY: 'success',
    ACCEPTED: 'success',
    CLOSED: 'default'
  };
  return colors[status] || 'default';
};
