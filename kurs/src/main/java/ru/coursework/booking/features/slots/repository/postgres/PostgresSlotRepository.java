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

public final class PostgresSlotRepository implements SlotRepository {
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
