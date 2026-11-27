package ru.coursework.booking.background;

import ru.coursework.booking.core.logging.ApplicationLog;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ExpiredHoldCleanup implements AutoCloseable {
    private final ApplicationLog log;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread worker = new Thread(task, "expired-hold-cleanup");
        worker.setDaemon(true);
        return worker;
    });

    public ExpiredHoldCleanup() {
        this(ApplicationLog.CONSOLE_ONLY);
    }

    public ExpiredHoldCleanup(ApplicationLog log) {
        this.log = log;
    }

    public void start(CleanupPass pass, int intervalSeconds) {
        executor.scheduleWithFixedDelay(() -> runPass(pass), 0, intervalSeconds, TimeUnit.SECONDS);
        log.event("Очистка холдов запланирована. Интервал: " + intervalSeconds + " с.");
    }

    private void runPass(CleanupPass pass) {
        try {
            int expired = pass.run();
            if (expired > 0) {
                log.event("Холды переведены в EXPIRED. Количество: " + expired + ".");
            }
        } catch (SQLException | RuntimeException exception) {
            log.error("Ошибка очистки холдов", exception);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.error("Поток очистки холдов не завершился за отведённое время.");
                    return;
                }
            }
            log.event("Очистка холдов остановлена.");
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
