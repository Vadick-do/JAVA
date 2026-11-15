package ru.coursework.booking.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.coursework.booking.core.config.AppConfig;
import ru.coursework.booking.core.domain.Resource;
import ru.coursework.booking.core.domain.Slot;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.core.repository.postgres.MigrationRunner;
import ru.coursework.booking.features.bookings.repository.BookingRepository;
import ru.coursework.booking.features.bookings.repository.postgres.PostgresBookingRepository;
import ru.coursework.booking.features.resources.repository.ResourceRepository;
import ru.coursework.booking.features.resources.repository.postgres.PostgresResourceRepository;
import ru.coursework.booking.features.slots.repository.SlotRepository;
import ru.coursework.booking.features.slots.repository.postgres.PostgresSlotRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Timeout(20)
class BookingTransactionIT {
    private static ConnectionFactory connections;
    private final ResourceRepository resources = new PostgresResourceRepository();
    private final SlotRepository slots = new PostgresSlotRepository();
    private final BookingRepository bookings = new PostgresBookingRepository();

    @BeforeAll
    static void prepareDatabase(@TempDir Path directory) throws Exception {
        assertEquals("booking_coursework_test", System.getenv("DB_NAME"));
        Path file = Files.writeString(directory.resolve("application.properties"), "db.host=postgres\n");
        AppConfig config = AppConfig.load(file, System.getenv());
        connections = new ConnectionFactory(config);
        new MigrationRunner(config).migrate();
    }

