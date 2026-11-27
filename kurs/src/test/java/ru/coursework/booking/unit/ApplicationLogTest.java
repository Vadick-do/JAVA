package ru.coursework.booking.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.coursework.booking.core.config.AppConfig.LogSettings;
import ru.coursework.booking.core.logging.ApplicationLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationLogTest {
    @TempDir
    Path directory;

    @Test
    void rotatesEachLogSeparatelyAndDoesNotWriteExceptionDetails() throws IOException {
        Path logs = directory.resolve("logs-%g");
        int limit = 256;
        try (ApplicationLog log = ApplicationLog.open(new LogSettings(logs, limit, 2, Level.INFO))) {
            for (int index = 0; index < 12; index++) {
                log.event("HOLD result=201 event=" + index);
                log.error("HOLD result=500 error=" + index, new SQLException("test-secret", "22008"));
            }
        }

        List<Path> files;
        try (var paths = Files.list(logs)) {
            files = paths.sorted().toList();
        }
        assertEquals(List.of("errors.0.log", "errors.1.log", "events.0.log", "events.1.log"),
                files.stream().map(path -> path.getFileName().toString()).toList());
        for (Path file : files) {
            assertTrue(Files.size(file) < limit + 128);
        }
        String events = Files.readString(logs.resolve("events.0.log")) + Files.readString(logs.resolve("events.1.log"));
        String errors = Files.readString(logs.resolve("errors.0.log")) + Files.readString(logs.resolve("errors.1.log"));
        assertTrue(events.contains(" INFO HOLD result=201 event=11"));
        assertFalse(events.contains("event=0\n"));
        assertFalse(events.contains("result=500"));
        assertTrue(errors.contains(" SEVERE HOLD result=500 error=11, тип=SQLException, SQLSTATE=22008"));
        assertFalse(errors.contains("result=201"));
        assertFalse(errors.contains("test-secret"));
        Instant.parse(events.split(" ", 2)[0]);
    }

    @Test
    void severeLevelKeepsErrorsAndFiltersOutOrdinaryEventsFromFiles() throws IOException {
        try (ApplicationLog log = ApplicationLog.open(new LogSettings(directory, 1024, 2, Level.SEVERE))) {
            log.event("HTTP ready");
            log.error("Test error");
        }

        assertEquals("", Files.readString(directory.resolve("events.0.log")));
        assertTrue(Files.readString(directory.resolve("errors.0.log")).contains(" SEVERE Test error"));
    }
}
