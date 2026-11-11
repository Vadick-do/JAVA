package ru.coursework.booking.features.resources.service;

import ru.coursework.booking.core.domain.Resource;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.features.resources.repository.ResourceRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class ResourceService {
    private final ConnectionFactory connections;
    private final ResourceRepository repository;

    public ResourceService(ConnectionFactory connections, ResourceRepository repository) {
        this.connections = connections;
        this.repository = repository;
    }

    public List<Resource> findAll() throws SQLException {
        try (Connection connection = connections.open()) {
            return repository.findAll(connection);
        }
    }

    public Optional<Resource> findById(long id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID ресурса должен быть положительным.");
        }
        try (Connection connection = connections.open()) {
            return repository.findById(connection, id);
        }
    }
}
