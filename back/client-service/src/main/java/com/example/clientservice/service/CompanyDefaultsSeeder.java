package com.example.clientservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Заполняет справочники новой компании стандартными материалами и операциями
 * (по прайс-листу reklama1.su, Севастополь). Каждая позиция получает company_id новой компании.
 */
@Service
@RequiredArgsConstructor
public class CompanyDefaultsSeeder {

    private static final List<Object[]> MATERIALS = List.of(
        row("Баннер ламинированный (от 320 г/м²)",                 "м2",   200.00),
        row("Баннер слабогорючий Г1 (литой)",                      "м2",  1000.00),
        row("Бумага BlueBack (для постеров)",                      "м2",   300.00),
        row("Бумага BackLit (для световых изображений)",           "м2",   400.00),
        row("Бумага City",                                         "м2",   500.00),
        row("Плёнка самоклеящаяся белая/прозрачная",               "м2",   550.00),
        row("Плёнка самоклеящаяся перфорированная One Way Vision", "м2",   700.00),
        row("Сетка баннерная",                                     "м2",   650.00),
        row("Плёнка 641 (белая, прозрачная)",                      "м2",   650.00),
        row("Плёнка 641 наклейки с порезкой",                      "м2",  1000.00),
        row("Плёнка 641 наклейки с ламинацией",                    "м2",  1500.00),
        row("Плёнка Oracal 820 пломбировочная",                    "м2",  4800.00),
        row("Фотообои лён",                                        "м2",  1700.00),
        row("Фотообои холст",                                      "м2",  1700.00),
        row("Фотообои штукатурка",                                 "м2",  1700.00),
        row("Холст натуральный (ширина 1,52 м)",                   "м2",  1500.00),
        row("Холст ПВХ",                                           "м2",  1200.00),
        row("Акрил прозрачный/молочный 3 мм",                      "м2",  3000.00),
        row("Акрил цветной/зеркальный 3 мм",                       "м2",  5000.00),
        row("ПВХ вспененный 3 мм",                                 "м2",  1200.00),
        row("Композит (алюминиевая панель) 3 мм",                  "м2",  3000.00),
        row("ПЭТ прозрачный 0,3 мм",                               "м2",   600.00),
        row("Фанера 4 мм",                                         "м2",  1500.00),
        row("Пластик полистирол (для гравировки) 1,5 мм",          "м2",  5000.00),
        row("Пенокартон белый 5 мм",                               "м2",  1200.00),
        row("Пенокартон белый 10 мм",                              "м2",  1800.00),
        row("Магнитный винил 0,75 мм",                             "м.п.", 1000.00),
        row("Скотч вспененный/баннерный",                          "м.п.",  100.00)
    );

