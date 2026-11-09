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

    private AppConfig(String jdbcUrl, String databaseUser, String databasePassword) {
        this.jdbcUrl = jdbcUrl;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
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
        int port = parsePort(environment.getOrDefault("DB_PORT", file.getProperty("db.port", "5432")));
        String database = required(environment, file, "DB_NAME", "db.name");
        String user = required(environment, file, "DB_USER", "db.user");
        String password = required(environment, file, "DB_PASSWORD", "db.password");
        String encodedDatabase = URLEncoder.encode(database, StandardCharsets.UTF_8);
        return new AppConfig("jdbc:postgresql://" + host + ":" + port + "/" + encodedDatabase, user, password);
    }

    private static String required(Map<String, String> environment, Properties file, String variable, String key) {
        String value = environment.getOrDefault(variable, file.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Укажите " + key + " или " + variable + ".");
        }
        return value;
    }

    private static int parsePort(String value) {
        String error = "db.port (DB_PORT): укажите целое число от 1 до 65535.";
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
}
