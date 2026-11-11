package ru.coursework.booking.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.coursework.booking.core.config.AppConfig;
import ru.coursework.booking.core.domain.Resource;
import ru.coursework.booking.core.domain.ResourceType;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.core.repository.postgres.MigrationRunner;
import ru.coursework.booking.features.resources.repository.ResourceRepository;
import ru.coursework.booking.features.resources.repository.postgres.PostgresResourceRepository;
import ru.coursework.booking.features.resources.service.ResourceService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceServiceIT {
    private static ConnectionFactory connections;
    private TrackingRepository repository;
    private ResourceService service;

    @BeforeAll
    static void prepareDatabase(@TempDir Path directory) throws Exception {
        assertEquals("booking_coursework_test", System.getenv("DB_NAME"));
        Path file = Files.writeString(directory.resolve("application.properties"), "db.host=postgres\n");
        AppConfig config = AppConfig.load(file, System.getenv());
        connections = new ConnectionFactory(config);
        new MigrationRunner(config).migrate();
    }

    @BeforeEach
    void prepareServiceAndEmptyResources() throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT current_database()")) {
                assertTrue(rows.next());
                assertEquals("booking_coursework_test", rows.getString(1));
            }
            statement.executeUpdate("DELETE FROM resources");
        }
        repository = new TrackingRepository();
        service = new ResourceService(connections, repository);
    }

    @Test
    void distinguishesEmptyListAndMissingId() throws SQLException {
        assertTrue(service.findAll().isEmpty());
        assertTrue(repository.lastConnection.isClosed());
        assertTrue(service.findById(1).isEmpty());
        assertTrue(repository.lastConnection.isClosed());
    }

    @Test
    void readsBothTypesIncludingInactiveResourcesInIdOrder() throws SQLException {
        insertResource(3_000_000_000L, "Зал 'Север'", "MEETING_ROOM", 12, false);
        insertResource(7, "Стол у окна", "TABLE", 4, true);

        List<Resource> resources = service.findAll();

        assertEquals(2, resources.size());
        assertResource(resources.get(0), 7, "Стол у окна", ResourceType.TABLE, 4, true);
        assertResource(resources.get(1), 3_000_000_000L, "Зал 'Север'", ResourceType.MEETING_ROOM, 12, false);
        assertTrue(repository.lastConnection.isClosed());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void readsCardRegardlessOfActiveFlag(boolean active) throws SQLException {
        insertResource(3_000_000_000L, "Переговорная", "MEETING_ROOM", 8, active);

        Resource resource = service.findById(3_000_000_000L).orElseThrow();

        assertResource(resource, 3_000_000_000L, "Переговорная", ResourceType.MEETING_ROOM, 8, active);
        assertTrue(repository.lastConnection.isClosed());
        assertTrue(service.findById(7).isEmpty());
        assertTrue(repository.lastConnection.isClosed());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveId(long id) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.findById(id));

        assertEquals("ID ресурса должен быть положительным.", error.getMessage());
    }

    @Test
    void leavesTransactionAndConnectionUnderCallerControl() throws SQLException {
        PostgresResourceRepository postgres = new PostgresResourceRepository();
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("""
                    INSERT INTO resources (id, name, type, capacity, active)
                    VALUES (9, 'Незафиксированный стол', 'TABLE', 2, TRUE)
                    """)) {
                statement.executeUpdate();
            }

            assertEquals(1, postgres.findAll(connection).size());
            assertTrue(postgres.findById(connection, 9).isPresent());
            assertFalse(connection.isClosed());
            assertFalse(connection.getAutoCommit());
            connection.rollback();
            assertTrue(postgres.findAll(connection).isEmpty());
        }
        assertTrue(service.findAll().isEmpty());
        assertTrue(repository.lastConnection.isClosed());
    }

    @Test
    void closesConnectionsAndPreservesRepositoryFailure() throws SQLException {
        SQLException failure = new SQLException("Тестовая ошибка чтения.", "08006");
        repository.failure = failure;

        assertSame(failure, assertThrows(SQLException.class, () -> service.findAll()));
        assertTrue(repository.lastConnection.isClosed());
        assertSame(failure, assertThrows(SQLException.class, () -> service.findById(1)));
        assertTrue(repository.lastConnection.isClosed());
    }

    private void insertResource(long id, String name, String type, int capacity, boolean active) throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.prepareStatement("""
                INSERT INTO resources (id, name, type, capacity, active) VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, id);
            statement.setString(2, name);
            statement.setString(3, type);
            statement.setInt(4, capacity);
            statement.setBoolean(5, active);
            statement.executeUpdate();
        }
    }

    private void assertResource(Resource resource, long id, String name, ResourceType type, int capacity, boolean active) {
        assertEquals(id, resource.getId());
        assertEquals(name, resource.getName());
        assertEquals(type, resource.getType());
        assertEquals(capacity, resource.getCapacity());
        assertEquals(active, resource.isActive());
    }

    private static final class TrackingRepository implements ResourceRepository {
        private final ResourceRepository delegate = new PostgresResourceRepository();
        private Connection lastConnection;
        private SQLException failure;

        @Override
        public List<Resource> findAll(Connection connection) throws SQLException {
            track(connection);
            return delegate.findAll(connection);
        }

        @Override
        public Optional<Resource> findById(Connection connection, long id) throws SQLException {
            track(connection);
            return delegate.findById(connection, id);
        }

        private void track(Connection connection) throws SQLException {
            lastConnection = connection;
            if (failure != null) {
                throw failure;
            }
        }
    }
}