    private static final List<Object[]> OPERATIONS = List.of(
        unitRow("Набор текста (макетирование)",                                    "PIECE",         300.00),
        unitRow("Разработка оригинал-макета",                                      "PIECE",         500.00),
        unitRow("Час работы с дизайнером",                                         "PIECE",        1000.00),
        unitRow("Печать и подбор цветопробы",                                      "PIECE",         700.00),
        unitRow("Демонтаж плёнки с поверхности",                                   "PIECE",         400.00),
        unitRow("Ламинирование поверхности",                                       "SQUARE_METER",  700.00),
        unitRow("Напольная ламинация",                                             "SQUARE_METER", 1500.00),
        unitRow("Поклейка плёнки заказчика на поверхность",                        "PIECE",         500.00),
        unitRow("Оклейка автомобиля плёнкой с печатью и ламинацией",               "SQUARE_METER",  500.00),
        unitRow("Печать на плёнке 720 dpi",                                        "SQUARE_METER",  550.00),
        unitRow("Печать на плёнке 1440 dpi",                                       "SQUARE_METER",  650.00),
        unitRow("Печать на плёнке 1440 dpi с подрезкой",                           "SQUARE_METER", 1000.00),
        unitRow("Печать на плёнке 1440 dpi с ламинацией",                          "SQUARE_METER", 1700.00),
        unitRow("Печать на сатине и C8500",                                        "SQUARE_METER", 1800.00),
        unitRow("Прямая УФ-печать на плёнке 1440 dpi",                             "SQUARE_METER", 1800.00),
        unitRow("Порезка на плоттере",                                             "PIECE",        1000.00),
        unitRow("Порезка на плоттере плёнки заказчика (до 80 мкм)",                "PIECE",         300.00),
        unitRow("Порезка на плоттере плёнки заказчика (свыше 80 мкм)",             "PIECE",         500.00),
        unitRow("Порезка с выборкой",                                              "PIECE",        1200.00),
        unitRow("Порезка с выборкой и поклейкой на монтажную плёнку",              "PIECE",        1600.00),
        unitRow("Порезка с выборкой, монтажной плёнкой и поклейкой на поверхность","PIECE",        2000.00),
        unitRow("Порезка на отдельные изделия",                                    "LINEAR_METER",   30.00),
        unitRow("Спайка баннера",                                                  "LINEAR_METER",   50.00),
        unitRow("Набивка люверсов",                                                "PIECE",          15.00),
        unitRow("Установка клапана",                                               "PIECE",          50.00),
        unitRow("Изготовление подрамника",                                         "SQUARE_METER",  900.00),
        unitRow("Натяжка холста на подрамник",                                     "SQUARE_METER",  600.00),
        unitRow("Монтаж баннера на раму заказчика",                                "SQUARE_METER",  400.00),
        unitRow("Монтаж баннера на стену без рамы",                                "SQUARE_METER",  500.00),
        unitRow("Изготовление сварной рамы",                                       "LINEAR_METER",  500.00),
        unitRow("Монтаж баннера с изготовленной рамой",                            "SQUARE_METER",  700.00),
        unitRow("Монтаж вывески до 2 м.п.",                                        "PIECE",        2500.00),
        unitRow("Монтаж вывески от 2 до 4 м.п.",                                   "PIECE",        4000.00),
        unitRow("Монтаж вывески от 4 до 6 м.п.",                                   "PIECE",        6000.00),
        unitRow("Поклейка постера на плоскость",                                   "PIECE",        2000.00),
        unitRow("Монтаж таблички на дистанционные крепления",                      "PIECE",        1000.00),
        unitRow("Монтаж таблички на двухсторонний скотч",                          "LINEAR_METER",   80.00),
        unitRow("Лазерная резка (раскрой материала)",                              "LINEAR_METER",  100.00),
        unitRow("Лазерная гравировка",                                             "SQUARE_METER", 100000.00),
        unitRow("Фрезеровка (раскрой материала)",                                  "LINEAR_METER",  100.00),
        unitRow("2D-гравировка",                                                   "SQUARE_METER", 30000.00),
        unitRow("3D-гравировка",                                                   "SQUARE_METER", 60000.00),
        unitRow("УФ-печать (пластик, оргстекло, композит, холст) без белого слоя", "SQUARE_METER",  1500.00),
        unitRow("УФ-печать (пластик, оргстекло, композит, холст) с белым слоем",   "SQUARE_METER",  2500.00),
        unitRow("УФ-печать (стекло, акрил, кафель, дерево, металл) без белого слоя","SQUARE_METER", 3000.00),
        unitRow("УФ-печать (стекло, акрил, кафель, дерево, металл) с белым слоем", "SQUARE_METER",  4000.00),
        unitRow("УФ-печать лаком",                                                 "SQUARE_METER",  2000.00),
        unitRow("УФ-печать праймером",                                             "SQUARE_METER",  1500.00),
        unitRow("УФ-печать с двойной заливкой (3D-эффект)",                        "SQUARE_METER", 15000.00)
    );

    private static final List<Object[]> DEFAULT_OPERATIONS =
        List.<Object[]>of(unitRow("Ручная выборка пленки", "SQUARE_METER", 100.00));

    private final JdbcTemplate jdbcTemplate;

    private static Object[] row(String name, String unit, Double price) {
        return new Object[]{name, unit, price};
    }

    private static Object[] unitRow(String name, String unitType, Double price) {
        return new Object[]{name, unitType, price};
    }

    /**
     * Создаёт справочники (материалы + операции) для новой компании.
     * Записи с тем же названием, уже существующие у этой компании, не дублируются.
     */
    public void seedDefaultsForCompany(Long companyId) {
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO svschema.materials (name, type, unit, price, waste_coefficient, company_id, created_at, deleted)
            SELECT ?, 'MATERIAL', ?, ?, 1.0, ?, NOW(), FALSE
            WHERE NOT EXISTS (
                SELECT 1 FROM svschema.materials
                WHERE name = ? AND company_id = ?
            )
            """,
            MATERIALS.stream()
                .map(r -> new Object[]{r[0], r[1], r[2], companyId, r[0], companyId})
                .toList()
        );

        jdbcTemplate.batchUpdate(
            """
            INSERT INTO svschema.calculator_operations (name, price, unit_type, is_default, company_id, created_at, deleted)
            SELECT ?, ?, ?, FALSE, ?, NOW(), FALSE
            WHERE NOT EXISTS (
                SELECT 1 FROM svschema.calculator_operations
                WHERE name = ? AND company_id = ?
            )
            """,
            OPERATIONS.stream()
                .map(r -> new Object[]{r[0], r[2], r[1], companyId, r[0], companyId})
                .toList()
        );

        jdbcTemplate.batchUpdate(
            """
            INSERT INTO svschema.calculator_operations (name, price, unit_type, is_default, company_id, created_at, deleted)
            SELECT ?, ?, ?, TRUE, ?, NOW(), FALSE
            WHERE NOT EXISTS (
                SELECT 1 FROM svschema.calculator_operations
                WHERE name = ? AND company_id = ?
            )
            """,
            DEFAULT_OPERATIONS.stream()
                .map(r -> new Object[]{r[0], r[2], r[1], companyId, r[0], companyId})
                .toList()
        );
    }
}
