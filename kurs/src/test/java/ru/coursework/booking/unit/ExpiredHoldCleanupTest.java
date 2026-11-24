package ru.coursework.booking.unit;

import org.junit.jupiter.api.Test;
import ru.coursework.booking.background.ExpiredHoldCleanup;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpiredHoldCleanupTest {
    @Test
    void waitsAfterThePassAndKeepsRunningAfterAnSqlFailure() throws InterruptedException {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstPass = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        AtomicLong firstFinishedAt = new AtomicLong();
        AtomicLong secondStartedAt = new AtomicLong();
        AtomicReference<Thread> worker = new AtomicReference<>();

        try (ExpiredHoldCleanup cleanup = new ExpiredHoldCleanup()) {
            cleanup.start(() -> {
                if (attempts.incrementAndGet() == 1) {
                    worker.set(Thread.currentThread());
                    firstStarted.countDown();
                    try {
                        releaseFirstPass.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    firstFinishedAt.set(System.nanoTime());
                    throw new SQLException("Тестовая ошибка", "57014");
                }
                secondStartedAt.compareAndSet(0, System.nanoTime());
                secondStarted.countDown();
                return 0;
            }, 1);

            assertTrue(firstStarted.await(3, TimeUnit.SECONDS));
            try {
                assertFalse(secondStarted.await(1200, TimeUnit.MILLISECONDS));
            } finally {
                releaseFirstPass.countDown();
            }
            assertTrue(secondStarted.await(3, TimeUnit.SECONDS));
            assertTrue(secondStartedAt.get() - firstFinishedAt.get() >= TimeUnit.SECONDS.toNanos(1));
        }

        assertTrue(worker.get().join(Duration.ofSeconds(2)));
    }

    @Test
    void interruptsTheWorkerAndRestoresInterruptWhenShutdownWaitIsInterrupted() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean workerInterrupted = new AtomicBoolean();
        AtomicBoolean interruptRestored = new AtomicBoolean();

        try (ExpiredHoldCleanup cleanup = new ExpiredHoldCleanup()) {
            cleanup.start(() -> {
                started.countDown();
                try {
                    new CountDownLatch(1).await(8, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    workerInterrupted.set(true);
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
                return 0;
            }, 1);

            assertTrue(started.await(3, TimeUnit.SECONDS));
            Thread stopper = new Thread(() -> {
                Thread.currentThread().interrupt();
                cleanup.close();
                interruptRestored.set(Thread.currentThread().isInterrupted());
            });
            stopper.start();

            assertTrue(stopper.join(Duration.ofSeconds(3)));
            assertTrue(interruptRestored.get());
            assertTrue(finished.await(3, TimeUnit.SECONDS));
            assertTrue(workerInterrupted.get());
        }
    }
}
