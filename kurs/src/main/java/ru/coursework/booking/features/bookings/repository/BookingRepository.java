package ru.coursework.booking.features.bookings.repository;

import ru.coursework.booking.core.domain.Booking;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    Optional<Booking> findById(Connection connection, UUID id) throws SQLException;

    Instant currentTime(Connection connection) throws SQLException;

    boolean hasActiveBooking(Connection connection, long slotId, Instant now) throws SQLException;

    void expireHolds(Connection connection, long slotId, Instant now) throws SQLException;

    void insertHold(Connection connection, Booking booking) throws SQLException;

    void updateConfirmation(Connection connection, Booking booking) throws SQLException;

    void updateCancellation(Connection connection, Booking booking) throws SQLException;
}
