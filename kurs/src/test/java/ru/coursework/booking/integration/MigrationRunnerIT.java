package ru.coursework.booking.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.coursework.booking.core.config.AppConfig;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.core.repository.postgres.MigrationRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationRunnerIT {
    private static MigrationRunner migrations;
    private static ConnectionFactory connections;
    private static long resourceId;

    @BeforeAll
    static void migrateEmptyDatabase(@TempDir Path directory) throws Exception {
        assertEquals("booking_coursework_test", System.getenv("DB_NAME"));
        Path file = Files.writeString(directory.resolve("application.properties"), "db.host=postgres\n");
        AppConfig config = AppConfig.load(file, System.getenv());
        migrations = new MigrationRunner(config);
        connections = new ConnectionFactory(config);
        assertEquals(3, migrations.migrate());

        try (var connection = connections.open(); var statement = connection.createStatement();
             var rows = statement.executeQuery("""
                     INSERT INTO resources (name, type, capacity)
                     VALUES ('Тестовый стол', 'TABLE', 4) RETURNING id
                     """)) {
            assertTrue(rows.next());
            resourceId = rows.getLong(1);
        }
    }

    @Test
    void validatesAndKeepsExistingDataOnSecondRun() throws SQLException {
        assertEquals(0, migrations.migrate());

        try (var connection = connections.open(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT name FROM resources WHERE id = " + resourceId)) {
            assertTrue(rows.next());
            assertEquals("Тестовый стол", rows.getString("name"));
        }
    }

    @Test
    void enforcesResourceForeignKeyAndUtcHourGrid() throws SQLException {
        try (var connection = connections.open(); var statement = connection.createStatement()) {
            statement.execute("SET TIME ZONE 'Asia/Kolkata'");

            String onHour = "2030-01-01 10:00:00+00";
            String nextHour = "2030-01-01 11:00:00+00";
            assertSqlState("23503", statement, slotInsert(0, onHour, nextHour));
            assertSqlState("23514", statement, slotInsert(resourceId, "2030-01-01 10:30:00+00", "2030-01-01 11:30:00+00"));
            assertSqlState("23514", statement, slotInsert(resourceId, onHour, "2030-01-01 11:01:00+00"));
            assertEquals(1, statement.executeUpdate(slotInsert(resourceId, onHour, nextHour)));
            assertSqlState("23505", statement, slotInsert(resourceId, onHour, nextHour));
            assertEquals(1, statement.executeUpdate(slotInsert(resourceId, nextHour, "2030-01-01 12:00:00+00")));
        }
    }

    @Test
    void enforcesBookingFieldsAndKeepsOnlyOneActiveBookingPerSlot() throws SQLException {
        try (var connection = connections.open(); var statement = connection.createStatement();
             var rows = statement.executeQuery(slotInsert(resourceId, "2030-01-02 10:00:00+00",
                     "2030-01-02 11:00:00+00") + " RETURNING id")) {
            assertTrue(rows.next());
            long slotId = rows.getLong(1);

            assertSqlState("23503", statement, bookingInsert(UUID.randomUUID(), 0, "HOLD"));
            assertSqlState("23514", statement, bookingInsert(UUID.randomUUID(), slotId, "CONFIRMED"));

            UUID holdId = UUID.randomUUID();
            assertEquals(1, statement.executeUpdate(bookingInsert(holdId, slotId, "HOLD")));
            assertSqlState("23505", statement, bookingInsert(UUID.randomUUID(), slotId, "HOLD"));
            assertEquals(1, statement.executeUpdate("UPDATE bookings SET status = 'EXPIRED' WHERE id = '" + holdId + "'"));
            assertEquals(1, statement.executeUpdate(bookingInsert(UUID.randomUUID(), slotId, "HOLD")));
        }
    }

    private static String slotInsert(long resourceId, String startsAt, String endsAt) {
        return "INSERT INTO slots (resource_id, starts_at, ends_at) VALUES (%d, '%s', '%s')"
                .formatted(resourceId, startsAt, endsAt);
    }

    private static String bookingInsert(UUID id, long slotId, String status) {
        return """
                INSERT INTO bookings (id, slot_id, status, created_at, expires_at)
                VALUES ('%s', %d, '%s', '2030-01-01 09:00:00+00', '2030-01-01 09:10:00+00')
                """.formatted(id, slotId, status);
    }

    private static void assertSqlState(String state, Statement statement, String sql) {
        SQLException error = assertThrows(SQLException.class, () -> statement.executeUpdate(sql));
        assertEquals(state, error.getSQLState());
    }
}
