/**
 * Минимальный клиентский сервис калькуляции заказов.
 * Используется для мгновенного пересчёта при изменении priceplus и размеров материалов
 * без постоянных запросов к бэкенду.
 */

import { isM2, isLinearMeter } from '../utils/orderUtils';

/**
 * Рассчитывает стоимость одного материала по позиции.
 */
export function calculateMaterialCost(material, widthM, heightM) {
  if (!material || !widthM) return 0;

  const price = Number(material.price || material.pricePerSquareMeter || 0);
  const waste = Number(material.wasteCoefficient || 1);

  if (isLinearMeter(material)) {
    // Линейные материалы (м.п.) — только ширина
    return widthM * price * waste;
  }

  if (isM2(material)) {
    const h = heightM != null ? heightM : 0;
    return widthM * h * price * waste;
  }

  // Штучные и прочие — считаем как линейные по умолчанию
  return widthM * price * waste;
}

/**
 * Пересчитывает стоимость одной позиции заказа (материал + операции).
 * Операции берутся с уже рассчитанными subtotal (если есть).
 */
export function calculateItemCost(item, material) {
  const widthM = Number(item.widthM ?? item.qty1value ?? 0);
  const heightM = Number(item.heightM ?? item.qty2value ?? 0);

  const materialCost = calculateMaterialCost(material, widthM, heightM);

  const operationsCost = (item.operations || []).reduce((sum, op) => {
    return sum + (Number(op.subtotal) || 0);
  }, 0);

  const eyeletCost = Number(item.eyeletCost || 0);

  return {
    materialCost: Number(materialCost.toFixed(2)),
    operationsCost: Number(operationsCost.toFixed(2)),
    eyeletCost: Number(eyeletCost.toFixed(2)),
    totalWithoutPriceplus: Number((materialCost + operationsCost + eyeletCost).toFixed(2)),
  };
}

/**
 * Пересчитывает весь заказ локально (для live preview).
 * Не пересчитывает сложные вещи (подвороты, точные количества операций, люверсы) —
 * это остаётся на бэкенде.
 */
export function recalculateOrderLocally(order, priceplusPercent = 0) {
  if (!order || !order.materials) {
    return {
      totalWithoutPriceplus: 0,
      totalWithPriceplus: 0,
      materials: [],
    };
  }

  let totalWithout = 0;

  const updatedMaterials = order.materials.map((matItem) => {
    const material = matItem.material || matItem;

    const widthM = Number(matItem.widthM ?? matItem.qty1value ?? 0);
    const heightM = Number(matItem.heightM ?? matItem.qty2value ?? 0);

    const matCost = calculateMaterialCost(material, widthM, heightM);
    const opsCost = (matItem.operations || []).reduce((s, o) => s + (Number(o.subtotal) || 0), 0);
    const eyeCost = Number(matItem.eyeletCost || 0);

    const itemTotal = matCost + opsCost + eyeCost;
    totalWithout += itemTotal;

    return {
      ...matItem,
      cost: Number(matCost.toFixed(2)),
      operationsCost: Number(opsCost.toFixed(2)),
      eyeletCost: Number(eyeCost.toFixed(2)),
      totalWithoutPriceplus: Number(itemTotal.toFixed(2)),
    };
  });

  const totalWith = totalWithout * (1 + (Number(priceplusPercent) || 0) / 100);

  return {
    totalWithoutPriceplus: Number(totalWithout.toFixed(2)),
    totalWithPriceplus: Number(totalWith.toFixed(2)),
    materials: updatedMaterials,
  };
}

/**
 * Применяет priceplus к уже рассчитанной сумме.
 */
export function applyPriceplus(total, priceplusPercent) {
  return total * (1 + (Number(priceplusPercent) || 0) / 100);
}

/**
 * Вычисляет количество для операции на основе типа и параметров (локальная версия для live preview).
 * Дублирует упрощённую логику из CalculationService.java для мгновенного отклика.
 * Для сложных случаев (точные подвороты) будет небольшая погрешность, валидируемая на бэкенде.
 */
