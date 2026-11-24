package ru.coursework.booking;

import io.javalin.Javalin;
import ru.coursework.booking.background.ExpiredHoldCleanup;
import ru.coursework.booking.core.config.AppConfig;
import ru.coursework.booking.core.repository.postgres.MigrationRunner;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.core.transport.http.HttpServer;
import ru.coursework.booking.features.bookings.repository.postgres.PostgresBookingRepository;
import ru.coursework.booking.features.bookings.service.BookingService;
import ru.coursework.booking.features.bookings.transport.http.BookingHandler;
import ru.coursework.booking.features.resources.repository.postgres.PostgresResourceRepository;
import ru.coursework.booking.features.resources.service.ResourceService;
import ru.coursework.booking.features.resources.transport.http.ResourceHandler;
import ru.coursework.booking.features.slots.repository.postgres.PostgresSlotRepository;
import ru.coursework.booking.features.slots.service.SlotService;
import ru.coursework.booking.features.slots.transport.http.SlotHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

public class Application {
    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

    public static void main(String[] args) {
        if (args.length != 2 || !args[0].equals("--config")) {
            System.err.println("Использование: --config <путь к application.properties>");
            System.exit(1);
        }

        try {
            AppConfig config = AppConfig.load(Path.of(args[1]), System.getenv());
            int appliedMigrations = new MigrationRunner(config).migrate();
            LOGGER.info("Схема PostgreSQL готова. Применено миграций: " + appliedMigrations + ".");
            ConnectionFactory connections = new ConnectionFactory(config);
            PostgresResourceRepository resourceRepository = new PostgresResourceRepository();
            ResourceService resources = new ResourceService(connections, resourceRepository);
            PostgresSlotRepository slotRepository = new PostgresSlotRepository();
            SlotService slots = new SlotService(connections, resourceRepository, slotRepository);
            BookingService bookings = new BookingService(connections, resourceRepository, slotRepository,
                    new PostgresBookingRepository());
            Javalin server = HttpServer.create(new ResourceHandler(resources), new SlotHandler(slots),
                    new BookingHandler(bookings));
            ExpiredHoldCleanup cleanup = new ExpiredHoldCleanup();
            try {
                server.start(config.getHttpHost(), config.getHttpPort());
                Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(server, cleanup), "application-shutdown"));
                cleanup.start(bookings::expirePendingHolds, config.getCleanupIntervalSeconds());
            } catch (RuntimeException exception) {
                stop(server, cleanup);
                throw exception;
            }
        } catch (IOException exception) {
            System.err.println("Не удалось прочитать файл конфигурации: " + args[1]);
            System.exit(1);
        } catch (IllegalArgumentException exception) {
            System.err.println("Ошибка конфигурации: " + exception.getMessage());
            System.exit(1);
        } catch (RuntimeException exception) {
            LOGGER.severe("Не удалось запустить сервер, тип=" + exception.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static void stop(Javalin server, ExpiredHoldCleanup cleanup) {
        try {
            cleanup.close();
        } finally {
            server.stop();
        }
    }
}
