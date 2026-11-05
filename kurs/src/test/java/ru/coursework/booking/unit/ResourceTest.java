package ru.coursework.booking.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.coursework.booking.core.domain.Resource;
import ru.coursework.booking.core.domain.ResourceType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceTest {
    @ParameterizedTest
    @EnumSource(ResourceType.class)
    void acceptsBothTypesAndNormalizesName(ResourceType type) {
        Resource resource = new Resource(1, "  Зал 1\u2003", type, 4, true);

        assertEquals("Зал 1", resource.getName());
        assertEquals(type, resource.getType());
    }

    @Test
    void acceptsInactiveResourceWithMinimumCapacity() {
        Resource resource = new Resource(1, "Стол 1", ResourceType.TABLE, 1, false);

        assertFalse(resource.isActive());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void rejectsMissingOrBlankName(String name) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Resource(1, name, ResourceType.TABLE, 4, true));

        assertEquals("Название ресурса не должно быть пустым.", exception.getMessage());
    }

    @Test
    void checksNameLengthAfterNormalizationByUnicodeCharacters() {
        String name = "😀".repeat(100);
        Resource resource = new Resource(1, " " + name + " ", ResourceType.TABLE, 4, true);

        assertEquals(name, resource.getName());
        assertThrows(IllegalArgumentException.class,
                () -> new Resource(1, name + "я", ResourceType.TABLE, 4, true));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveId(long id) {
        assertThrows(IllegalArgumentException.class,
                () -> new Resource(id, "Стол 1", ResourceType.TABLE, 4, true));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveCapacity(int capacity) {
        assertThrows(IllegalArgumentException.class,
                () -> new Resource(1, "Стол 1", ResourceType.TABLE, capacity, true));
    }

    @Test
    void rejectsMissingType() {
        assertThrows(IllegalArgumentException.class,
                () -> new Resource(1, "Стол 1", null, 4, true));
    }
}
