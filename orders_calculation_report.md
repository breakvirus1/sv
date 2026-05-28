# Анализ алгоритмов калькуляции заказов

## Обзор

В бэкенде калькуляция заказов реализована в двух сервисах:
1. **calculator-service** — вычисление себестоимости
2. **order-service** — расчет финальной цены с наценкой (priceplus)

---

## 1. Calculator Service (`CalculationService.java`)

### 1.1 Расчет площади материала (с подворотами)

**Файл:** `calculator-service/src/main/java/.../CalculationService.java:220-244`

```java
private BigDecimal calculateMaterialArea(Calculation c) {
    BigDecimal width = c.getWidthM();
    BigDecimal height = c.getHeightM();

    if (c.hasPodvorot()) {
        BigDecimal extraW = podvorotMmHorizontal / 1000 * (podvorotCountPerSide * 2);
        BigDecimal extraH = podvorotMmVertical / 1000 * (podvorotCountPerSide * 2);
        width = width.add(extraW);
        height = height.add(extraH);
    }

    return width.multiply(height);
}
```

**Формула:**
- Без подворотов: `area = widthM × heightM`
- С подворотами: `area = (widthM + extraW) × (heightM + extraH)`
- Где `extraW/H = (podvorotMm / 1000) × (podvorotCountPerSide × 2)`

---

### 1.2 Расчет стоимости материала

**Файл:** `calculator-service/src/main/java/.../CalculationService.java:186-218`

```java
BigDecimal materialArea = calculateMaterialArea(calc);
BigDecimal materialPrice = calc.getMaterial().getPricePerSquareMeter();
BigDecimal wasteCoeff = calc.getMaterial().getWasteCoefficient();
BigDecimal materialCost = materialArea.multiply(materialPrice).multiply(wasteCoeff);
```

**Формула:** `materialCost = area × pricePerM2 × wasteCoefficient`

---

### 1.3 Расчет количества для операций

**Файл:** `calculator-service/src/main/java/.../CalculationService.java:246-282`

| Единица измерения | Формула |
|-------------------|---------|
| `SQUARE_METER` | `widthM × heightM` |
| `LINEAR_METER` | `(widthM + heightM) × 2` (периметр) |
| `PIECE` | `calculateEyeletsQuantity()` для люверсов, иначе `1` |

**Особый случай для "подворот":**
```java
widthMm = widthM × 1000 + hemWidthMm × hemCount
heightMm = heightM × 1000 + hemWidthMm × hemCount
quantity = (widthMm × heightMm) / 1_000_000
```

---

### 1.4 Расчет люверсов

**Файл:** `calculator-service/src/main/java/.../CalculationService.java:288-294`

```java
BigDecimal perimeterM = (widthM + heightM) * 2;
BigDecimal perimeterCm = perimeterM * 100;
BigDecimal eyeletQty = perimeterCm / eyeletStepCm (округление вверх);
```

---

### 1.5 Итоговый расчет

```java
total = materialCost + Σ(operationsCost) + eyeletCost
```

---

## 2. Order Service (`OrderService.java`)

### 2.1 Расчет позиции заказа

**Файл:** `order-service/src/main/java/.../OrderService.java:314-321`

```java
BigDecimal materialArea = widthM.multiply(heightM);
BigDecimal materialCost = materialArea.multiply(materialPrice).multiply(wasteCoeff);
BigDecimal effectiveArea = materialArea;

// eyeletCost вычисляется как разница:
BigDecimal eyeletCost = totalPrice - operationsSubtotal - materialCost;
```

### 2.2 Priceplus (наценка)

**Файл:** `order-service/src/main/java/.../OrderService.java:324-325`

```java
BigDecimal costPriceplus = totalPrice × (1 + priceplus/100);
```

---

## 3. Фронтенд (обновлен)

Все расчеты перенесены на бэкенд. Фронтенд теперь отправляет запросы в calculator-service для получения актуальных цен.

**Файл:** `front/src/services/calculationService.js`

```javascript
const calculateItemCostBackend = async (...) => {
  const response = await api.post('/api/v1/calculations/preview', payload);
  return response.data;
};
```

---

## Сводная таблица формул

| Показатель | Формула | Примечание |
|------------|---------|------------|
| Площадь материала | `widthM × heightM` | Без подворотов |
| Площадь с подворотами | `(w + Δw) × (h + Δh)` | Δw/h = (mm/1000) × count × 2 |
| Стоимость материала | `area × price × waste` | waste = 1.10 (10% отход) |
| Периметр | `(widthM + heightM) × 2` | Для линейных операций |
| Люверсы | `perimeterCm / step` | Округление вверх |
| Итого | `material + operations + eyelet` | |
| С наценкой | `total × (1 + priceplus/100)` | |
---

## 7. Конвертация единиц измерения (Frontend)

**Файл:** `front/src/services/calculationService.js`

При передаче размеров в бэкенд выполняется конвертация:

```javascript
const toMeters = (value, unit) => {
  const v = parseFloat(value) || 0;
  return unit === 'мм' ? v / 1000 : v;
};

const widthM = toMeters(item.widthM ?? item.qty1value ?? 0, unit);
const heightM = toMeters(item.heightM ?? item.qty2value ?? 0, unit);
```

**Поддерживаемые единицы:**
- `мм` (миллиметры) → деление на 1000
- `м` (метры) → без конвертации
- По умолчанию `м` если единица не указана
