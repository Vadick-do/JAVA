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
import ru.coursework.booking.features.resources.repository.postgres.PostgresResourceRepository;
import ru.coursework.booking.features.resources.service.ResourceService;
import ru.coursework.booking.features.resources.transport.http.ResourceHandler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpServerIT {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static ConnectionFactory connections;
    private Javalin server;
    private HttpClient client;

    @BeforeAll
    static void prepareDatabase(@TempDir Path directory) throws Exception {
        assertEquals("booking_coursework_test", System.getenv("DB_NAME"));
        Path file = Files.writeString(directory.resolve("application.properties"), "db.host=postgres\n");
        AppConfig config = AppConfig.load(file, System.getenv());
        connections = new ConnectionFactory(config);
        new MigrationRunner(config).migrate();
    }

    @BeforeEach
    void startServerWithEmptyResources() throws SQLException {
        executeSql("DELETE FROM resources");
        ResourceService service = new ResourceService(connections, new PostgresResourceRepository());
        server = HttpServer.create(new ResourceHandler(service)).start("127.0.0.1", 0);
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
    void returnsHealthAndEmptyJsonList() throws Exception {
        HttpResponse<String> health = get("/health");
        assertJsonStatus(health, 200);
        assertEquals("UP", JSON.readTree(health.body()).get("status").asText());

        HttpResponse<String> resources = get("/api/v1/resources");
        assertJsonStatus(resources, 200);
        assertEquals(JSON.readTree("[]"), JSON.readTree(resources.body()));
    }

    @Test
    void readsOnlyActiveResourcesInIdOrderAndReturnsCard() throws Exception {
        executeSql("""
                INSERT INTO resources (id, name, type, capacity, active) VALUES
                (3000000000, 'Зал "Север"', 'MEETING_ROOM', 12, TRUE),
                (7, 'Стол у окна', 'TABLE', 4, TRUE),
                (8, 'Неактивный стол', 'TABLE', 2, FALSE)
                """);

        HttpResponse<String> response = get("/api/v1/resources");
        assertJsonStatus(response, 200);
        JsonNode resources = JSON.readTree(response.body());
        assertEquals(2, resources.size());
        assertEquals(7, resources.get(0).get("id").asLong());
        assertEquals("Стол у окна", resources.get(0).get("name").asText());
        assertEquals("TABLE", resources.get(0).get("type").asText());
        assertEquals(4, resources.get(0).get("capacity").asInt());
        assertTrue(resources.get(0).get("active").asBoolean());
        assertEquals(3_000_000_000L, resources.get(1).get("id").asLong());
        assertEquals("Зал \"Север\"", resources.get(1).get("name").asText());
        assertEquals("MEETING_ROOM", resources.get(1).get("type").asText());

        HttpResponse<String> card = get("/api/v1/resources/3000000000");
        assertJsonStatus(card, 200);
        assertEquals(resources.get(1), JSON.readTree(card.body()));
        assertEquals(5, JSON.readTree(card.body()).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "1.5", "9223372036854775808"})
    void rejectsInvalidResourceId(String id) throws Exception {
        assertError(get("/api/v1/resources/" + id), 400, "INVALID_REQUEST");
        assertJsonStatus(get("/health"), 200);
    }

    @Test
    void hidesMissingAndInactiveResourcesAndUnknownRoutes() throws Exception {
        executeSql("""
                INSERT INTO resources (id, name, type, capacity, active)
                VALUES (7, 'Неактивный стол', 'TABLE', 2, FALSE)
                """);

        assertError(get("/api/v1/resources/1"), 404, "NOT_FOUND");
        assertError(get("/api/v1/resources/7"), 404, "NOT_FOUND");
        assertError(get("/unknown"), 404, "NOT_FOUND");
        assertEquals(JSON.readTree("[]"), JSON.readTree(get("/api/v1/resources").body()));
    }

    @Test
    void hidesDatabaseFailureAndKeepsHealthIndependentFromDatabase() throws Exception {
        executeSql("ALTER TABLE resources RENAME TO resources_unavailable");
        try {
            assertError(get("/api/v1/resources"), 500, "INTERNAL_ERROR");
            assertError(get("/api/v1/resources/1"), 500, "INTERNAL_ERROR");
            assertJsonStatus(get("/health"), 200);
        } finally {
            executeSql("ALTER TABLE resources_unavailable RENAME TO resources");
        }
        assertJsonStatus(get("/api/v1/resources"), 200);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
                .timeout(Duration.ofSeconds(5)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertJsonStatus(HttpResponse<String> response, int status) {
        assertEquals(status, response.statusCode());
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

    private void executeSql(String sql) throws SQLException {
        try (Connection connection = connections.open(); var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
