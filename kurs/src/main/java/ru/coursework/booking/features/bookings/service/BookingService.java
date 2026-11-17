package ru.coursework.booking.features.bookings.service;

import ru.coursework.booking.core.domain.Booking;
import ru.coursework.booking.core.domain.Resource;
import ru.coursework.booking.core.domain.Slot;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.features.bookings.repository.BookingRepository;
import ru.coursework.booking.features.resources.repository.ResourceRepository;
import ru.coursework.booking.features.slots.repository.SlotRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

public final class BookingService {
    private final ConnectionFactory connections;
    private final ResourceRepository resources;
    private final SlotRepository slots;
    private final BookingRepository bookings;

    public BookingService(ConnectionFactory connections, ResourceRepository resources,
                          SlotRepository slots, BookingRepository bookings) {
        this.connections = connections;
        this.resources = resources;
        this.slots = slots;
        this.bookings = bookings;
    }

    public Booking createHold(long slotId) throws SQLException {
        if (slotId <= 0) {
            throw new IllegalArgumentException("ID слота должен быть положительным.");
        }
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                Slot preliminary = slots.findById(connection, slotId)
                        .orElseThrow(() -> new NoSuchElementException("Слот не найден."));
                Resource resource = resources.lockById(connection, preliminary.getResourceId())
                        .orElseThrow(() -> new NoSuchElementException("Ресурс не найден."));
                if (!resource.isActive()) {
                    throw new ResourceInactiveException();
                }
                Slot slot = slots.lockById(connection, slotId)
                        .orElseThrow(() -> new NoSuchElementException("Слот не найден."));
                Instant now = bookings.currentTime(connection);
                if (!now.isBefore(slot.getStartsAt())) {
                    throw new IllegalStateException("Начавшийся слот нельзя удержать.");
                }
                bookings.expireHolds(connection, slotId, now);
                if (bookings.hasActiveBooking(connection, slotId, now)) {
                    throw new IllegalStateException("Слот уже занят.");
                }
                Booking booking = Booking.hold(UUID.randomUUID(), slot, now);
                bookings.insertHold(connection, booking);
                connection.commit();
                return booking;
            } catch (SQLException | RuntimeException error) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            }
        }
    }

    public Booking findById(UUID id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("ID брони должен быть указан.");
        }
        try (Connection connection = connections.open()) {
            Booking booking = bookings.findById(connection, id)
                    .orElseThrow(() -> new NoSuchElementException("Бронь не найдена."));
            return booking.expire(bookings.currentTime(connection));
        }
    }
}
