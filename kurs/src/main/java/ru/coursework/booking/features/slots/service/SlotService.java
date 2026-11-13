package ru.coursework.booking.features.slots.service;

import ru.coursework.booking.core.domain.Resource;
import ru.coursework.booking.core.domain.Slot;
import ru.coursework.booking.core.repository.postgres.ConnectionFactory;
import ru.coursework.booking.features.resources.repository.ResourceRepository;
import ru.coursework.booking.features.slots.repository.SlotRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

public final class SlotService {
    private final ConnectionFactory connections;
    private final ResourceRepository resources;
    private final SlotRepository slots;

    public SlotService(ConnectionFactory connections, ResourceRepository resources, SlotRepository slots) {
        this.connections = connections;
        this.resources = resources;
        this.slots = slots;
    }

    public List<Slot> findAvailable(long resourceId, Instant from, Instant to) throws SQLException {
        if (resourceId <= 0) {
            throw new IllegalArgumentException("ID ресурса должен быть положительным.");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("Укажите начало и конец диапазона.");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("Начало диапазона должно быть раньше конца.");
        }
        if (Duration.between(from, to).compareTo(Duration.ofDays(7)) > 0) {
            throw new IllegalArgumentException("Диапазон не должен превышать 7 суток.");
        }
        try (Connection connection = connections.open()) {
            Resource resource = resources.findById(connection, resourceId)
                    .orElseThrow(() -> new NoSuchElementException("Ресурс не найден."));
            if (!resource.isActive()) {
                return List.of();
            }
            return slots.findAvailable(connection, resourceId, from, to);
        }
    }
}
