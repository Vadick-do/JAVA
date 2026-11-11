package ru.coursework.booking.features.resources.repository.postgres;

import ru.coursework.booking.core.domain.Resource;
import ru.coursework.booking.core.domain.ResourceType;
import ru.coursework.booking.features.resources.repository.ResourceRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PostgresResourceRepository implements ResourceRepository {
    @Override
    public List<Resource> findAll(Connection connection) throws SQLException {
        String sql = "SELECT id, name, type, capacity, active FROM resources ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<Resource> resources = new ArrayList<>();
            while (rows.next()) {
                resources.add(readResource(rows));
            }
            return resources;
        }
    }

    @Override
    public Optional<Resource> findById(Connection connection, long id) throws SQLException {
        String sql = "SELECT id, name, type, capacity, active FROM resources WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    return Optional.of(readResource(rows));
                }
                return Optional.empty();
            }
        }
    }

    private Resource readResource(ResultSet rows) throws SQLException {
        return new Resource(
                rows.getLong("id"),
                rows.getString("name"),
                ResourceType.valueOf(rows.getString("type")),
                rows.getInt("capacity"),
                rows.getBoolean("active")
        );
    }
}
