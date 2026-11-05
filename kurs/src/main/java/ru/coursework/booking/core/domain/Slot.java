package ru.coursework.booking.core.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Slot {
    private final long id;
    private final long resourceId;
    private final Instant startsAt;
    private final Instant endsAt;

    public Slot(long id, long resourceId, Instant startsAt, Instant endsAt) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID слота должен быть положительным.");
        }
        if (resourceId <= 0) {
            throw new IllegalArgumentException("ID ресурса должен быть положительным.");
        }
        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException("Начало и конец слота должны быть указаны.");
        }
        if (!startsAt.equals(startsAt.truncatedTo(ChronoUnit.HOURS))) {
            throw new IllegalArgumentException("Слот должен начинаться на границе часа UTC.");
        }
        if (!Duration.between(startsAt, endsAt).equals(Duration.ofHours(1))) {
            throw new IllegalArgumentException("Слот должен длиться ровно 60 минут.");
        }
        this.id = id;
        this.resourceId = resourceId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public long getId() {
        return id;
    }

    public long getResourceId() {
        return resourceId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }
}
