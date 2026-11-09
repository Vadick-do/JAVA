package ru.coursework.booking;

import ru.coursework.booking.core.config.AppConfig;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Application {
    public static void main(String[] args) {
        if (args.length != 2 || !args[0].equals("--config")) {
            System.err.println("Использование: --config <путь к application.properties>");
            System.exit(1);
        }

        try {
            AppConfig config = AppConfig.load(Path.of(args[1]), System.getenv());
            verifyDatabase(new ConnectionFactory(config));
            System.out.println("Подключение к PostgreSQL проверено: SELECT 1 = 1.");
        } catch (IOException exception) {
            System.err.println("Не удалось прочитать файл конфигурации: " + args[1]);
            System.exit(1);
        } catch (IllegalArgumentException exception) {
            System.err.println("Ошибка конфигурации: " + exception.getMessage());
            System.exit(1);
        } catch (SQLException exception) {
            System.err.println("Ошибка PostgreSQL (SQLState " + exception.getSQLState() + ").");
            System.exit(1);
        }
    }

    private static void verifyDatabase(ConnectionFactory factory) throws SQLException {
        try (Connection connection = factory.open();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1")) {
            if (!result.next() || result.getInt(1) != 1) {
                throw new SQLException("Неожиданный результат SELECT 1.");
            }
        }
    }
}
