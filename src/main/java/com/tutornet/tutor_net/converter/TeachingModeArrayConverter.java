// TeachingModeArrayConverter.java
package com.tutornet.tutor_net.converter;

import com.tutornet.tutor_net.enums.TeachingMode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;

import java.sql.Array;
import java.util.Arrays;

@Converter
public class TeachingModeArrayConverter implements AttributeConverter<TeachingMode[], String> {

    @Override
    public String convertToDatabaseColumn(TeachingMode[] attribute) {
        if (attribute == null || attribute.length == 0) return "{}";
        // Postgres array literal: {online,offline}
        String joined = Arrays.stream(attribute)
                .map(Enum::name)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return "{" + joined + "}";
    }

    @Override
    public TeachingMode[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || dbData.equals("{}")) {
            return new TeachingMode[0];
        }
        // Strip braces: "{online,offline}" → "online,offline"
        String stripped = dbData.replaceAll("[{}]", "");
        return Arrays.stream(stripped.split(","))
                .map(String::trim)
                .map(s -> TeachingMode.valueOf(s.toUpperCase()))
                .toArray(TeachingMode[]::new);
    }
}