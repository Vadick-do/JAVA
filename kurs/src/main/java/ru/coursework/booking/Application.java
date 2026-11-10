package ru.coursework.booking;

import org.flywaydb.core.api.FlywayException;
import ru.coursework.booking.core.config.AppConfig;
import ru.coursework.booking.core.repository.postgres.MigrationRunner;

import java.io.IOException;
import java.nio.file.Path;

public class Application {
    public static void main(String[] args) {
        if (args.length != 2 || !args[0].equals("--config")) {
            System.err.println("Использование: --config <путь к application.properties>");
            System.exit(1);
        }

        try {
            AppConfig config = AppConfig.load(Path.of(args[1]), System.getenv());
            int appliedMigrations = new MigrationRunner(config).migrate();
            System.out.println("Схема PostgreSQL готова. Применено миграций: " + appliedMigrations + ".");
        } catch (IOException exception) {
            System.err.println("Не удалось прочитать файл конфигурации: " + args[1]);
            System.exit(1);
        } catch (IllegalArgumentException exception) {
            System.err.println("Ошибка конфигурации: " + exception.getMessage());
            System.exit(1);
        } catch (FlywayException exception) {
            System.err.println("Ошибка миграции PostgreSQL: " + exception.getMessage());
            System.exit(1);
        }
    }
}
