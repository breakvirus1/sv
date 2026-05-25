/**
 * Утилиты для работы с заказами: статус, метки и обработка единиц.
 */

// Сопоставление цветов статуса для компонента Chip Material-UI
export const getStatusColor = (status) => {
  const colors = {
    DRAFT: 'default',
    APPROVAL: 'warning',
    IN_PROGRESS: 'primary',
    READY: 'success'
  };
  return colors[status] || 'default';
};

// Человекочитаемые метки статусов на русском языке
export const getStatusLabel = (status) => {
  const labels = {
    DRAFT: 'Черновик',
    APPROVAL: 'Согласование',
    IN_PROGRESS: 'В работе',
    READY: 'Готов'
  };
  return labels[status] || status;
};

// Проверка, является ли единица измерения материала квадратными метрами (м2)
export const isM2 = (mat) => {
  if (!mat || !mat.unit) return false;
  const u = mat.unit.trim().toLowerCase().replace(/[.]/g, '').replace(/\s/g, '');
  return u === 'м2' || u === 'm2' || u === 'м²' || u === 'm²';
};

// Проверка, является ли единица измерения материала линейными метрами (м.п.)
export const isLinearMeter = (mat) => {
  if (!mat || !mat.unit) return false;
  const u = mat.unit.trim().toLowerCase().replace(/\s/g, '');
  return u === 'м.п.' || u === 'м.п' || u === 'п.м.' || u === 'п.м' || u === 'мп' || u === 'пм';
};

// Проверка, является ли единица измерения материала штуками (шт)
export const isPiece = (mat) => {
  if (!mat || !mat.unit) return false;
  const u = mat.unit.trim().toLowerCase();
  return u === 'шт' || u === 'шт.';
};

// Определить, нужно ли показывать второе измерение (материалы на площадь)
export const showSecondDimension = (mat) => isM2(mat);

// Определить, нужно ли показывать только одно измерение (линейные/штучные материалы)
export const showSingleDimension = (mat) => isLinearMeter(mat) || isPiece(mat);