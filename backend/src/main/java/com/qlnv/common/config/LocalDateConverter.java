package com.qlnv.common.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute != null ? attribute.toString() : null;
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        dbData = dbData.trim();
        if (dbData.length() >= 10) {
            String datePart = dbData.substring(0, 10);
            try {
                return LocalDate.parse(datePart);
            } catch (Exception ignored) {}
        }
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(dbData, formatter);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
