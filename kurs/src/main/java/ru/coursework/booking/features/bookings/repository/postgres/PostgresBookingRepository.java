package ru.coursework.booking.features.bookings.repository.postgres;

import ru.coursework.booking.core.domain.Booking;
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
    public void expireHolds(Connection connection, long slotId, Instant now) throws SQLException {
        String sql = """
                UPDATE bookings b SET status = 'EXPIRED'
                FROM slots s
                WHERE b.slot_id = s.id AND b.slot_id = ? AND b.status = 'HOLD'
                  AND (b.expires_at <= ? OR s.starts_at <= ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            statement.setLong(1, slotId);
            statement.setObject(2, now.atOffset(ZoneOffset.UTC));
            statement.setObject(3, now.atOffset(ZoneOffset.UTC));
            statement.executeUpdate();
        }
    }

    @Override
    public void insertHold(Connection connection, Booking booking) throws SQLException {
        String sql = """
                INSERT INTO bookings (id, slot_id, status, created_at, expires_at)
                VALUES (?, ?, 'HOLD', ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            statement.setObject(1, booking.getId());
            statement.setLong(2, booking.getSlotId());
            statement.setObject(3, booking.getCreatedAt().atOffset(ZoneOffset.UTC));
            statement.setObject(4, booking.getExpiresAt().atOffset(ZoneOffset.UTC));
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Не удалось создать холд.");
            }
        }
    }

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
