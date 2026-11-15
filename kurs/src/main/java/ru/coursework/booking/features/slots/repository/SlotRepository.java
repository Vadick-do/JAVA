package ru.coursework.booking.features.slots.repository;

import ru.coursework.booking.core.domain.Slot;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SlotRepository {
    List<Slot> findAvailable(Connection connection, long resourceId, Instant from, Instant to) throws SQLException;

    Optional<Slot> findById(Connection connection, long id) throws SQLException;

    Optional<Slot> lockById(Connection connection, long id) throws SQLException;
}
