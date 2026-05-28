/**
 * Калькуляция заказов на фронтенде.
 * Дублирует логику из CalculationService.java для мгновенного расчёта.
 * Валидируется на бэкенде с погрешностью 0.01.
 */
import { isM2, isLinearMeter } from '../utils/orderUtils';

const round2 = (value) => Number(value.toFixed(2));

const perimeter = (widthM, heightM) => (widthM + (heightM || 0)) * 2;

export function calculateMaterialArea(widthM, heightM, podvorotMmHorizontal, podvorotMmVertical, podvorotCountPerSide = 2) {
  let effectiveWidth = widthM;
  let effectiveHeight = heightM;

  if (podvorotMmHorizontal != null && podvorotMmHorizontal > 0) {
    const extraW = (podvorotMmHorizontal / 1000) * (podvorotCountPerSide * 2);
    effectiveWidth = widthM + extraW;
  }

  if (podvorotMmVertical != null && podvorotMmVertical > 0) {
    const extraH = (podvorotMmVertical / 1000) * (podvorotCountPerSide * 2);
    effectiveHeight = heightM + extraH;
  }

  return effectiveWidth * effectiveHeight;
}

export function calculateEyeletsQuantity(widthM, heightM, eyeletStepCm = 40) {
  const perimeterCm = perimeter(widthM, heightM) * 100;
  const step = eyeletStepCm || 40;
  return Math.ceil(perimeterCm / step);
}

export function calculateOperationQuantity(op, widthM, heightM, eyeletStepCm = null) {
  if (!op) return 0;

  const unit = op.unit || op.unitType;
  const name = (op.name || '').toLowerCase();
  const hemWidthMm = op.hemWidthMm;
  const hemCount = op.hemCount;

  if (hemWidthMm != null && hemCount != null) {
    const widthMm = widthM * 1000;
    const heightMm = (heightM || 0) * 1000;
    const added = hemWidthMm * hemCount;
    const newWidth = widthMm + added;
    const newHeight = heightMm + added;
    return (newWidth * newHeight) / 1_000_000;
  }

  const unitStr = String(unit || '').toLowerCase().replace(/\s/g, '').replace(/[.^]/g, '');

  if (unitStr === 'м2' || unitStr === 'm2' || unitStr.includes('кв')) {
    return widthM * (heightM != null ? heightM : 1);
  }

  if (unitStr === 'пм' || unitStr === 'мп' || unitStr.includes('пог')) {
    return perimeter(widthM, heightM);
  }

  if (unitStr === 'шт' || name.includes('люверс') || name.includes('eyelet')) {
    const isEyeletOp = name.includes('люверс') || name.includes('eyelet') || name.includes('установка');
    if (isEyeletOp) {
      return calculateEyeletsQuantity(widthM, heightM, eyeletStepCm ?? op.eyeletStepCm ?? 40);
    }
    return 1;
  }

  return 1;
}

export function calculateMaterialCost(material, widthM, heightM, podvorotMmHorizontal, podvorotMmVertical, podvorotCountPerSide) {
  if (!material || !widthM) return 0;

  const price = Number(material.price || material.pricePerSquareMeter || 0);
  const waste = Number(material.wasteCoefficient || 1);

  if (isLinearMeter(material)) {
    return round2(widthM * price * waste);
  }

  if (isM2(material)) {
    const h = heightM != null ? heightM : 1;
    if (podvorotMmHorizontal != null || podvorotMmVertical != null) {
      const area = calculateMaterialArea(widthM, h, podvorotMmHorizontal, podvorotMmVertical, podvorotCountPerSide);
      return round2(area * price * waste);
    }
    return round2(widthM * h * price * waste);
  }

  return round2(widthM * price * waste);
}

export function calculateEyeletHardwareCost(eyeletId, eyeletsCount, eyeletsData) {
  if (!eyeletId || !eyeletsData) {
    console.log('[DEBUG] eyeletId or eyeletsData missing:', {eyeletId, eyeletsDataLength: eyeletsData ? eyeletsData.length : 0});
    return 0;
  }
  const eyelet = eyeletsData.find(e => e.id == eyeletId || e.eyeletId == eyeletId);
  if (!eyelet) {
    console.log('[DEBUG] eyelet not found:', {eyeletId, eyeletsData});
    return 0;
  }
  const price = Number(eyelet.pricePerPiece || eyelet.price || 0);
  console.log('[DEBUG] found eyelet:', {eyelet, price, eyeletsCount});
  return round2(price * eyeletsCount);
}

