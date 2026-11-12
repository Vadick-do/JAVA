package ru.coursework.booking.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.coursework.booking.core.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {
    private static final String FILE_SETTINGS = """
            db.host=postgres
            db.name=бронь & тест
            db.user=file-user
            db.password=file-password
            """;

    @TempDir
    Path directory;

    @Test
    void readsUtf8SettingsAndUsesDefaultPort() throws IOException {
        Path file = Files.writeString(directory.resolve("application.properties"), FILE_SETTINGS);

        AppConfig config = AppConfig.load(file, Map.of());

        assertEquals("jdbc:postgresql://postgres:5432/%D0%B1%D1%80%D0%BE%D0%BD%D1%8C+%26+%D1%82%D0%B5%D1%81%D1%82",
                config.getJdbcUrl());
        assertEquals("file-user", config.getDatabaseUser());
        assertEquals("file-password", config.getDatabasePassword());
        assertEquals("0.0.0.0", config.getHttpHost());
        assertEquals(8080, config.getHttpPort());
    }

    @Test
    void environmentOverridesFileSettings() throws IOException {
        Path file = Files.writeString(directory.resolve("application.properties"), FILE_SETTINGS + "db.port=bad\n");
        Map<String, String> environment = Map.of(
                "DB_HOST", "postgres-test", "DB_PORT", "5433", "DB_NAME", "test",
                "DB_USER", "env-user", "DB_PASSWORD", "env-password ");

        AppConfig config = AppConfig.load(file, environment);

        assertEquals("jdbc:postgresql://postgres-test:5433/test", config.getJdbcUrl());
        assertEquals("env-user", config.getDatabaseUser());
        assertEquals("env-password ", config.getDatabasePassword());
    }

    @Test
    void rejectsMissingFileEvenWithEnvironmentSettings() {
        assertThrows(NoSuchFileException.class,
                () -> AppConfig.load(directory.resolve("missing.properties"), Map.of(
                        "DB_HOST", "postgres", "DB_NAME", "test", "DB_USER", "user", "DB_PASSWORD", "password")));
    }

    @Test
    void rejectsMissingPassword() throws IOException {
        Path file = Files.writeString(directory.resolve("application.properties"), """
                db.host=postgres
                db.name=booking_coursework
                db.user=booking
                """);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(file, Map.of()));

        assertTrue(exception.getMessage().contains("DB_PASSWORD"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "0", "65536"})
    void rejectsInvalidPort(String port) throws IOException {
        Path file = Files.writeString(directory.resolve("application.properties"), FILE_SETTINGS + "db.port=" + port);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(file, Map.of()));

        assertTrue(exception.getMessage().contains("db.port"));
    }

    @Test
    void rejectsHostWithUrlParameters() throws IOException {
        Path file = Files.writeString(directory.resolve("application.properties"), FILE_SETTINGS);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(file, Map.of("DB_HOST", "postgres/test?user=other")));

        assertTrue(exception.getMessage().contains("db.host"));
    }

    @Test
    void readsHttpSettingsFromDifferentFilesWithoutRebuilding() throws IOException {
        Path first = Files.writeString(directory.resolve("first.properties"),
                FILE_SETTINGS + "http.host=127.0.0.1\nhttp.port=8081\n");
        Path second = Files.writeString(directory.resolve("second.properties"),
                FILE_SETTINGS + "http.host=0.0.0.0\nhttp.port=9090\n");

        AppConfig firstConfig = AppConfig.load(first, Map.of());
        AppConfig secondConfig = AppConfig.load(second, Map.of());

        assertEquals("127.0.0.1", firstConfig.getHttpHost());
        assertEquals(8081, firstConfig.getHttpPort());
        assertEquals("0.0.0.0", secondConfig.getHttpHost());
        assertEquals(9090, secondConfig.getHttpPort());
    }

    @Test
    void environmentOverridesInvalidHttpSettingsInFile() throws IOException {
        Path file = Files.writeString(directory.resolve("application.properties"),
                FILE_SETTINGS + "http.host=bad/host\nhttp.port=bad\n");

        AppConfig config = AppConfig.load(file, Map.of("HTTP_HOST", "127.0.0.1", "HTTP_PORT", "65535"));

        assertEquals("127.0.0.1", config.getHttpHost());
        assertEquals(65535, config.getHttpPort());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "0", "-1", "65536", "2147483648"})
    void rejectsInvalidHttpPort(String port) throws IOException {
        Path file = Files.writeString(directory.resolve("application.properties"), FILE_SETTINGS + "http.port=" + port);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(file, Map.of()));

        assertTrue(exception.getMessage().contains("http.port"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "http://localhost", "localhost/path", "127.0.0.1:8080", "bad host"})
    void rejectsInvalidHttpHost(String host) throws IOException {
        Path file = Files.writeString(directory.resolve("application.properties"), FILE_SETTINGS + "http.host=" + host);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(file, Map.of()));

        assertTrue(exception.getMessage().contains("http.host"));
    }
}
