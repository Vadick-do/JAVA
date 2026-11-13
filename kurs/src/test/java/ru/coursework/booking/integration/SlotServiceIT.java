package ru.coursework.booking.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.coursework.booking.core.config.AppConfig;
import ru.coursework.booking.core.domain.Slot;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.core.repository.postgres.MigrationRunner;
import ru.coursework.booking.features.resources.repository.postgres.PostgresResourceRepository;
import ru.coursework.booking.features.slots.repository.SlotRepository;
import ru.coursework.booking.features.slots.repository.postgres.PostgresSlotRepository;
import ru.coursework.booking.features.slots.service.SlotService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotServiceIT {
    private static ConnectionFactory connections;
    private TrackingRepository repository;
    private SlotService service;
    private Instant currentHour;
    private Instant from;
    private Instant to;

    @BeforeAll
    static void prepareDatabase(@TempDir Path directory) throws Exception {
        assertEquals("booking_coursework_test", System.getenv("DB_NAME"));
        Path file = Files.writeString(directory.resolve("application.properties"), "db.host=postgres\n");
        AppConfig config = AppConfig.load(file, System.getenv());
        connections = new ConnectionFactory(config);
        new MigrationRunner(config).migrate();
    }

    @BeforeEach
    void prepareServiceAndData() throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT current_database(), date_trunc('hour', clock_timestamp(), 'UTC')")) {
                assertTrue(rows.next());
                assertEquals("booking_coursework_test", rows.getString(1));
                currentHour = rows.getObject(2, OffsetDateTime.class).toInstant();
            }
            statement.executeUpdate("DELETE FROM bookings");
            statement.executeUpdate("DELETE FROM slots");
            statement.executeUpdate("DELETE FROM resources");
            statement.executeUpdate("""
                    INSERT INTO resources (id, name, type, capacity, active) VALUES
                    (7, 'Стол', 'TABLE', 4, TRUE),
                    (8, 'Зал', 'MEETING_ROOM', 8, TRUE),
                    (9, 'Закрытый зал', 'MEETING_ROOM', 8, FALSE)
                    """);
        }
        from = currentHour.plus(Duration.ofDays(1));
        to = from.plus(Duration.ofHours(3));
        repository = new TrackingRepository();
        service = new SlotService(connections, new PostgresResourceRepository(), repository);
    }

    @Test
    void distinguishesNoSlotsMissingResourceAndInactiveResource() throws SQLException {
        assertTrue(service.findAvailable(7, from, to).isEmpty());
        assertTrue(repository.lastConnection.isClosed());
        assertThrows(NoSuchElementException.class, () -> service.findAvailable(10, from, to));
        insertSlot(1, 9, from);
        assertTrue(service.findAvailable(9, from, to).isEmpty());
        try (Connection connection = connections.open()) {
            assertTrue(new PostgresSlotRepository().findAvailable(connection, 9, from, to).isEmpty());
        }
    }

    @Test
    void readsOnlyFullyContainedSlotsOfRequestedResourceInTimeOrder() throws SQLException {
        insertSlot(3_000_000_000L, 7, from);
        insertSlot(2, 7, from.plusSeconds(3600));
        insertSlot(1, 7, from.plusSeconds(7200));
        insertSlot(4, 7, to);
        insertSlot(5, 8, from);

        List<Slot> slots = service.findAvailable(7, from, to);

        assertEquals(List.of(3_000_000_000L, 2L, 1L), ids(slots));
        assertEquals(7, slots.get(0).getResourceId());
        assertEquals(from, slots.get(0).getStartsAt());
        assertEquals(from.plusSeconds(3600), slots.get(0).getEndsAt());
        assertTrue(repository.lastConnection.isClosed());
        assertEquals(List.of(2L), ids(service.findAvailable(7, from.plusSeconds(1), to.minusSeconds(1))));
        assertEquals(List.of(2L), ids(service.findAvailable(7, from.plusNanos(1), to.minusNanos(1))));
        assertEquals(4, service.findAvailable(7, from, from.plus(Duration.ofDays(7))).size());
    }

    @ParameterizedTest
    @CsvSource({
            "HOLD, false, false",
            "HOLD, true, true",
            "CONFIRMED, false, false",
            "CONFIRMED, true, false",
            "EXPIRED, true, true",
            "CANCELLED, false, true",
            "CANCELLED, true, true"
    })
    void usesStatusAndDatabaseTimeWithoutCleanup(String status, boolean old, boolean available) throws SQLException {
        insertSlot(1, 7, from);
        insertBooking(1, status, old);

        assertEquals(available, !service.findAvailable(7, from, to).isEmpty());
        assertTrue(repository.lastConnection.isClosed());
        try (Connection connection = connections.open(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT status FROM bookings")) {
            assertTrue(rows.next());
            assertEquals(status, rows.getString(1));
            assertFalse(rows.next());
        }
    }

    @Test
    void keepsAdjacentHoursIndependentAndDoesNotDuplicateHistory() throws SQLException {
        insertSlot(1, 7, from);
        insertSlot(2, 7, from.plusSeconds(3600));
        insertBooking(1, "HOLD", false);
        insertBooking(2, "EXPIRED", true);
        insertBooking(2, "CANCELLED", true);
        insertBooking(2, "HOLD", true);

        assertEquals(List.of(2L), ids(service.findAvailable(7, from, to)));
    }

    @Test
    void excludesStartedAndPastSlotsEvenWithoutBookings() throws SQLException {
        insertSlot(1, 7, currentHour.minusSeconds(3600));
        insertSlot(2, 7, currentHour);
        insertSlot(3, 7, currentHour.plusSeconds(3600));

        assertEquals(List.of(3L), ids(service.findAvailable(7, currentHour.minusSeconds(3600), currentHour.plusSeconds(7200))));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveIdBeforeReading(long id) {
        assertThrows(IllegalArgumentException.class, () -> service.findAvailable(id, from, to));
        assertNull(repository.lastConnection);
    }

    @Test
    void rejectsMissingReversedEqualAndExcessiveRanges() {
        assertThrows(IllegalArgumentException.class, () -> service.findAvailable(7, null, to));
        assertThrows(IllegalArgumentException.class, () -> service.findAvailable(7, from, null));
        assertThrows(IllegalArgumentException.class, () -> service.findAvailable(7, to, from));
        assertThrows(IllegalArgumentException.class, () -> service.findAvailable(7, from, from));
        assertThrows(IllegalArgumentException.class, () -> service.findAvailable(7, from, from.plus(Duration.ofDays(7)).plusNanos(1)));
        assertNull(repository.lastConnection);
    }

    @Test
    void closesConnectionAfterRealSqlFailureAndAllowsNextRead() throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE bookings RENAME TO unavailable_bookings");
            try {
                SQLException error = assertThrows(SQLException.class, () -> service.findAvailable(7, from, to));
                assertEquals("42P01", error.getSQLState());
                assertTrue(repository.lastConnection.isClosed());
            } finally {
                statement.executeUpdate("ALTER TABLE unavailable_bookings RENAME TO bookings");
            }
        }
        insertSlot(1, 7, from);
        assertEquals(List.of(1L), ids(service.findAvailable(7, from, to)));
        assertTrue(repository.lastConnection.isClosed());
    }

    @Test
    void leavesTransactionAndConnectionUnderCallerControl() throws SQLException {
        insertSlot(1, 7, from);
        try (Connection connection = connections.open(); var statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.executeUpdate("UPDATE resources SET active = FALSE WHERE id = 7");
            assertTrue(new PostgresSlotRepository().findAvailable(connection, 7, from, to).isEmpty());
            assertFalse(connection.isClosed());
            assertFalse(connection.getAutoCommit());
            connection.rollback();
        }
        assertEquals(List.of(1L), ids(service.findAvailable(7, from, to)));
    }

    private void insertSlot(long id, long resourceId, Instant startsAt) throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.prepareStatement("""
                INSERT INTO slots (id, resource_id, starts_at, ends_at) VALUES (?, ?, ?, ?)
                """)) {
            statement.setLong(1, id);
            statement.setLong(2, resourceId);
            statement.setObject(3, startsAt.atOffset(ZoneOffset.UTC));
            statement.setObject(4, startsAt.plusSeconds(3600).atOffset(ZoneOffset.UTC));
            statement.executeUpdate();
        }
    }

    private void insertBooking(long slotId, String status, boolean old) throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.prepareStatement("""
                WITH data AS (
                    SELECT ?::uuid AS id, ?::bigint AS slot_id, ?::varchar AS status,
                           statement_timestamp() - CASE WHEN ? THEN INTERVAL '20 minutes' ELSE INTERVAL '0' END AS created_at
                )
                INSERT INTO bookings (id, slot_id, status, created_at, expires_at,
                                      customer_name, customer_contact, confirmed_at, cancelled_at)
                SELECT id, slot_id, status, created_at, created_at + INTERVAL '10 minutes',
                       CASE WHEN status = 'CONFIRMED' THEN 'Тест' END,
                       CASE WHEN status = 'CONFIRMED' THEN 'test@example.test' END,
                       CASE WHEN status = 'CONFIRMED' THEN created_at + INTERVAL '1 second' END,
                       CASE WHEN status = 'CANCELLED' THEN created_at + INTERVAL '2 seconds' END
                FROM data
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setLong(2, slotId);
            statement.setString(3, status);
            statement.setBoolean(4, old);
            statement.executeUpdate();
        }
    }

    private List<Long> ids(List<Slot> slots) {
        List<Long> ids = new ArrayList<>();
        for (Slot slot : slots) {
            ids.add(slot.getId());
        }
        return ids;
    }

    private static final class TrackingRepository implements SlotRepository {
        private final SlotRepository delegate = new PostgresSlotRepository();
        private Connection lastConnection;

        @Override
        public List<Slot> findAvailable(Connection connection, long resourceId, Instant from, Instant to) throws SQLException {
            lastConnection = connection;
            return delegate.findAvailable(connection, resourceId, from, to);
        }
    }
}