export function calculateItemCost(item, material, eyeletsData = null) {
  const widthM = Number(item.widthM ?? item.qty1value ?? 0);
  const heightM = Number(item.heightM ?? item.qty2value ?? 0);

  const podvorotMmHorizontal = item.podvorotMmHorizontal != null ? Number(item.podvorotMmHorizontal) : null;
  const podvorotMmVertical = item.podvorotMmVertical != null ? Number(item.podvorotMmVertical) : null;
  const podvorotCountPerSide = item.podvorotCountPerSide || 2;

  const eyeletOp = item.operations?.find(op => op.eyeletId != null);
  const eyeletId = item.eyeletId ?? eyeletOp?.eyeletId ?? null;
  const eyeletStepCm = item.eyeletStepCm ?? eyeletOp?.eyeletStepCm ?? 40;

  const materialCost = calculateMaterialCost(material, widthM, heightM, podvorotMmHorizontal, podvorotMmVertical, podvorotCountPerSide);

  const operationsCost = (item.operations || []).reduce((sum, op) => {
    if (op.subtotal != null) {
      return sum + Number(op.subtotal);
    }
    const price = Number(op.price || op.pricePerUnit || 0);
    if (!price) return sum;
    const qty = calculateOperationQuantity(op, widthM, heightM, eyeletStepCm);
    return sum + round2(qty * price);
  }, 0);

  let eyeletCost = Number(item.eyeletCost || 0);
  // Считаем стоимость люверсов только если eyeletId передан и eyeletsData доступен
  // и eyeletCost ещё не рассчитан (операции люверс сами по себе не включают стоимость фурнитуры)
  if (eyeletId != null && eyeletsData && eyeletCost === 0) {
    const qty = calculateEyeletsQuantity(widthM, heightM, eyeletStepCm);
    eyeletCost = calculateEyeletHardwareCost(eyeletId, qty, eyeletsData);
    console.log('[DEBUG calculateItemCost] eyeletCost after calc:', eyeletCost, 'eyeletId:', eyeletId, 'eyeletsData length:', eyeletsData.length);
  } else {
    console.log('[DEBUG calculateItemCost] eyeletCost skipped:', {eyeletId, eyeletsDataExists: !!eyeletsData, eyeletCost});
  }

  const total = materialCost + operationsCost + eyeletCost;

  return {
    materialCost: Number(materialCost.toFixed(2)),
    operationsCost: Number(operationsCost.toFixed(2)),
    eyeletCost: Number(eyeletCost.toFixed(2)),
    totalWithoutPriceplus: Number(total.toFixed(2)),
  };
}

export function recalculateOrderFull(order, priceplusPercent = 0, eyeletsData = null) {
  const items = order.materials || order.items || [];
  if (!items.length) {
    return { totalWithoutPriceplus: 0, totalWithPriceplus: 0, materials: [] };
  }

  let totalWithout = 0;
  const updated = items.map((it) => {
    const material = it.material || it;
    const calc = calculateItemCost(it, material, eyeletsData);
    totalWithout += calc.totalWithoutPriceplus;
    return { ...it, ...calc };
  });

  const totalWith = totalWithout * (1 + (Number(priceplusPercent) || 0) / 100);

  return {
    totalWithoutPriceplus: round2(totalWithout),
    totalWithPriceplus: round2(totalWith),
    materials: updated,
  };
}

export function applyPriceplus(total, priceplusPercent) {
  return total * (1 + (Number(priceplusPercent) || 0) / 100);
}

export function calculateItemCostBase(item, material) {
  return calculateItemCost(item, material);
}

export function calculateItemCostFull(item, material, eyeletsData = null) {
  return calculateItemCost(item, material, eyeletsData);
}

export function recalculateOrderLocally(order, priceplusPercent = 0) {
  return recalculateOrderFull(order, priceplusPercent);
}

export default {
  calculateMaterialCost,
  calculateItemCost,
  calculateItemCostFull,
  calculateItemCostBase,
  recalculateOrderLocally,
  recalculateOrderFull,
  calculateOperationQuantity,
  calculateMaterialArea,
  calculateEyeletsQuantity,
  calculateEyeletHardwareCost,
  applyPriceplus,
};