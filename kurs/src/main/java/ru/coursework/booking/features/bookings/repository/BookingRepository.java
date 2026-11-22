package ru.coursework.booking.features.bookings.repository;

import ru.coursework.booking.core.domain.Booking;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    record ExpiredHoldCandidate(UUID id, long slotId) {}

    Optional<Booking> findById(Connection connection, UUID id) throws SQLException;

    List<ExpiredHoldCandidate> findExpiredHoldCandidates(Connection connection, int limit) throws SQLException;

    Instant currentTime(Connection connection) throws SQLException;

    boolean hasActiveBooking(Connection connection, long slotId, Instant now) throws SQLException;

    int expireHolds(Connection connection, long slotId, Instant now) throws SQLException;

    void insertHold(Connection connection, Booking booking) throws SQLException;

    void updateConfirmation(Connection connection, Booking booking) throws SQLException;

    void updateCancellation(Connection connection, Booking booking) throws SQLException;
}
