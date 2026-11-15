package ru.coursework.booking.features.slots.transport.http;

import ru.coursework.booking.core.domain.Slot;

public record SlotResponse(long id, long resourceId, String startsAt, String endsAt) {
    public static SlotResponse from(Slot slot) {
        return new SlotResponse(slot.getId(), slot.getResourceId(),
                slot.getStartsAt().toString(), slot.getEndsAt().toString());
    }
}
