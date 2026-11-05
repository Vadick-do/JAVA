package ru.coursework.booking.unit;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.coursework.booking.core.domain.Slot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlotTest {
    private final Instant startsAt = Instant.parse("2026-11-05T10:00:00Z");
    private final Instant endsAt = Instant.parse("2026-11-05T11:00:00Z");

    @Test
    void acceptsHourSlotAndAdjacentSlot() {
        Slot first = new Slot(1, 1, startsAt, endsAt);
        Slot second = new Slot(2, 1, endsAt, Instant.parse("2026-11-05T12:00:00Z"));

        assertEquals(first.getEndsAt(), second.getStartsAt());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveId(long id) {
        assertThrows(IllegalArgumentException.class,
                () -> new Slot(id, 1, startsAt, endsAt));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveResourceId(long resourceId) {
        assertThrows(IllegalArgumentException.class,
                () -> new Slot(1, resourceId, startsAt, endsAt));
    }

    @Test
    void rejectsMissingTimeBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> new Slot(1, 1, null, endsAt));
        assertThrows(IllegalArgumentException.class,
                () -> new Slot(1, 1, startsAt, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2026-11-05T10:01:00Z",
            "2026-11-05T10:00:01Z",
            "2026-11-05T10:00:00.000000001Z"
    })
    void rejectsStartOutsideUtcHourBoundary(String value) {
        Instant invalidStart = Instant.parse(value);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Slot(1, 1, invalidStart, invalidStart.plusSeconds(3600)));

        assertEquals("Слот должен начинаться на границе часа UTC.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(longs = {-3600, 0, 3599, 3601, 7200})
    void rejectsDurationOtherThanOneHour(long seconds) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Slot(1, 1, startsAt, startsAt.plusSeconds(seconds)));

        assertEquals("Слот должен длиться ровно 60 минут.", exception.getMessage());
    }

    @Test
    void rejectsFractionalSecondInDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> new Slot(1, 1, startsAt, endsAt.plusNanos(1)));
    }
}
