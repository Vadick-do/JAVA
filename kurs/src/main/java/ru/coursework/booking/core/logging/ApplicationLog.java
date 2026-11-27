package ru.coursework.booking.core.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.coursework.booking.core.config.AppConfig.LogSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public final class ApplicationLog implements AutoCloseable {
    private static final Logger CONSOLE = LoggerFactory.getLogger(ApplicationLog.class);
    public static final ApplicationLog CONSOLE_ONLY = new ApplicationLog(null, null);

    private final FileHandler events;
    private final FileHandler errors;

    private ApplicationLog(FileHandler events, FileHandler errors) {
        this.events = events;
        this.errors = errors;
    }

    public static ApplicationLog open(LogSettings settings) throws IOException {
        Files.createDirectories(settings.directory());
        FileHandler events = openFile(settings, "events");
        try {
            return new ApplicationLog(events, openFile(settings, "errors"));
        } catch (IOException | RuntimeException exception) {
            events.close();
            throw exception;
        }
    }

    private static FileHandler openFile(LogSettings settings, String name) throws IOException {
        String pattern = settings.directory().resolve(name).toAbsolutePath().toString().replace("%", "%%");
        FileHandler file = new FileHandler(pattern + ".%g.log", settings.limitBytes(), settings.fileCount(), true);
        try {
            file.setLevel(settings.level());
            file.setEncoding(StandardCharsets.UTF_8.name());
            file.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return "%s %s %s%n".formatted(record.getInstant(), record.getLevel().getName(), record.getMessage());
                }
            });
            return file;
        } catch (IOException | RuntimeException exception) {
            file.close();
            throw exception;
        }
    }

    public void event(String message) {
        CONSOLE.info(message);
        if (events != null) {
            events.publish(new LogRecord(Level.INFO, message));
        }
    }

    public void error(String message, Exception exception) {
        String details = "тип=" + exception.getClass().getSimpleName();
        if (exception instanceof SQLException sqlException) {
            details += ", SQLSTATE=" + sqlException.getSQLState();
        }
        if (exception.getSuppressed().length > 0) {
            details += ", вторичных исключений=" + exception.getSuppressed().length;
        }
        error(message + ", " + details);
    }

    public void error(String message) {
        CONSOLE.error(message);
        if (errors != null) {
            errors.publish(new LogRecord(Level.SEVERE, message));
        }
    }

    @Override
    public void close() {
        if (events != null) {
            try {
                events.close();
            } finally {
                errors.close();
            }
        }
    }
}
