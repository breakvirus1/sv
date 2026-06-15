/**
 * Service for order calculations.
 * All calculations are done on the backend via API calls.
 */
import api from './api';

const toMeters = (value, unit) => {
  const v = parseFloat(value) || 0;
  return unit === 'мм' ? v / 1000 : v;
};

const calculateItemCostBackend = async (materialId, widthM, heightM, operations = [], eyeletId = null, eyeletStepCm = 40, podvorotMmHorizontal = null, podvorotMmVertical = null, podvorotCountPerSide = 2) => {
  if (!materialId) {
    throw new Error('materialId is required');
  }
  if (!widthM || widthM <= 0) {
    throw new Error('widthM must be greater than 0');
  }
  const payload = {
    materialId,
    widthM,
    heightM,
    operationIds: operations.map(op => op.id || op.operationId).filter(id => id != null),
    eyeletId,
    eyeletStepCm,
    podvorotMmHorizontal,
    podvorotMmVertical,
    podvorotCountPerSide
  };

  const response = await api.post('/api/v1/calculations/preview', payload);
  return response.data;
};

const recalculateOrderBackend = async (items, priceplusPercent = 0) => {
  const promises = items.map(async (item) => {
    const materialId = item.material?.id || item.materialId;
    if (!materialId) {
      throw new Error('materialId is required for calculation');
    }
    const unit = item.unit || 'м';
    const widthM = toMeters(item.widthM ?? item.qty1value ?? 0, unit);
    const heightM = toMeters(item.heightM ?? item.qty2value ?? 0, unit);
    
    const operations = (item.operations || []).map(op => ({
      operationId: op.id || op.operationId,
      widthMm: op.widthMm,
      heightMm: op.heightMm
    }));
    
    const eyeletOp = (item.operations || []).find(op => op.eyeletId);
    const eyeletId = item.eyeletId ?? eyeletOp?.eyeletId ?? null;
    const eyeletStepCm = item.eyeletStepCm ?? eyeletOp?.eyeletStepCm ?? 40;
    
    const podvorotOp = (item.operations || []).find(op => op.hemWidthMm != null);
    const podvorotMmHorizontal = podvorotOp?.hemWidthMm ?? null;
    const podvorotMmVertical = podvorotOp?.hemWidthMm ?? null;
    const podvorotCountPerSide = podvorotOp?.hemCount ?? 2;
    
    const result = await calculateItemCostBackend(
      materialId,
      widthM,
      heightM,
      operations,
      eyeletId,
      eyeletStepCm,
      podvorotMmHorizontal,
      podvorotMmVertical,
      podvorotCountPerSide
    );
    
    return {
      ...item,
      ...result,
      widthM,
      heightM
    };
  });
  
  const results = await Promise.all(promises);
  const totalWithout = results.reduce((sum, item) => sum + (item.totalPrice || 0), 0);
  const totalWith = totalWithout * (1 + (Number(priceplusPercent) || 0) / 100);
  
  return {
    totalWithoutPriceplus: Number(totalWithout.toFixed(2)),
    totalWithPriceplus: Number(totalWith.toFixed(2)),
    materials: results
  };
};

const applyPriceplus = (total, priceplusPercent) => {
  return total * (1 + (Number(priceplusPercent) || 0) / 100);
};

export {
  calculateItemCostBackend as calculateItemCost,
  recalculateOrderBackend as recalculateOrderFull,
  recalculateOrderBackend as recalculateOrderLocally,
  recalculateOrderBackend,
  applyPriceplus
};

export default {
  calculateItemCost: calculateItemCostBackend,
  recalculateOrderFull: recalculateOrderBackend,
  recalculateOrderLocally: recalculateOrderBackend,
  applyPriceplus
};