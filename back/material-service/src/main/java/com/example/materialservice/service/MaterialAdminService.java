package com.example.materialservice.service;

import com.example.materialservice.entity.Material;
import com.example.materialservice.entity.MaterialType;
import com.example.materialservice.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialAdminService {

    private final MaterialRepository materialRepository;
    private Random random = new Random();

    private static final String[] MATERIALS = {
        "Баннер лит. 450 гр/м2", "Баннер лит. 300 гр/м2", "Пленка самоклеящаяся", "Жидкий ламинат",
        "Плоттерная резка", "УФ-печать", "Сушка", "Постой баннера", "Подворот", "Установка люверса",
        "Монтаж на конструкцию", "Установка подсветки", "Пошив штор", "Изготовление стенда",
        "Лазерная резка", "Фрезеровка", "Тиснение фольгой", "Вырубка", "Склейка"
    };

    private static final String[] UNITS = { "м2", "м.п." };

    private static final MaterialType[] TYPES = { MaterialType.MATERIAL, MaterialType.OPERATION };

    /**
     * Сгенерировать тестовые материалы для разработки и тестирования.
     * <p>
     * Создаёт {@code count} случайных материалов с именами из предопределённого массива {@link #MATERIALS}
     * и единицами измерения из {@link #UNITS}. Цена выбирается случайно в диапазоне [100; 10000),
     * коэффициент отходов — в диапазоне [1.0; 2.0) с шагом 0.01.
     * Все материалы сохраняются в базе и возвращаются в виде списка сущностей.
     *
     * @param count количество материалов для генерации (обычно 20)
     * @return список сохранённых сущностей {@link Material} с присвоенными {@code id} и временными метками
     */
    public List<Material> generateTestMaterials(int count) {
        List<Material> materials = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Material material = new Material();
            material.setName(MATERIALS[random.nextInt(MATERIALS.length)] + " " + (i + 1));
            material.setUnit(UNITS[random.nextInt(UNITS.length)]);
            material.setPrice(BigDecimal.valueOf(100 + random.nextInt(9000), 0));
            material.setWasteCoefficient(BigDecimal.ONE.add(BigDecimal.valueOf(random.nextInt(50) / 100.0)));
            material.setType(TYPES[random.nextInt(TYPES.length)]);

            materials.add(materialRepository.save(material));
        }

        return materials;
    }
}
