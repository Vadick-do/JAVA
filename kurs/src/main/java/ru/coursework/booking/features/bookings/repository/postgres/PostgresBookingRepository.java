package ru.coursework.booking.features.bookings.repository.postgres;

import ru.coursework.booking.core.domain.Booking;
import ru.coursework.booking.core.domain.BookingStatus;
import ru.coursework.booking.core.domain.Slot;
import ru.coursework.booking.features.bookings.repository.BookingRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

public final class PostgresBookingRepository implements BookingRepository {
    @Override
    public Optional<Booking> findById(Connection connection, UUID id) throws SQLException {
        String sql = """
                SELECT b.id, b.status, b.created_at, b.expires_at, b.customer_name, b.customer_contact,
                       b.confirmed_at, b.cancelled_at, s.id AS slot_id, s.resource_id, s.starts_at, s.ends_at
                FROM bookings b JOIN slots s ON s.id = b.slot_id
                WHERE b.id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            statement.setObject(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                Slot slot = new Slot(rows.getLong("slot_id"), rows.getLong("resource_id"),
                        readInstant(rows, "starts_at"), readInstant(rows, "ends_at"));
                return Optional.of(Booking.restore(rows.getObject("id", UUID.class), slot,
                        BookingStatus.valueOf(rows.getString("status")),
                        readInstant(rows, "created_at"), readInstant(rows, "expires_at"),
                        rows.getString("customer_name"), rows.getString("customer_contact"),
                        readInstant(rows, "confirmed_at"), readInstant(rows, "cancelled_at")));
            }
        }
    }

    private Instant readInstant(ResultSet rows, String column) throws SQLException {
        OffsetDateTime value = rows.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

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
    public void updateConfirmation(Connection connection, Booking booking) throws SQLException {
        String sql = """
                UPDATE bookings
                SET status = 'CONFIRMED', customer_name = ?, customer_contact = ?, confirmed_at = ?
                WHERE id = ? AND status = 'HOLD'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            statement.setString(1, booking.getCustomerName());
            statement.setString(2, booking.getCustomerContact());
            statement.setObject(3, booking.getConfirmedAt().atOffset(ZoneOffset.UTC));
            statement.setObject(4, booking.getId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Не удалось подтвердить холд.");
            }
        }
    }

    @Override
    public void updateCancellation(Connection connection, Booking booking) throws SQLException {
        String sql = """
                UPDATE bookings
                SET status = 'CANCELLED', cancelled_at = ?
                WHERE id = ? AND status IN ('HOLD', 'CONFIRMED')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            statement.setObject(1, booking.getCancelledAt().atOffset(ZoneOffset.UTC));
            statement.setObject(2, booking.getId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Не удалось отменить бронь.");
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
