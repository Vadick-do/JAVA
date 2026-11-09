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
}
