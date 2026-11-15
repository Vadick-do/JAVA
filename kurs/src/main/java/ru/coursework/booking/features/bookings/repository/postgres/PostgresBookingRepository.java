package ru.coursework.booking.features.bookings.repository.postgres;

import ru.coursework.booking.features.bookings.repository.BookingRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class PostgresBookingRepository implements BookingRepository {
    @Override
    public Instant currentTime(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT clock_timestamp()")) {
            statement.setQueryTimeout(5);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new SQLException("PostgreSQL не вернула текущее время.");
                }
                return rows.getObject(1, OffsetDateTime.class).toInstant();
            }
        }
    }

    @Override
    public boolean hasActiveBooking(Connection connection, long slotId, Instant now) throws SQLException {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM bookings
                    WHERE slot_id = ?
                      AND (status = 'CONFIRMED' OR (status = 'HOLD' AND expires_at > ?))
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            statement.setLong(1, slotId);
            statement.setObject(2, now.atOffset(ZoneOffset.UTC));
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new SQLException("PostgreSQL не вернула результат проверки брони.");
                }
                return rows.getBoolean(1);
            }
        }
    }
}