export function calculateOperationQuantity(op, widthM, heightM, eyeletStepCm = 40) {
  if (!op) return 0;
  const name = (op.name || '').toLowerCase();
  const unit = (op.unit || '').toLowerCase().replace(/\s/g, '');

  // Специальная логика для подворота (hem)
  if (op.hemWidthMm != null && op.hemCount != null && op.hemWidthMm > 0) {
    const added = op.hemWidthMm * (op.hemCount || 2);
    const newW = (Number(widthM) * 1000) + added;
    const newH = (Number(heightM || 0) * 1000) + added;
    return (newW * newH) / 1_000_000; // м²
  }

  if (unit.includes('м2') || unit.includes('m2') || unit.includes('кв') || unit === 'm²') {
    return Number(widthM) * (Number(heightM) || 1);
  }
  if (unit.includes('м') || unit.includes('пог') || unit.includes('linear') || unit.includes('п.м')) {
    // периметр
    return (Number(widthM) + (Number(heightM) || 0)) * 2;
  }
  if (unit.includes('шт') || name.includes('люверс') || name.includes('eyelet')) {
    if (name.includes('люверс') || name.includes('eyelet')) {
      const perimCm = (Number(widthM) + (Number(heightM) || 0)) * 2 * 100;
      const step = eyeletStepCm || op.eyeletStepCm || 40;
      return Math.ceil(perimCm / step);
    }
    return 1;
  }
  return 1;
}

/**
 * Полный пересчёт стоимости позиции с вычислением qty операций локально.
 * Используется для real-time в Create/Edit без лишних запросов.
 */
export function calculateItemCostFull(item, material) {
  const widthM = Number(item.widthM ?? item.qty1value ?? 0);
  const heightM = Number(item.heightM ?? item.qty2value ?? 0);

  const materialCost = calculateMaterialCost(material, widthM, heightM);

  const operationsCost = (item.operations || []).reduce((sum, op) => {
    if (op.subtotal != null) {
      return sum + Number(op.subtotal);
    }
    const price = Number(op.price || op.pricePerUnit || 0);
    if (!price) return sum;
    const qty = calculateOperationQuantity(op, widthM, heightM);
    return sum + (qty * price);
  }, 0);

  const eyeletCost = Number(item.eyeletCost || 0);

  const totalWithout = materialCost + operationsCost + eyeletCost;

  return {
    materialCost: Number(materialCost.toFixed(2)),
    operationsCost: Number(operationsCost.toFixed(2)),
    eyeletCost: Number(eyeletCost.toFixed(2)),
    totalWithoutPriceplus: Number(totalWithout.toFixed(2)),
  };
}

/**
 * Полный пересчёт всего заказа с использованием полной калькуляции позиций (локально).
 * Для real-time preview в CreateOrder / EditOrder.
 */
export function recalculateOrderFull(order, priceplusPercent = 0) {
  if (!order || !order.materials && !order.items) {
    return { totalWithoutPriceplus: 0, totalWithPriceplus: 0, materials: [] };
  }

  const items = order.materials || order.items || [];
  let totalWithout = 0;

  const updated = items.map((it) => {
    const material = it.material || it;
    const calc = calculateItemCostFull(it, material);
    totalWithout += calc.totalWithoutPriceplus;
    return {
      ...it,
      ...calc,
    };
  });

  const totalWith = totalWithout * (1 + (Number(priceplusPercent) || 0) / 100);

  return {
    totalWithoutPriceplus: Number(totalWithout.toFixed(2)),
    totalWithPriceplus: Number(totalWith.toFixed(2)),
    materials: updated,
  };
}

export default {
  calculateMaterialCost,
  calculateItemCost,
  recalculateOrderLocally,
  recalculateOrderFull,
  calculateItemCostFull,
  calculateOperationQuantity,
  applyPriceplus,
};
