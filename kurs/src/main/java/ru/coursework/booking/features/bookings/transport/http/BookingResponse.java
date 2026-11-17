package ru.coursework.booking.features.bookings.transport.http;

import ru.coursework.booking.core.domain.Booking;
import ru.coursework.booking.core.domain.BookingStatus;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(UUID id, long slotId, BookingStatus status, String createdAt, String expiresAt,
                              String customerName, String customerContact, String confirmedAt, String cancelledAt) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getSlotId(), booking.getStatus(),
                booking.getCreatedAt().toString(), booking.getExpiresAt().toString(),
                booking.getCustomerName(), booking.getCustomerContact(),
                formatTime(booking.getConfirmedAt()), formatTime(booking.getCancelledAt()));
    }

    private static String formatTime(Instant value) {
        return value == null ? null : value.toString();
    }
}
