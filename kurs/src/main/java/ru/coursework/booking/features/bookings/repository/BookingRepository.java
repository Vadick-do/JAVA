package ru.coursework.booking.features.bookings.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

public interface BookingRepository {
    Instant currentTime(Connection connection) throws SQLException;

    boolean hasActiveBooking(Connection connection, long slotId, Instant now) throws SQLException;
}
