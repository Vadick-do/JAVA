package ru.coursework.booking.core.config;

import java.io.IOException;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

public final class AppConfig {
    private final String jdbcUrl;
    private final String databaseUser;
    private final String databasePassword;
    private final String httpHost;
    private final int httpPort;
    private final int cleanupIntervalSeconds;
    private final LogSettings logSettings;

    private AppConfig(String jdbcUrl, String databaseUser, String databasePassword, String httpHost, int httpPort,
                      int cleanupIntervalSeconds, LogSettings logSettings) {
        this.jdbcUrl = jdbcUrl;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
        this.httpHost = httpHost;
        this.httpPort = httpPort;
        this.cleanupIntervalSeconds = cleanupIntervalSeconds;
        this.logSettings = logSettings;
    }

    public static AppConfig load(Path path, Map<String, String> environment) throws IOException {
        Properties file = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            file.load(reader);
        }

        String host = required(environment, file, "DB_HOST", "db.host");
        if (!host.matches("[A-Za-z0-9][A-Za-z0-9.-]*")) {
            throw new IllegalArgumentException("db.host (DB_HOST): укажите DNS-имя или IPv4-адрес.");
        }
        int port = parsePort(environment.getOrDefault("DB_PORT", file.getProperty("db.port", "5432")), "db.port (DB_PORT)");
        String database = required(environment, file, "DB_NAME", "db.name");
        String user = required(environment, file, "DB_USER", "db.user");
        String password = required(environment, file, "DB_PASSWORD", "db.password");
        String encodedDatabase = URLEncoder.encode(database, StandardCharsets.UTF_8);
        String httpHost = environment.getOrDefault("HTTP_HOST", file.getProperty("http.host", "0.0.0.0"));
        if (!httpHost.matches("[A-Za-z0-9][A-Za-z0-9.-]*")) {
            throw new IllegalArgumentException("http.host (HTTP_HOST): укажите DNS-имя или IPv4-адрес.");
        }
        int httpPort = parsePort(environment.getOrDefault("HTTP_PORT", file.getProperty("http.port", "8080")),
                "http.port (HTTP_PORT)");
        int cleanupIntervalSeconds = parsePositiveInt(environment.getOrDefault("CLEANUP_INTERVAL_SECONDS",
                file.getProperty("cleanup.interval.seconds", "30")), "cleanup.interval.seconds (CLEANUP_INTERVAL_SECONDS)");
        String logDirectory = environment.getOrDefault("LOG_DIRECTORY", file.getProperty("log.directory", "logs"));
        if (logDirectory.isBlank()) {
            throw new IllegalArgumentException("log.directory (LOG_DIRECTORY): укажите каталог журнала.");
        }
        int logLimitBytes = parsePositiveInt(environment.getOrDefault("LOG_FILE_LIMIT_BYTES",
                file.getProperty("log.file.limit.bytes", "1048576")), "log.file.limit.bytes (LOG_FILE_LIMIT_BYTES)");
        int logFileCount = parsePositiveInt(environment.getOrDefault("LOG_FILE_COUNT",
                file.getProperty("log.file.count", "3")), "log.file.count (LOG_FILE_COUNT)");
        Level logFileLevel = switch (environment.getOrDefault("LOG_FILE_LEVEL", file.getProperty("log.file.level", "INFO"))) {
            case "INFO" -> Level.INFO;
            case "SEVERE" -> Level.SEVERE;
            default -> throw new IllegalArgumentException("log.file.level (LOG_FILE_LEVEL): укажите INFO или SEVERE.");
        };
        return new AppConfig("jdbc:postgresql://" + host + ":" + port + "/" + encodedDatabase,
                user, password, httpHost, httpPort, cleanupIntervalSeconds,
                new LogSettings(Path.of(logDirectory), logLimitBytes, logFileCount, logFileLevel));
    }

    private static String required(Map<String, String> environment, Properties file, String variable, String key) {
        String value = environment.getOrDefault(variable, file.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Укажите " + key + " или " + variable + ".");
        }
        return value;
    }

    private static int parsePort(String value, String setting) {
        String error = setting + ": укажите целое число от 1 до 65535.";
        int port;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(error);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(error);
        }
        return port;
    }

    private static int parsePositiveInt(String value, String setting) {
        String error = setting + ": укажите положительное целое число.";
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(error);
        }
        if (number < 1) {
            throw new IllegalArgumentException(error);
        }
        return number;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public String getHttpHost() {
        return httpHost;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public int getCleanupIntervalSeconds() {
        return cleanupIntervalSeconds;
    }

    public LogSettings getLogSettings() {
        return logSettings;
    }

    public record LogSettings(Path directory, int limitBytes, int fileCount, Level level) {
    }
}
