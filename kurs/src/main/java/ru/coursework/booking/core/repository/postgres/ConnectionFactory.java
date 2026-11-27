package ru.coursework.booking.core.repository.postgres;

import ru.coursework.booking.core.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConnectionFactory {
    private final AppConfig config;

    public ConnectionFactory(AppConfig config) {
        this.config = config;
    }

    public Connection open() throws SQLException {
        return open(10, 5);
    }

    public Connection openForCleanup() throws SQLException {
        return open(4, 3);
    }

    private Connection open(int socketTimeoutSeconds, int statementTimeoutSeconds) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", config.getDatabaseUser());
        properties.setProperty("password", config.getDatabasePassword());
        properties.setProperty("loginTimeout", "3");
        properties.setProperty("connectTimeout", "3");
        properties.setProperty("socketTimeout", Integer.toString(socketTimeoutSeconds));
        properties.setProperty("options", "-c statement_timeout=" + statementTimeoutSeconds + "s");
        return DriverManager.getConnection(config.getJdbcUrl(), properties);
    }
}
