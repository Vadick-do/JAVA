package ru.coursework.booking.core.repository.postgres;

import org.flywaydb.core.Flyway;
import ru.coursework.booking.core.config.AppConfig;

public final class MigrationRunner {
    private final AppConfig config;

    public MigrationRunner(AppConfig config) {
        this.config = config;
    }

    public int migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(config.getJdbcUrl(), config.getDatabaseUser(), config.getDatabasePassword())
                .load();
        return flyway.migrate().migrationsExecuted;
    }
}
