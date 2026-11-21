package ru.coursework.booking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.coursework.booking.core.config.AppConfig;
import ru.coursework.booking.core.domain.BookingStatus;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(20)
class BookingHttpIT {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CUSTOMER = "{\"customerName\":\"Анна\",\"customerContact\":\"anna@example.test\"}";
    private static ConnectionFactory connections;
    private Javalin server;
    private HttpClient client;
    private Instant futureHour;

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
                futureHour = rows.getObject(2, OffsetDateTime.class).toInstant().plusSeconds(86400);
            }
            statement.executeUpdate("DELETE FROM bookings");
            statement.executeUpdate("DELETE FROM slots");
            statement.executeUpdate("DELETE FROM resources");
            statement.executeUpdate("""
                    INSERT INTO resources (id, name, type, capacity, active) VALUES
                    (1, 'Стол', 'TABLE', 4, TRUE), (2, 'Закрытый зал', 'MEETING_ROOM', 8, FALSE)
                    """);
        }
        insertSlot(3_000_000_000L, 1, futureHour);
        insertSlot(2, 2, futureHour);
        insertSlot(3, 1, futureHour.minusSeconds(86400));
        startServer();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    private void startServer() {
        PostgresResourceRepository resources = new PostgresResourceRepository();
        PostgresSlotRepository slots = new PostgresSlotRepository();
        BookingService bookings = new BookingService(connections, resources, slots, new PostgresBookingRepository());
        server = HttpServer.create(new ResourceHandler(new ResourceService(connections, resources)),
                new SlotHandler(new SlotService(connections, resources, slots)), new BookingHandler(bookings))
                .start("127.0.0.1", 0);
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
    void createsPersistsAndReadsSameHoldAfterServerRestartWithoutExtendingTtl() throws Exception {
        HttpResponse<String> response = post("{\"slotId\":3000000000}");
        assertJsonStatus(response, 201);
        JsonNode booking = JSON.readTree(response.body());
        UUID id = UUID.fromString(booking.get("id").asText());
        assertEquals(9, booking.size());
        assertEquals(3_000_000_000L, booking.get("slotId").asLong());
        assertEquals("HOLD", booking.get("status").asText());
        Instant createdAt = Instant.parse(booking.get("createdAt").asText());
        Instant expiresAt = Instant.parse(booking.get("expiresAt").asText());
        assertEquals(Duration.ofMinutes(10), Duration.between(createdAt, expiresAt));
        assertTrue(booking.get("createdAt").asText().endsWith("Z"));
        assertTrue(booking.get("expiresAt").asText().endsWith("Z"));
        assertTrue(booking.get("customerName").isNull());
        assertTrue(booking.get("customerContact").isNull());
        assertTrue(booking.get("confirmedAt").isNull());
        assertTrue(booking.get("cancelledAt").isNull());
        try (Connection connection = connections.open(); var statement = connection.prepareStatement("SELECT * FROM bookings WHERE id = ?")) {
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals("HOLD", rows.getString("status"));
                assertEquals(createdAt, rows.getObject("created_at", OffsetDateTime.class).toInstant());
                assertEquals(expiresAt, rows.getObject("expires_at", OffsetDateTime.class).toInstant());
                assertTrue(!rows.next());
            }
        }
        server.stop();
        startServer();
        assertEquals(booking, readBooking(id));
        assertEquals(booking, readBooking(id));
        assertEquals(booking, JSON.readTree(get("/api/v1/bookings/" + id.toString().toUpperCase()).body()));
        assertError(post("{\"slotId\":3000000000}"), 409, "BOOKING_CONFLICT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "null", "[]", "1", "{}", "{\"slotId\":null}",
            "{\"slotId\":0}", "{\"slotId\":-1}", "{\"slotId\":1.5}", "{\"slotId\":1.0}",
            "{\"slotId\":\"3000000000\"}", "{\"slotId\":true}", "{\"slotId\":[]}",
            "{\"slotId\":9223372036854775808}", "{\"slotId\":3000000000,\"customerName\":\"Анна\"}",
            "{\"slotId\":3000000000,\"slotId\":2}", "{\"slotId\":3000000000} {}"})
    void rejectsMalformedBodiesBeforeCreatingAnyHold(String body) throws Exception {
        assertError(post(body), 400, "INVALID_REQUEST");
        assertJsonStatus(post("{\"slotId\":3000000000}"), 201);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "1-1-1-1-1", "6dc3cb7377df435f87e8c19313c3c00d", "zzc3cb73-77df-435f-87e8-c19313c3c00d"})
    void rejectsMalformedUuids(String id) throws Exception {
        assertError(get("/api/v1/bookings/" + id), 400, "INVALID_REQUEST");
        assertJsonStatus(get("/health"), 200);
    }

    @Test
    void distinguishesMissingSlotAndBookingFromInactiveAndStartedSlot() throws Exception {
        assertError(post("{\"slotId\":9223372036854775807}"), 404, "NOT_FOUND");
        assertError(get("/api/v1/bookings/" + UUID.randomUUID()), 404, "NOT_FOUND");
        assertError(post("{\"slotId\":2}"), 409, "RESOURCE_INACTIVE");
        assertError(post("{\"slotId\":3}"), 409, "BOOKING_CONFLICT");
        assertJsonStatus(post("{\"slotId\":3000000000}"), 201);
    }

    @Test
    void showsLogicalExpirationWithoutWritingItAndAllowsNewHoldWithDifferentUuid() throws Exception {
        UUID oldId = insertBooking(BookingStatus.HOLD);
        JsonNode expired = readBooking(oldId);
        assertEquals("EXPIRED", expired.get("status").asText());
        assertEquals(expired, readBooking(oldId));
        assertStoredStatus(oldId, "HOLD");
        HttpResponse<String> response = post("{\"slotId\":3000000000}");
        assertJsonStatus(response, 201);
        UUID newId = UUID.fromString(JSON.readTree(response.body()).get("id").asText());
        assertTrue(!oldId.equals(newId));
        assertStoredStatus(oldId, "EXPIRED");
        assertEquals(expired, readBooking(oldId));
        assertEquals("HOLD", readBooking(newId).get("status").asText());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"CONFIRMED", "CANCELLED", "EXPIRED"})
    void readsSavedHistoryAfterResourceDeactivation(BookingStatus status) throws Exception {
        UUID id = insertBooking(status);
        if (status == BookingStatus.CONFIRMED) {
            assertError(post("{\"slotId\":3000000000}"), 409, "BOOKING_CONFLICT");
        }
        executeSql("UPDATE resources SET active = FALSE WHERE id = 1");
        JsonNode booking = readBooking(id);
        assertEquals(status.name(), booking.get("status").asText());
        assertStoredStatus(id, status.name());
        if (status == BookingStatus.CONFIRMED || status == BookingStatus.CANCELLED) {
            assertEquals("Анна", booking.get("customerName").asText());
            assertEquals("anna@example.test", booking.get("customerContact").asText());
            assertTrue(booking.get("confirmedAt").isTextual());
        }
        assertEquals(status == BookingStatus.CANCELLED, booking.get("cancelledAt").isTextual());
        assertError(post("{\"slotId\":3000000000}"), 409, "RESOURCE_INACTIVE");
    }

    @Test
    void confirmsOnceWithNormalizedDetailsAndPreservesTheOriginalTimes() throws Exception {
        HttpResponse<String> created = post("{\"slotId\":3000000000}");
        assertJsonStatus(created, 201);
        JsonNode hold = JSON.readTree(created.body());
        UUID id = UUID.fromString(hold.get("id").asText());
        executeSql("UPDATE resources SET active = FALSE WHERE id = 1");

        HttpResponse<String> response = confirm(id, "{\"customerName\":\"  Анна  \","
                + "\"customerContact\":\" anna@example.test \"}");
        assertJsonStatus(response, 200);
        JsonNode booking = JSON.readTree(response.body());
        assertEquals("CONFIRMED", booking.get("status").asText());
        assertEquals("Анна", booking.get("customerName").asText());
        assertEquals("anna@example.test", booking.get("customerContact").asText());
        for (String field : new String[]{"id", "slotId", "createdAt", "expiresAt", "cancelledAt"}) {
            assertEquals(hold.get(field), booking.get(field));
        }
        Instant confirmedAt = Instant.parse(booking.get("confirmedAt").asText());
        assertTrue(!confirmedAt.isBefore(Instant.parse(hold.get("createdAt").asText())));
        assertTrue(confirmedAt.isBefore(Instant.parse(hold.get("expiresAt").asText())));
        assertStoredStatus(id, "CONFIRMED");
        assertEquals(booking, readBooking(id));

        HttpResponse<String> repeated = confirm(id, CUSTOMER);
        assertJsonStatus(repeated, 200);
        assertEquals(booking, JSON.readTree(repeated.body()));
        assertConfirmationConflict(confirm(id, CUSTOMER.replace("anna@", "new@")));
        assertEquals(booking, readBooking(id));
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"HOLD", "CANCELLED", "EXPIRED"})
    void rejectsExpiredAndTerminalBookingsAndPersistsExpiration(BookingStatus status) throws Exception {
        UUID id = insertBooking(status);
        assertStoredStatus(id, status.name());
        assertConfirmationConflict(confirm(id, CUSTOMER));
        assertStoredStatus(id, status == BookingStatus.HOLD ? "EXPIRED" : status.name());
    }

    @Test
    void rejectsMissingBookingAndInvalidConfirmationInput() throws Exception {
        assertError(send("/api/v1/bookings/1-1-1-1-1/confirm", CUSTOMER), 400, "INVALID_REQUEST");
        assertError(confirm(UUID.randomUUID(), CUSTOMER), 404, "NOT_FOUND");
        UUID id = insertBooking(BookingStatus.HOLD);
        for (String body : new String[]{"", "{}", "{\"customerName\":3,\"customerContact\":\"контакт\"}",
                CUSTOMER.replace("Анна", " "), CUSTOMER.replace("Анна", "я".repeat(101)),
                CUSTOMER.replace("anna@example.test", "я".repeat(201)),
                CUSTOMER.replace("Анна", "\\u0000"), CUSTOMER + " {}",
                CUSTOMER.replace("}", ",\"customerName\":\"Анна\"}"),
                CUSTOMER.replace("}", ",\"slotId\":1}")}) {
            assertError(confirm(id, body), 400, "INVALID_REQUEST");
        }
        assertStoredStatus(id, "HOLD");
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"HOLD", "CONFIRMED"})
    void cancelsActiveBookingAndAllowsSlotReuse(BookingStatus initial) throws Exception {
        String body = "{\"slotId\":3000000000}";
        String availableSlots = "/api/v1/resources/1/slots?from=" + futureHour
                + "&to=" + futureHour.plusSeconds(3600);
        HttpResponse<String> created = post(body);
        assertJsonStatus(created, 201);
        JsonNode active = JSON.readTree(created.body());
        UUID id = UUID.fromString(active.get("id").asText());
        if (initial == BookingStatus.CONFIRMED) {
            HttpResponse<String> confirmed = confirm(id, CUSTOMER);
            assertJsonStatus(confirmed, 200);
            active = JSON.readTree(confirmed.body());
        }
        assertEquals(initial.name(), active.get("status").asText());
        HttpResponse<String> unavailable = get(availableSlots);
        assertJsonStatus(unavailable, 200);
        assertEquals("[]", unavailable.body());

        HttpResponse<String> response = cancel(id.toString());
        assertJsonStatus(response, 200);
        JsonNode cancelled = JSON.readTree(response.body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        for (String field : new String[]{"id", "slotId", "createdAt", "expiresAt",
                "customerName", "customerContact", "confirmedAt"}) {
            assertEquals(active.get(field), cancelled.get(field));
        }
        Instant cancelledAt = Instant.parse(cancelled.get("cancelledAt").asText());
        String previousTime = initial == BookingStatus.HOLD ? "createdAt" : "confirmedAt";
        assertTrue(!cancelledAt.isBefore(Instant.parse(active.get(previousTime).asText())));
        assertStoredStatus(id, "CANCELLED");
        assertEquals(cancelled, readBooking(id));
        assertConfirmationConflict(confirm(id, CUSTOMER));

        HttpResponse<String> available = get(availableSlots);
        assertJsonStatus(available, 200);
        JsonNode slots = JSON.readTree(available.body());
        assertEquals(1, slots.size());
        assertEquals(active.get("slotId"), slots.get(0).get("id"));
        HttpResponse<String> replacement = post(body);
        assertJsonStatus(replacement, 201);
        UUID replacementId = UUID.fromString(JSON.readTree(replacement.body()).get("id").asText());
        assertNotEquals(id, replacementId);

        HttpResponse<String> repeated = cancel(id.toString());
        assertJsonStatus(repeated, 200);
        assertEquals(cancelled, JSON.readTree(repeated.body()));
        assertStoredStatus(replacementId, "HOLD");
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"HOLD", "EXPIRED"})
    void returnsAndPersistsExpiredWithoutRecordingCancellation(BookingStatus status) throws Exception {
        UUID id = insertBooking(status);
        assertStoredStatus(id, status.name());
        JsonNode before = readBooking(id);
        HttpResponse<String> response = cancel(id.toString());
        assertJsonStatus(response, 200);
        assertEquals(before, JSON.readTree(response.body()));
        assertEquals("EXPIRED", before.get("status").asText());
        assertTrue(before.get("cancelledAt").isNull());
        assertStoredStatus(id, "EXPIRED");
        assertEquals(before, readBooking(id));
    }

    @Test
    void rejectsMissingBookingAndInvalidCancellationId() throws Exception {
        assertError(cancel("1-1-1-1-1"), 400, "INVALID_REQUEST");
        assertError(cancel(UUID.randomUUID().toString()), 404, "NOT_FOUND");
    }

    @Test
    void hidesSqlFailureForBookingRoutesAndRecoversAfterRestoration() throws Exception {
        UUID id = insertBooking(BookingStatus.EXPIRED);
        executeSql("ALTER TABLE bookings RENAME TO bookings_unavailable");
        try {
            assertError(post("{\"slotId\":3000000000}"), 500, "INTERNAL_ERROR");
            assertError(get("/api/v1/bookings/" + id), 500, "INTERNAL_ERROR");
            assertError(confirm(id, CUSTOMER), 500, "INTERNAL_ERROR");
            assertError(cancel(id.toString()), 500, "INTERNAL_ERROR");
            assertError(post("{\"slotId\":0}"), 400, "INVALID_REQUEST");
            assertError(get("/api/v1/bookings/1-1-1-1-1"), 400, "INVALID_REQUEST");
            assertJsonStatus(get("/health"), 200);
        } finally {
            executeSql("ALTER TABLE bookings_unavailable RENAME TO bookings");
        }
        assertEquals("EXPIRED", readBooking(id).get("status").asText());
        assertJsonStatus(post("{\"slotId\":3000000000}"), 201);
    }

    private JsonNode readBooking(UUID id) throws Exception {
        HttpResponse<String> response = get("/api/v1/bookings/" + id);
        assertJsonStatus(response, 200);
        return JSON.readTree(response.body());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send(path, null);
    }

    private HttpResponse<String> post(String body) throws Exception {
        return send("/api/v1/holds", body);
    }

    private HttpResponse<String> confirm(UUID id, String body) throws Exception {
        return send("/api/v1/bookings/" + id + "/confirm", body);
    }

    private HttpResponse<String> cancel(String id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://127.0.0.1:" + server.port() + "/api/v1/bookings/" + id + "/cancel"))
                .timeout(Duration.ofSeconds(7))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> send(String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
                .timeout(Duration.ofSeconds(7));
        if (body == null) {
            request.GET();
        } else {
            request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
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
            case 409 -> code.equals("RESOURCE_INACTIVE") ? "Ресурс неактивен." : "Слот недоступен для создания холда.";
            default -> "Внутренняя ошибка сервера.";
        };
        assertEquals(message, error.get("message").asText());
    }

    private void assertConfirmationConflict(HttpResponse<String> response) throws Exception {
        assertJsonStatus(response, 409);
        JsonNode error = JSON.readTree(response.body());
        assertEquals(2, error.size());
        assertEquals("BOOKING_CONFLICT", error.get("code").asText());
        assertEquals("Подтверждение брони невозможно.", error.get("message").asText());
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

    private UUID insertBooking(BookingStatus status) throws SQLException {
        UUID id = UUID.randomUUID();
        try (Connection connection = connections.open(); var statement = connection.prepareStatement("""
                WITH data AS (
                    SELECT ?::uuid AS id, ?::varchar AS status, statement_timestamp() - INTERVAL '20 minutes' AS created_at
                )
                INSERT INTO bookings (id, slot_id, status, created_at, expires_at,
                                      customer_name, customer_contact, confirmed_at, cancelled_at)
                SELECT id, 3000000000, status, created_at, created_at + INTERVAL '10 minutes',
                       CASE WHEN status IN ('CONFIRMED', 'CANCELLED') THEN 'Анна' END,
                       CASE WHEN status IN ('CONFIRMED', 'CANCELLED') THEN 'anna@example.test' END,
                       CASE WHEN status IN ('CONFIRMED', 'CANCELLED') THEN created_at + INTERVAL '1 second' END,
                       CASE WHEN status = 'CANCELLED' THEN created_at + INTERVAL '2 seconds' END
                FROM data
                """)) {
            statement.setObject(1, id);
            statement.setString(2, status.name());
            statement.executeUpdate();
        }
        return id;
    }

    private void assertStoredStatus(UUID id, String status) throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.prepareStatement("SELECT status FROM bookings WHERE id = ?")) {
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(status, rows.getString(1));
            }
        }
    }

    private void executeSql(String sql) throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
