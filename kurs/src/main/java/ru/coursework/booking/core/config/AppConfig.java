package ru.coursework.booking.core.config;

import java.io.IOException;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class AppConfig {
    private final String jdbcUrl;
    private final String databaseUser;
    private final String databasePassword;
    private final String httpHost;
    private final int httpPort;

    private AppConfig(String jdbcUrl, String databaseUser, String databasePassword, String httpHost, int httpPort) {
        this.jdbcUrl = jdbcUrl;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
        this.httpHost = httpHost;
        this.httpPort = httpPort;
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
        return new AppConfig("jdbc:postgresql://" + host + ":" + port + "/" + encodedDatabase,
                user, password, httpHost, httpPort);
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
}
