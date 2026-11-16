package ru.coursework.booking.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.coursework.booking.core.config.AppConfig;
import ru.coursework.booking.core.domain.Booking;
import ru.coursework.booking.core.domain.BookingStatus;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.core.repository.postgres.MigrationRunner;
import ru.coursework.booking.features.bookings.repository.BookingRepository;
import ru.coursework.booking.features.bookings.repository.postgres.PostgresBookingRepository;
import ru.coursework.booking.features.bookings.service.BookingService;
import ru.coursework.booking.features.resources.repository.postgres.PostgresResourceRepository;
import ru.coursework.booking.features.slots.repository.postgres.PostgresSlotRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(20)
class BookingServiceIT {
    private static ConnectionFactory connections;
    private static BookingService service;
    private final BookingRepository bookings = new PostgresBookingRepository();

    @BeforeAll
    static void prepareDatabase(@TempDir Path directory) throws Exception {
        assertEquals("booking_coursework_test", System.getenv("DB_NAME"));
        Path file = Files.writeString(directory.resolve("application.properties"), "db.host=postgres\n");
        AppConfig config = AppConfig.load(file, System.getenv());
        connections = new ConnectionFactory(config);
        new MigrationRunner(config).migrate();
        service = new BookingService(connections, new PostgresResourceRepository(),
                new PostgresSlotRepository(), new PostgresBookingRepository());
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
                    SELECT id, CASE WHEN id = 3 THEN 9 ELSE 7 END,
                           date_trunc('hour', statement_timestamp(), 'UTC') + days * INTERVAL '1 day',
                           date_trunc('hour', statement_timestamp(), 'UTC') + days * INTERVAL '1 day' + INTERVAL '1 hour'
                    FROM (VALUES (1, 1), (2, 2), (3, 1), (4, 0), (5, -1)) AS data(id, days)
                    """);
        }
    }

    @Test
    void commitsHoldWithDatabaseTimeAndTenMinuteTtl() throws SQLException {
        Instant before;
        try (Connection connection = connections.open()) {
            before = bookings.currentTime(connection);
        }
        Booking booking = service.createHold(1);
        assertEquals(1, booking.getSlotId());
        assertEquals(BookingStatus.HOLD, booking.getStatus());
        assertEquals(Duration.ofMinutes(10), Duration.between(booking.getCreatedAt(), booking.getExpiresAt()));
        assertFalse(booking.getCreatedAt().isBefore(before));
        assertNull(booking.getCustomerName());
        assertNull(booking.getCustomerContact());
        assertNull(booking.getConfirmedAt());
        assertNull(booking.getCancelledAt());
        try (Connection connection = connections.open(); var statement = connection.prepareStatement(
                "SELECT * FROM bookings WHERE id = ?")) {
            assertFalse(booking.getCreatedAt().isAfter(bookings.currentTime(connection)));
            statement.setObject(1, booking.getId());
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(1, rows.getLong("slot_id"));
                assertEquals("HOLD", rows.getString("status"));
                assertEquals(booking.getCreatedAt(), rows.getObject("created_at", OffsetDateTime.class).toInstant());
                assertEquals(booking.getExpiresAt(), rows.getObject("expires_at", OffsetDateTime.class).toInstant());
                assertNull(rows.getString("customer_name"));
                assertNull(rows.getString("customer_contact"));
                assertNull(rows.getObject("confirmed_at"));
                assertNull(rows.getObject("cancelled_at"));
                assertFalse(rows.next());
            }
        }
        assertEquals(1, countBookings());
    }

    @ParameterizedTest
    @ValueSource(strings = {"HOLD", "CONFIRMED"})
    void rejectsOccupiedSlotWithoutChangingBooking(String status) throws SQLException {
        UUID oldId;
        try (Connection connection = connections.open()) {
            oldId = insertBooking(connection, 1, status, bookings.currentTime(connection).minusSeconds(60));
        }
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.createHold(1));
        assertEquals("Слот уже занят.", error.getMessage());
        assertEquals(status, readStatus(oldId));
        assertEquals(1, countBookings());
        assertEquals(2, service.createHold(2).getSlotId());
    }

    @Test
    void expiresOldHoldAndCreatesNewUuidWithoutCleanup() throws SQLException {
        UUID oldId;
        UUID otherId;
        try (Connection connection = connections.open()) {
            Instant oldTime = bookings.currentTime(connection).minusSeconds(601);
            oldId = insertBooking(connection, 1, "HOLD", oldTime);
            otherId = insertBooking(connection, 2, "HOLD", oldTime);
        }
        Booking booking = service.createHold(1);
        assertNotEquals(oldId, booking.getId());
        assertEquals("EXPIRED", readStatus(oldId));
        assertEquals("HOLD", readStatus(otherId));
        assertEquals("HOLD", readStatus(booking.getId()));
        assertEquals(3, countBookings());
        assertThrows(IllegalStateException.class, () -> service.createHold(1));
        assertEquals(3, countBookings());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CANCELLED", "EXPIRED"})
    void keepsFinalHistoryWhenSlotIsReused(String status) throws SQLException {
        UUID oldId;
        try (Connection connection = connections.open()) {
            oldId = insertBooking(connection, 1, status, bookings.currentTime(connection).minusSeconds(601));
        }
        Booking booking = service.createHold(1);
        assertNotEquals(oldId, booking.getId());
        assertEquals(status, readStatus(oldId));
        assertEquals("HOLD", readStatus(booking.getId()));
        assertEquals(2, countBookings());
    }

    @Test
    void rejectsMissingSlotAndLeavesConnectionUsable() throws SQLException {
        assertThrows(NoSuchElementException.class, () -> service.createHold(Long.MAX_VALUE));
        assertEquals(0, countBookings());
        assertEquals(1, service.createHold(1).getSlotId());
    }

    @ParameterizedTest
    @CsvSource({"3, Ресурс неактивен.", "4, Начавшийся слот нельзя удержать.",
            "5, Начавшийся слот нельзя удержать."})
    void rejectsInactiveResourceAndStartedSlots(long slotId, String message) throws SQLException {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.createHold(slotId));
        assertEquals(message, error.getMessage());
        assertEquals(0, countBookings());
        assertEquals(1, service.createHold(1).getSlotId());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void validatesIdBeforeOpeningConnection(long slotId) {
        BookingService disconnected = new BookingService(null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> disconnected.createHold(slotId));
    }

    @Test
    void rollsBackExpirationAndInsertAfterRealSqlFailure() throws SQLException {
        UUID oldId;
        try (Connection connection = connections.open(); var statement = connection.createStatement()) {
            oldId = insertBooking(connection, 1, "HOLD", bookings.currentTime(connection).minusSeconds(601));
            statement.execute("""
                    CREATE FUNCTION fail_hold_insert() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN
                        IF NOT EXISTS (SELECT 1 FROM bookings WHERE slot_id = NEW.slot_id AND status = 'EXPIRED') THEN
                            RAISE EXCEPTION 'Expiration was not applied' USING ERRCODE = 'P0002';
                        END IF;
                        RAISE EXCEPTION 'Test insert failure' USING ERRCODE = 'P0001';
                    END;
                    $$
                    """);
            try {
                statement.execute("""
                        CREATE TRIGGER fail_hold_insert AFTER INSERT ON bookings
                        FOR EACH ROW EXECUTE FUNCTION fail_hold_insert()
                        """);
                SQLException error = assertThrows(SQLException.class, () -> service.createHold(1));
                assertEquals("P0001", error.getSQLState());
                assertEquals("HOLD", readStatus(oldId));
                assertEquals(1, countBookings());
            } finally {
                statement.execute("DROP TRIGGER IF EXISTS fail_hold_insert ON bookings");
                statement.execute("DROP FUNCTION fail_hold_insert()");
            }
        }
        Booking recovered = service.createHold(1);
        assertEquals("EXPIRED", readStatus(oldId));
        assertEquals("HOLD", readStatus(recovered.getId()));
        assertEquals(2, countBookings());
    }

    @ParameterizedTest
    @CsvSource({"1, -1, EXPIRED", "1, 0, EXPIRED", "1, 1, HOLD", "4, 60, EXPIRED"})
    void expirationUsesInclusiveTtlAndSlotStart(long slotId, int expiresInSeconds, String expected) throws SQLException {
        try (Connection connection = connections.open()) {
            Instant now = bookings.currentTime(connection);
            UUID id = insertBooking(connection, slotId, "HOLD", now.plusSeconds(expiresInSeconds).minusSeconds(600));
            connection.setAutoCommit(false);
            try {
                new PostgresSlotRepository().lockById(connection, slotId).orElseThrow();
                bookings.expireHolds(connection, slotId, now);
                connection.commit();
            } finally {
                connection.rollback();
            }
            assertEquals(expected, readStatus(id));
        }
    }

    private UUID insertBooking(Connection connection, long slotId, String status, Instant createdAt) throws SQLException {
        UUID id = UUID.randomUUID();
        try (var statement = connection.prepareStatement("""
                WITH data AS (SELECT ?::uuid AS id, ?::bigint AS slot_id, ?::varchar AS status, ?::timestamptz AS created_at)
                INSERT INTO bookings (id, slot_id, status, created_at, expires_at,
                                      customer_name, customer_contact, confirmed_at, cancelled_at)
                SELECT id, slot_id, status, created_at, created_at + INTERVAL '10 minutes',
                       CASE WHEN status = 'CONFIRMED' THEN 'Тест' END,
                       CASE WHEN status = 'CONFIRMED' THEN 'test@example.test' END,
                       CASE WHEN status = 'CONFIRMED' THEN created_at + INTERVAL '1 second' END,
                       CASE WHEN status = 'CANCELLED' THEN created_at + INTERVAL '2 seconds' END
                FROM data
                """)) {
            statement.setObject(1, id);
            statement.setLong(2, slotId);
            statement.setString(3, status);
            statement.setObject(4, createdAt.atOffset(ZoneOffset.UTC));
            statement.executeUpdate();
        }
        return id;
    }

    private String readStatus(UUID id) throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.prepareStatement(
                "SELECT status FROM bookings WHERE id = ?")) {
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next());
                return rows.getString(1);
            }
        }
    }

    private long countBookings() throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT count(*) FROM bookings")) {
            assertTrue(rows.next());
            return rows.getLong(1);
        }
    }
}
