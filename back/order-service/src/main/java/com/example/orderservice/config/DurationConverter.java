package com.example.orderservice.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Duration;

@Converter(autoApply = true)
public class DurationConverter implements AttributeConverter<Duration, String> {

    @Override
    public String convertToDatabaseColumn(Duration attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toString();
    }

    @Override
    public Duration convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return Duration.parse(dbData);
    }
}
