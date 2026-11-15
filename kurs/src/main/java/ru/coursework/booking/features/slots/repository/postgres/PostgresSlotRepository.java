package ru.coursework.booking.features.slots.repository.postgres;

import ru.coursework.booking.core.domain.Slot;
import ru.coursework.booking.features.slots.repository.SlotRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PostgresSlotRepository implements SlotRepository {
    @Override
    public Optional<Slot> findById(Connection connection, long id) throws SQLException {
        String sql = "SELECT id, resource_id, starts_at, ends_at FROM slots WHERE id = ?";
        return findOne(connection, id, sql);
    }

    @Override
    public Optional<Slot> lockById(Connection connection, long id) throws SQLException {
        if (connection.getAutoCommit()) {
            throw new IllegalStateException("Для блокировки слота нужна явная транзакция.");
        }
        String sql = "SELECT id, resource_id, starts_at, ends_at FROM slots WHERE id = ? FOR UPDATE";
        return findOne(connection, id, sql);
    }

    private Optional<Slot> findOne(Connection connection, long id, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    return Optional.of(readSlot(rows));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public List<Slot> findAvailable(Connection connection, long resourceId, Instant from, Instant to) throws SQLException {
        String sql = """
                SELECT s.id, s.resource_id, s.starts_at, s.ends_at
                FROM slots s
                JOIN resources r ON r.id = s.resource_id
                WHERE s.resource_id = ? AND r.active
                  AND s.starts_at >= ? AND s.ends_at <= ?
                  AND s.starts_at > statement_timestamp()
                  AND NOT EXISTS (
                      SELECT 1 FROM bookings b
                      WHERE b.slot_id = s.id
                        AND (b.status = 'CONFIRMED'
                             OR (b.status = 'HOLD' AND b.expires_at > statement_timestamp()))
                  )
                ORDER BY s.starts_at, s.id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Instant startsFrom = from.truncatedTo(ChronoUnit.MICROS);
            if (startsFrom.isBefore(from)) {
                startsFrom = startsFrom.plus(1, ChronoUnit.MICROS);
            }
            statement.setLong(1, resourceId);
            statement.setObject(2, startsFrom.atOffset(ZoneOffset.UTC));
            statement.setObject(3, to.truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC));
            try (ResultSet rows = statement.executeQuery()) {
                List<Slot> slots = new ArrayList<>();
                while (rows.next()) {
                    slots.add(readSlot(rows));
                }
                return slots;
            }
        }
    }

    private Slot readSlot(ResultSet rows) throws SQLException {
        return new Slot(
                rows.getLong("id"),
                rows.getLong("resource_id"),
                rows.getObject("starts_at", OffsetDateTime.class).toInstant(),
                rows.getObject("ends_at", OffsetDateTime.class).toInstant()
        );
    }
}
