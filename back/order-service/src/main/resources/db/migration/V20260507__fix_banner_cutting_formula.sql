-- =============================================
-- Исправление формулы раскроя баннера: удаление множителя coefficient
-- Дата: 2026-05-07
-- Описание: Формула для операции раскроя должна быть без коэффициента.
-- Было: (width + marginWidth*2) * (height + marginHeight*2) / 1_000_000 * coefficient
-- Стало: (width + marginWidth*2) * (height + marginHeight*2) / 1_000_000
-- =============================================

-- Обновляем quantity_formula в product_operations, удаляя умножение на coefficient
UPDATE ordschema.product_operations
SET quantity_formula = regexp_replace(quantity_formula, '\s*\*\s*coefficient', '', 'g')
WHERE quantity_formula ~ '\*';

-- Если в формуле были пробелы и коэффициент в конце, убираем возможные лишние пробелы в конце
UPDATE ordschema.product_operations
SET quantity_formula = trim(quantity_formula)
WHERE quantity_formula LIKE '%coefficient%';

-- Также на всякий случай: если формула содержит параметр coefficient в любом виде, удаляем его (на случай если он был не только как множитель)
-- Это безопасно, так как coefficient использовался только как множитель в конце формулы
UPDATE ordschema.product_operations
SET quantity_formula = regexp_replace(quantity_formula, '\s*\+\s*coefficient|\s*\-\s*coefficient|\s*\*\s*coefficient|\s*\/\s*coefficient', '', 'g')
WHERE quantity_formula ~ 'coefficient';
