package ru.coursework.booking.features.resources.repository;

import ru.coursework.booking.core.domain.Resource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ResourceRepository {
    List<Resource> findAll(Connection connection) throws SQLException;

    Optional<Resource> findById(Connection connection, long id) throws SQLException;
}
