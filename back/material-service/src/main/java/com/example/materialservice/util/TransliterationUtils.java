package com.example.materialservice.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Утилита для транслитерации кириллицы в латиницу.
 * Используется для автоматической генерации paramKey из displayName.
 */
public final class TransliterationUtils {

    private static final Map<Character, String> CYRILLIC_TO_LATIN;

    static {
        Map<Character, String> mapping = new LinkedHashMap<>();
        // А-Я (прописные)
        mapping.put('А', "A");
        mapping.put('Б', "B");
        mapping.put('В', "V");
        mapping.put('Г', "G");
        mapping.put('Д', "D");
        mapping.put('Е', "E");
        mapping.put('Ё', "Yo");
        mapping.put('Ж', "Zh");
        mapping.put('З', "Z");
        mapping.put('И', "I");
        mapping.put('Й', "Y");
        mapping.put('К', "K");
        mapping.put('Л', "L");
        mapping.put('М', "M");
        mapping.put('Н', "N");
        mapping.put('О', "O");
        mapping.put('П', "P");
        mapping.put('Р', "R");
        mapping.put('С', "S");
        mapping.put('Т', "T");
        mapping.put('У', "U");
        mapping.put('Ф', "F");
        mapping.put('Х', "Kh");
        mapping.put('Ц', "Ts");
        mapping.put('Ч', "Ch");
        mapping.put('Ш', "Sh");
        mapping.put('Щ', "Shch");
        mapping.put('Ъ', "");
        mapping.put('Ы', "Y");
        mapping.put('Ь', "");
        mapping.put('Э', "E");
        mapping.put('Ю', "Yu");
        mapping.put('Я', "Ya");
        // а-я (строчные)
        mapping.put('а', "a");
        mapping.put('б', "b");
        mapping.put('в', "v");
        mapping.put('г', "g");
        mapping.put('д', "d");
        mapping.put('е', "e");
        mapping.put('ё', "yo");
        mapping.put('ж', "zh");
        mapping.put('з', "z");
        mapping.put('и', "i");
        mapping.put('й', "y");
        mapping.put('к', "k");
        mapping.put('л', "l");
        mapping.put('м', "m");
        mapping.put('н', "n");
        mapping.put('о', "o");
        mapping.put('п', "p");
        mapping.put('р', "r");
        mapping.put('с', "s");
        mapping.put('т', "t");
        mapping.put('у', "u");
        mapping.put('ф', "f");
        mapping.put('х', "kh");
        mapping.put('ц', "ts");
        mapping.put('ч', "ch");
        mapping.put('ш', "sh");
        mapping.put('щ', "shch");
        mapping.put('ъ', "");
        mapping.put('ы', "y");
        mapping.put('ь', "");
        mapping.put('э', "e");
        mapping.put('ю', "yu");
        mapping.put('я', "ya");

        CYRILLIC_TO_LATIN = mapping;
    }

    private TransliterationUtils() {
        // utility class
    }

    /**
     * Транслитерирует строку с кириллицы на латиницу.
     * Также: заменяет пробелы и недопустимые символы на подчёркивания,
     * приводит к lowerCamelCase.
     *
     * @param input исходная строка (например "Ширина (мм)")
     * @return транслитерированный ключ (например "shirina_mm")
     */
    public static String transliterate(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String lower = input.toLowerCase().trim();
        int i = 0;
        while (i < lower.length()) {
            char c = lower.charAt(i);
            String mapped = CYRILLIC_TO_LATIN.getOrDefault(c, String.valueOf(c));
            result.append(mapped);
            i++;
        }

        String key = result.toString();

        // Replace any non-alphanumeric with underscore, collapse multiple underscores
        key = key.replaceAll("[^a-zA-Z0-9]+", "_");
        key = key.replaceAll("_+", "_");
        key = key.replaceAll("^_|_$", "");

        // Lowercase first char for camelCase
        if (key.length() > 0) {
            key = Character.toLowerCase(key.charAt(0)) + key.substring(1);
        }

        return key;
    }

    /**
     * Транслитерирует и делает первый символ заглавным (PascalCase).
     */
    public static String transliteratePascalCase(String input) {
        String camel = transliterate(input);
        if (camel.isEmpty()) return camel;
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /**
     * Проверяет, содержит ли строка кириллические символы.
     */
    public static boolean containsCyrillic(String input) {
        if (input == null) return false;
        return input.matches(".*[А-Яа-яЁё].*");
    }
}
