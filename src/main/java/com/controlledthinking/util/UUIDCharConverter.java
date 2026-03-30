package com.controlledthinking.util;

/**
 *
 * @author brintoul
 */
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;

@Converter(autoApply = true)
public class UUIDCharConverter implements AttributeConverter<UUID, String> {

    @Override
    public String convertToDatabaseColumn(UUID uuid) {
        return (uuid == null) ? null : uuid.toString();
    }

    @Override
    public UUID convertToEntityAttribute(String uuidString) {
        return (uuidString == null) ? null : UUID.fromString(uuidString);
    }
}
