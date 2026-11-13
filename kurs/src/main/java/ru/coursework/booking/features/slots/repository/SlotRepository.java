package ru.coursework.booking.features.slots.repository;

import ru.coursework.booking.core.domain.Slot;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public interface SlotRepository {
    List<Slot> findAvailable(Connection connection, long resourceId, Instant from, Instant to) throws SQLException;
}
