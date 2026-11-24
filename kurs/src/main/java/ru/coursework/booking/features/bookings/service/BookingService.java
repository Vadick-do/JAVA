package ru.coursework.booking.features.bookings.service;

import ru.coursework.booking.core.domain.Booking;
import ru.coursework.booking.core.domain.BookingStatus;
import ru.coursework.booking.core.domain.Resource;
import ru.coursework.booking.core.domain.Slot;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.features.bookings.repository.BookingRepository;
import ru.coursework.booking.features.bookings.repository.BookingRepository.ExpiredHoldCandidate;
import ru.coursework.booking.features.resources.repository.ResourceRepository;
import ru.coursework.booking.features.slots.repository.SlotRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public final class BookingService {
    private static final int EXPIRATION_BATCH_SIZE = 100;

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
                rollback(connection, error);
                throw error;
            }
        }
    }

    public Booking confirmBooking(UUID id, String name, String contact) throws SQLException {
        requireBookingId(id);
        Booking.validateCustomerDetails(name, contact);
        Booking result;
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                Booking booking = readBookingUnderSlotLock(connection, id);
                Instant now = bookings.currentTime(connection);
                result = booking.expire(now);
                if (result.getStatus() != BookingStatus.EXPIRED) {
                    result = booking.confirm(name, contact, now);
                    if (result != booking) {
                        bookings.updateConfirmation(connection, result);
                    }
                } else if (result != booking) {
                    bookings.expireHolds(connection, booking.getSlotId(), now);
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        }
        if (result.getStatus() == BookingStatus.EXPIRED) {
            throw new IllegalStateException("Срок холда истёк.");
        }
        return result;
    }

    public Booking cancelBooking(UUID id) throws SQLException {
        requireBookingId(id);
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                Booking booking = readBookingUnderSlotLock(connection, id);
                Instant now = bookings.currentTime(connection);
                Booking result = booking.cancel(now);
                if (result != booking) {
                    if (result.getStatus() == BookingStatus.EXPIRED) {
                        bookings.expireHolds(connection, booking.getSlotId(), now);
                    } else {
                        bookings.updateCancellation(connection, result);
                    }
                }
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        }
    }

    public Booking findById(UUID id) throws SQLException {
        requireBookingId(id);
        try (Connection connection = connections.open()) {
            Booking booking = requireBooking(connection, id);
            return booking.expire(bookings.currentTime(connection));
        }
    }

    public int expirePendingHolds() throws SQLException {
        List<ExpiredHoldCandidate> candidates;
        try (Connection connection = connections.openForCleanup()) {
            candidates = bookings.findExpiredHoldCandidates(connection, EXPIRATION_BATCH_SIZE);
        }
        int expiredCount = 0;
        for (ExpiredHoldCandidate candidate : candidates) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            if (expireHold(candidate)) {
                expiredCount++;
            }
        }
        return expiredCount;
    }

    private boolean expireHold(ExpiredHoldCandidate candidate) throws SQLException {
        try (Connection connection = connections.openForCleanup()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                boolean expired = false;
                if (slots.lockById(connection, candidate.slotId()).isPresent()) {
                    Booking booking = bookings.findById(connection, candidate.id()).orElse(null);
                    if (booking != null && booking.getSlotId() == candidate.slotId()) {
                        Instant now = bookings.currentTime(connection);
                        expired = booking.expire(now) != booking
                                && bookings.expireHolds(connection, candidate.slotId(), now) == 1;
                    }
                }
                connection.commit();
                return expired;
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        }
    }

    private void requireBookingId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID брони должен быть указан.");
        }
    }

    private Booking requireBooking(Connection connection, UUID id) throws SQLException {
        return bookings.findById(connection, id)
                .orElseThrow(() -> new NoSuchElementException("Бронь не найдена."));
    }

    private Booking readBookingUnderSlotLock(Connection connection, UUID id) throws SQLException {
        long slotId = requireBooking(connection, id).getSlotId();
        slots.lockById(connection, slotId)
                .orElseThrow(() -> new NoSuchElementException("Слот не найден."));
        return requireBooking(connection, id);
    }

    private void rollback(Connection connection, Exception error) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            error.addSuppressed(rollbackError);
        }
    }
}
