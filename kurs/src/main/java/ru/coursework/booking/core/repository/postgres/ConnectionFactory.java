package ru.coursework.booking.core.repository.postgres;

import ru.coursework.booking.core.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionFactory {
    private final AppConfig config;

    public ConnectionFactory(AppConfig config) {
        this.config = config;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(config.getJdbcUrl(), config.getDatabaseUser(), config.getDatabasePassword());
    }
}
