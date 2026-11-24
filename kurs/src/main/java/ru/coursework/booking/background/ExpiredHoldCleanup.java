package ru.coursework.booking.background;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ExpiredHoldCleanup implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredHoldCleanup.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread worker = new Thread(task, "expired-hold-cleanup");
        worker.setDaemon(true);
        return worker;
    });

    public void start(CleanupPass pass, int intervalSeconds) {
        executor.scheduleWithFixedDelay(() -> runPass(pass), 0, intervalSeconds, TimeUnit.SECONDS);
        LOGGER.info("Очистка холдов запланирована. Интервал: {} с.", intervalSeconds);
    }

    private void runPass(CleanupPass pass) {
        try {
            int expired = pass.run();
            if (expired > 0) {
                LOGGER.info("Холды переведены в EXPIRED. Количество: {}.", expired);
            }
        } catch (SQLException exception) {
            LOGGER.warn("Ошибка очистки холдов, SQLSTATE={}", exception.getSQLState());
        } catch (RuntimeException exception) {
            LOGGER.warn("Ошибка очистки холдов, тип={}", exception.getClass().getSimpleName());
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    LOGGER.warn("Поток очистки холдов не завершился за отведённое время.");
                    return;
                }
            }
            LOGGER.info("Очистка холдов остановлена.");
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    public interface CleanupPass {
        int run() throws SQLException;
    }
}