    @BeforeEach
    void prepareData() throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT current_database()")) {
                assertTrue(rows.next());
                assertEquals("booking_coursework_test", rows.getString(1));
            }
            statement.executeUpdate("DELETE FROM bookings");
            statement.executeUpdate("DELETE FROM slots");
            statement.executeUpdate("DELETE FROM resources");
            statement.executeUpdate("""
                    INSERT INTO resources (id, name, type, capacity, active) VALUES
                    (7, 'Стол', 'TABLE', 4, TRUE),
                    (9, 'Закрытый зал', 'MEETING_ROOM', 8, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO slots (id, resource_id, starts_at, ends_at)
                    SELECT id, 7, date_trunc('hour', statement_timestamp(), 'UTC') + id * INTERVAL '1 day',
                                  date_trunc('hour', statement_timestamp(), 'UTC') + id * INTERVAL '1 day' + INTERVAL '1 hour'
                    FROM generate_series(1, 2) AS data(id)
                    """);
        }
    }

    @ParameterizedTest
    @CsvSource({"SLOT, true", "SLOT, false", "RESOURCE, true", "RESOURCE, false"})
    void waitsForSameRowUntilCommitOrRollbackAndReadsFreshTime(String row, boolean commit) throws Exception {
        Connection first = connections.open();
        Connection second = connections.open();
        try (first; second; Connection observer = connections.open()) {
            first.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            first.setAutoCommit(false);
            second.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            second.setAutoCommit(false);
            if (row.equals("SLOT")) {
                assertTrue(slots.lockById(first, 1).isPresent());
            } else {
                assertTrue(resources.lockById(first, 7).isPresent());
            }
            Instant transactionStartedAt = transactionTime(second);
            int firstPid = backendPid(first);
            int secondPid = backendPid(second);
            ExecutorService worker = Executors.newSingleThreadExecutor();
            try {
                Future<Instant> result = worker.submit(() -> {
                    Slot preliminary = slots.findById(second, 1).orElseThrow();
                    Resource resource = resources.lockById(second, preliminary.getResourceId()).orElseThrow();
                    assertTrue(resource.isActive());
                    Slot locked = slots.lockById(second, preliminary.getId()).orElseThrow();
                    Instant now = bookings.currentTime(second);
                    assertFalse(bookings.hasActiveBooking(second, locked.getId(), now));
                    assertFalse(second.getAutoCommit());
                    second.commit();
                    return now;
                });
                awaitBlocked(observer, firstPid, secondPid, result);
                assertFalse(result.isDone());
                Instant beforeRelease = bookings.currentTime(first);
                if (commit) {
                    first.commit();
                } else {
                    first.rollback();
                }
                Instant afterWait = result.get(7, TimeUnit.SECONDS);
                assertTrue(afterWait.isAfter(transactionStartedAt));
                assertFalse(afterWait.isBefore(beforeRelease));
                assertFalse(first.isClosed());
                assertFalse(second.isClosed());
                assertFalse(first.getAutoCommit());
                assertFalse(second.getAutoCommit());
            } finally {
                try {
                    first.rollback();
                } finally {
                    stopWorker(worker);
                    second.rollback();
                }
            }
        }
        assertTrue(first.isClosed());
        assertTrue(second.isClosed());
    }

    @Test
    void differentSlotCanBeLockedWhileFirstSlotIsHeld() throws Exception {
        try (Connection first = connections.open(); Connection second = connections.open()) {
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            assertTrue(slots.lockById(first, 1).isPresent());
            ExecutorService worker = Executors.newSingleThreadExecutor();
            try {
                Future<Slot> result = worker.submit(() -> {
                    Slot slot = slots.lockById(second, 2).orElseThrow();
                    second.commit();
                    return slot;
                });
                assertEquals(2, result.get(3, TimeUnit.SECONDS).getId());
            } finally {
                try {
                    first.rollback();
                } finally {
                    stopWorker(worker);
                    second.rollback();
                }
            }
        }
    }

    @Test
    void requiresExplicitTransactionAndDistinguishesMissingRows() throws SQLException {
        try (Connection connection = connections.open()) {
            assertTrue(connection.getAutoCommit());
            assertThrows(IllegalStateException.class, () -> resources.lockById(connection, 7));
            assertThrows(IllegalStateException.class, () -> slots.lockById(connection, 1));
            assertTrue(slots.findById(connection, 99).isEmpty());
            Slot slot = slots.findById(connection, 1).orElseThrow();
            assertEquals(7, slot.getResourceId());
            assertEquals(Duration.ofHours(1), Duration.between(slot.getStartsAt(), slot.getEndsAt()));
            connection.setAutoCommit(false);
            assertTrue(resources.lockById(connection, 99).isEmpty());
            assertTrue(slots.lockById(connection, 99).isEmpty());
            assertFalse(resources.lockById(connection, 9).orElseThrow().isActive());
            assertFalse(connection.getAutoCommit());
            assertFalse(connection.isClosed());
            connection.rollback();
        }
    }

    @Test
    void keepsChangesPrivateAndRollsBackAllWorkOnSharedConnection() throws SQLException {
        try (Connection transaction = connections.open(); Connection observer = connections.open()) {
            transaction.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            transaction.setAutoCommit(false);
            try {
                assertTrue(resources.lockById(transaction, 7).isPresent());
                assertTrue(slots.lockById(transaction, 1).isPresent());
                Instant now = bookings.currentTime(transaction);
                try (var statement = transaction.createStatement()) {
                    statement.executeUpdate("UPDATE resources SET capacity = 6 WHERE id = 7");
                }
                insertBooking(transaction, "HOLD", now.minusSeconds(60));
                assertTrue(bookings.hasActiveBooking(transaction, 1, now));
                assertFalse(bookings.hasActiveBooking(observer, 1, now));
                assertEquals(4, resources.findById(observer, 7).orElseThrow().getCapacity());
                assertEquals(6, resources.findById(transaction, 7).orElseThrow().getCapacity());
                assertFalse(transaction.getAutoCommit());
            } finally {
                transaction.rollback();
            }
            assertEquals(4, resources.findById(observer, 7).orElseThrow().getCapacity());
            assertFalse(bookings.hasActiveBooking(observer, 1, bookings.currentTime(observer)));
            assertFalse(transaction.isClosed());
        }
    }

    @Test
    void lockTimeoutCanBeRolledBackAndConnectionReused() throws SQLException {
        try (Connection first = connections.open(); Connection second = connections.open()) {
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            try {
                assertTrue(slots.lockById(first, 1).isPresent());
                try (var statement = second.createStatement()) {
                    statement.execute("SET LOCAL lock_timeout = '150ms'");
                }
                SQLException error = assertThrows(SQLException.class, () -> slots.lockById(second, 1));
                assertEquals("55P03", error.getSQLState());
                second.rollback();
                first.rollback();
                assertTrue(slots.lockById(second, 1).isPresent());
                assertFalse(second.isClosed());
                assertFalse(second.getAutoCommit());
            } finally {
                first.rollback();
                second.rollback();
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
            "HOLD, 1, true", "HOLD, 0, false", "HOLD, -1, false",
            "CONFIRMED, -1, true", "CANCELLED, 1, false", "CANCELLED, -1, false",
            "EXPIRED, -1, false", "EXPIRED, 1, false"
    })
    void checksBookingAtSuppliedDatabaseTimeWithoutChangingStatus(String status, int expiresInSeconds,
                                                                boolean active) throws SQLException {
        try (Connection connection = connections.open()) {
            Instant now = bookings.currentTime(connection);
            assertFalse(bookings.hasActiveBooking(connection, 1, now));
            insertBooking(connection, status, now.plusSeconds(expiresInSeconds).minus(Duration.ofMinutes(10)));

            assertEquals(active, bookings.hasActiveBooking(connection, 1, now));
            assertFalse(bookings.hasActiveBooking(connection, 2, now));
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT status FROM bookings")) {
                assertTrue(rows.next());
                assertEquals(status, rows.getString(1));
                assertFalse(rows.next());
            }
        }
    }

    private void insertBooking(Connection connection, String status, Instant createdAt) throws SQLException {
        try (var statement = connection.prepareStatement("""
                WITH data AS (SELECT ?::uuid AS id, ?::varchar AS status, ?::timestamptz AS created_at)
                INSERT INTO bookings (id, slot_id, status, created_at, expires_at,
                                      customer_name, customer_contact, confirmed_at, cancelled_at)
                SELECT id, 1, status, created_at, created_at + INTERVAL '10 minutes',
                       CASE WHEN status = 'CONFIRMED' THEN 'Тест' END,
                       CASE WHEN status = 'CONFIRMED' THEN 'test@example.test' END,
                       CASE WHEN status = 'CONFIRMED' THEN created_at + INTERVAL '1 second' END,
                       CASE WHEN status = 'CANCELLED' THEN created_at + INTERVAL '2 seconds' END
                FROM data
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, status);
            statement.setObject(3, createdAt.atOffset(ZoneOffset.UTC));
            statement.executeUpdate();
        }
    }

    private int backendPid(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT pg_backend_pid()")) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }

    private Instant transactionTime(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT transaction_timestamp()")) {
            assertTrue(rows.next());
            return rows.getObject(1, OffsetDateTime.class).toInstant();
        }
    }

    private void awaitBlocked(Connection observer, int blockerPid, int waiterPid, Future<?> result) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        try (var statement = observer.prepareStatement("SELECT ? = ANY(pg_blocking_pids(?))")) {
            statement.setQueryTimeout(1);
            statement.setInt(1, blockerPid);
            statement.setInt(2, waiterPid);
            while (System.nanoTime() < deadline) {
                try (var rows = statement.executeQuery()) {
                    assertTrue(rows.next());
                    if (rows.getBoolean(1)) {
                        return;
                    }
                }
                if (result.isDone()) {
                    result.get(1, TimeUnit.SECONDS);
                    fail("Второй запрос завершился без ожидания блокировки.");
                }
                TimeUnit.MILLISECONDS.sleep(10);
            }
        }
        fail("PostgreSQL не подтвердила ожидание блокировки за 3 секунды.");
    }

    private void stopWorker(ExecutorService worker) throws InterruptedException {
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(7, TimeUnit.SECONDS));
    }
}
