package ru.coursework.booking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.coursework.booking.core.config.AppConfig;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.core.repository.postgres.MigrationRunner;
import ru.coursework.booking.core.transport.http.HttpServer;
import ru.coursework.booking.features.bookings.repository.postgres.PostgresBookingRepository;
import ru.coursework.booking.features.bookings.service.BookingService;
import ru.coursework.booking.features.bookings.transport.http.BookingHandler;
import ru.coursework.booking.features.resources.repository.postgres.PostgresResourceRepository;
import ru.coursework.booking.features.resources.service.ResourceService;
import ru.coursework.booking.features.resources.transport.http.ResourceHandler;
import ru.coursework.booking.features.slots.repository.postgres.PostgresSlotRepository;
import ru.coursework.booking.features.slots.service.SlotService;
import ru.coursework.booking.features.slots.transport.http.SlotHandler;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotHttpIT {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static ConnectionFactory connections;
    private Javalin server;
    private HttpClient client;
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
    void prepareDataAndServer() throws SQLException {
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
                    (3000000000, 'Стол', 'TABLE', 4, TRUE),
                    (8, 'Зал', 'MEETING_ROOM', 8, TRUE),
                    (9, 'Закрытый зал', 'MEETING_ROOM', 8, FALSE)
                    """);
        }
        from = currentHour.plus(Duration.ofDays(1));
        to = from.plus(Duration.ofHours(3));
        PostgresResourceRepository resources = new PostgresResourceRepository();
        ResourceService resourceService = new ResourceService(connections, resources);
        SlotService slotService = new SlotService(connections, resources, new PostgresSlotRepository());
        BookingService bookings = new BookingService(connections, resources,
                new PostgresSlotRepository(), new PostgresBookingRepository());
        server = HttpServer.create(new ResourceHandler(resourceService), new SlotHandler(slotService), new BookingHandler(bookings))
                .start("127.0.0.1", 0);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @AfterEach
    void stopServer() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void distinguishesEmptyInactiveAndMissingResources() throws Exception {
        assertEquals(JSON.readTree("[]"), readSlots("3000000000", from, to));
        insertSlot(1, 9, from);
        assertEquals(JSON.readTree("[]"), readSlots("9", from, to));
        assertError(get(slotsPath("10", from.toString(), to.toString())), 404, "NOT_FOUND");
        assertError(get(slotsPath("9223372036854775807", from.toString(), to.toString())), 404, "NOT_FOUND");
    }

    @Test
    void returnsOrderedFullyContainedSlotsWithUtcStringsAndLongIds() throws Exception {
        insertSlot(3_000_000_000L, 3_000_000_000L, from);
        insertSlot(2, 3_000_000_000L, from.plusSeconds(3600));
        insertSlot(1, 3_000_000_000L, from.plusSeconds(7200));
        insertSlot(4, 3_000_000_000L, to);
        insertSlot(5, 8, from);

        JsonNode slots = readSlots("3000000000", from, to);

        assertEquals(3, slots.size());
        assertEquals(3_000_000_000L, slots.get(0).get("id").asLong());
        assertEquals(2, slots.get(1).get("id").asLong());
        assertEquals(1, slots.get(2).get("id").asLong());
        assertEquals(4, slots.get(0).size());
        assertEquals(3_000_000_000L, slots.get(0).get("resourceId").asLong());
        assertTrue(slots.get(0).get("startsAt").isTextual());
        assertEquals(from.toString(), slots.get(0).get("startsAt").asText());
        assertEquals(from.plusSeconds(3600).toString(), slots.get(0).get("endsAt").asText());
        JsonNode inner = readSlots("3000000000", from.plusNanos(1), to.minusNanos(1));
        assertEquals(1, inner.size());
        assertEquals(2, inner.get(0).get("id").asLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Z", "+03:00", "-05:00", "+05:30"})
    void treatsEquivalentOffsetsAsSameInstants(String offset) throws Exception {
        insertSlot(1, 3_000_000_000L, from);
        String start = from.atOffset(ZoneOffset.of(offset)).toString();
        String end = to.atOffset(ZoneOffset.of(offset)).toString();
        HttpResponse<String> response = get(slotsPath("3000000000", start, end));
        assertJsonStatus(response, 200);
        assertEquals(readSlots("3000000000", from, to), JSON.readTree(response.body()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "1.5", "9223372036854775808"})
    void rejectsInvalidResourceIds(String id) throws Exception {
        assertError(get(slotsPath(id, from.toString(), to.toString())), 400, "INVALID_REQUEST");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "?from=2030-01-01T00:00:00Z", "?to=2030-01-02T00:00:00Z",
            "?from=&to=2030-01-02T00:00:00Z", "?from=2030-01-01T00:00:00Z&to=",
            "?from=2030-01-01T00:00:00&to=2030-01-02T00:00:00Z",
            "?from=2030-01-01T00:00:00Z&to=2030-01-02T00:00:00",
            "?from=2030-02-30T00:00:00Z&to=2030-03-02T00:00:00Z",
            "?from=tomorrow&to=2030-01-02T00:00:00Z",
            "?from=2030-01-01T00:00:00Z&to=invalid",
            "?from=2030-01-01T00:00:00Z&from=2030-01-01T01:00:00Z&to=2030-01-02T00:00:00Z",
            "?from=2030-01-01T00:00:00Z&to=2030-01-02T00:00:00Z&to=2030-01-03T00:00:00Z"
    })
    void rejectsMissingMalformedAndDuplicateBoundaries(String query) throws Exception {
        assertError(get("/api/v1/resources/3000000000/slots" + query), 400, "INVALID_REQUEST");
        assertJsonStatus(get("/health"), 200);
    }

    @Test
    void validatesRangeAfterConvertingOffsetsAndAllowsExactlySevenDays() throws Exception {
        insertSlot(1, 3_000_000_000L, from);
        assertEquals(1, readSlots("3000000000", from, from.plus(Duration.ofDays(7))).size());
        assertError(get(slotsPath("3000000000", from.toString(), from.toString())), 400, "INVALID_REQUEST");
        assertError(get(slotsPath("3000000000", to.toString(), from.toString())), 400, "INVALID_REQUEST");
        assertError(get(slotsPath("3000000000", from.toString(),
                from.plus(Duration.ofDays(7)).plusNanos(1).toString())), 400, "INVALID_REQUEST");
        String sameMoment = from.atOffset(ZoneOffset.ofHours(3)).toString();
        assertError(get(slotsPath("3000000000", sameMoment, from.toString())), 400, "INVALID_REQUEST");
        String earlierMoment = from.minusSeconds(3600).atOffset(ZoneOffset.ofHours(3)).toString();
        assertError(get(slotsPath("3000000000", from.toString(), earlierMoment)), 400, "INVALID_REQUEST");
    }

    @Test
    void reflectsBookingStatesAndExcludesStartedSlotsWithoutCleanup() throws Exception {
        insertSlot(1, 3_000_000_000L, from);
        insertSlot(2, 3_000_000_000L, from.plusSeconds(3600));
        insertSlot(3, 3_000_000_000L, from.plusSeconds(7200));
        insertSlot(4, 3_000_000_000L, currentHour);
        insertBooking(1, "HOLD", false);
        insertBooking(2, "CONFIRMED", true);
        insertBooking(3, "HOLD", true);

        JsonNode slots = readSlots("3000000000", currentHour, to);

        assertEquals(1, slots.size());
        assertEquals(3, slots.get(0).get("id").asLong());
    }

    @Test
    void returnsSafeSqlErrorAndRecoversAfterDatabaseRestoration() throws Exception {
        executeSql("ALTER TABLE slots RENAME TO slots_unavailable");
        try {
            assertError(get(slotsPath("3000000000", from.toString(), to.toString())), 500, "INTERNAL_ERROR");
            assertError(get(slotsPath("3000000000", from.toString(), from.toString())), 400, "INVALID_REQUEST");
            assertError(get("/api/v1/resources/3000000000/slots"), 400, "INVALID_REQUEST");
            assertJsonStatus(get("/health"), 200);
        } finally {
            executeSql("ALTER TABLE slots_unavailable RENAME TO slots");
        }
        insertSlot(1, 3_000_000_000L, from);
        assertEquals(1, readSlots("3000000000", from, to).size());
    }

    private JsonNode readSlots(String id, Instant start, Instant end) throws Exception {
        HttpResponse<String> response = get(slotsPath(id, start.toString(), end.toString()));
        assertJsonStatus(response, 200);
        JsonNode slots = JSON.readTree(response.body());
        assertTrue(slots.isArray());
        return slots;
    }

    private String slotsPath(String id, String start, String end) {
        return "/api/v1/resources/" + id + "/slots?from=" + URLEncoder.encode(start, StandardCharsets.UTF_8)
                + "&to=" + URLEncoder.encode(end, StandardCharsets.UTF_8);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
                .timeout(Duration.ofSeconds(5)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertJsonStatus(HttpResponse<String> response, int status) {
        assertEquals(status, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
    }

    private void assertError(HttpResponse<String> response, int status, String code) throws Exception {
        assertJsonStatus(response, status);
        JsonNode error = JSON.readTree(response.body());
        assertEquals(2, error.size());
        assertEquals(code, error.get("code").asText());
        String message = switch (status) {
            case 400 -> "Проверьте параметры запроса.";
            case 404 -> "Ресурс или маршрут не найден.";
            default -> "Внутренняя ошибка сервера.";
        };
        assertEquals(message, error.get("message").asText());
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
                                      customer_name, customer_contact, confirmed_at)
                SELECT id, slot_id, status, created_at, created_at + INTERVAL '10 minutes',
                       CASE WHEN status = 'CONFIRMED' THEN 'Тест' END,
                       CASE WHEN status = 'CONFIRMED' THEN 'test@example.test' END,
                       CASE WHEN status = 'CONFIRMED' THEN created_at + INTERVAL '1 second' END
                FROM data
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setLong(2, slotId);
            statement.setString(3, status);
            statement.setBoolean(4, old);
            statement.executeUpdate();
        }
    }

    private void executeSql(String sql) throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
