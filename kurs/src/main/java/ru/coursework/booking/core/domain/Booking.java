package ru.coursework.booking.core.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class Booking {
    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    private final UUID id;
    private final Slot slot;
    private final BookingStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final String customerName;
    private final String customerContact;
    private final Instant confirmedAt;
    private final Instant cancelledAt;

    private Booking(UUID id, Slot slot, BookingStatus status, Instant createdAt,
                    String customerName, String customerContact, Instant confirmedAt, Instant cancelledAt) {
        this.id = id;
        this.slot = slot;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plus(HOLD_DURATION);
        this.customerName = customerName;
        this.customerContact = customerContact;
        this.confirmedAt = confirmedAt;
        this.cancelledAt = cancelledAt;
    }

    public static Booking hold(UUID id, Slot slot, Instant now) {
        if (id == null || slot == null || now == null) {
            throw new IllegalArgumentException("ID брони, слот и время создания должны быть указаны.");
        }
        if (!now.isBefore(slot.getStartsAt())) {
            throw new IllegalStateException("Начавшийся слот нельзя удержать.");
        }
        return new Booking(id, slot, BookingStatus.HOLD, now, null, null, null, null);
    }

    public BookingStatus getEffectiveStatus(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Текущее время должно быть указано.");
        }
        if (status == BookingStatus.HOLD && (!now.isBefore(expiresAt) || !now.isBefore(slot.getStartsAt()))) {
            return BookingStatus.EXPIRED;
        }
        return status;
    }

    public Booking confirm(String name, String contact, Instant now) {
        String normalizedName = normalizeCustomerField(name, 100, "Имя клиента");
        String normalizedContact = normalizeCustomerField(contact, 200, "Контакт клиента");
        BookingStatus effectiveStatus = getEffectiveStatus(now);
        if (effectiveStatus == BookingStatus.CONFIRMED) {
            if (customerName.equals(normalizedName) && customerContact.equals(normalizedContact)) {
                return this;
            }
            throw new IllegalStateException("Бронь уже подтверждена с другими данными клиента.");
        }
        if (effectiveStatus != BookingStatus.HOLD) {
            throw new IllegalStateException("Подтвердить можно только действующий холд.");
        }
        return new Booking(id, slot, BookingStatus.CONFIRMED, createdAt, normalizedName, normalizedContact, now, null);
    }

    public Booking cancel(Instant now) {
        Booking current = expire(now);
        if (current.status == BookingStatus.CANCELLED || current.status == BookingStatus.EXPIRED) {
            return current;
        }
        return new Booking(id, slot, BookingStatus.CANCELLED, createdAt, customerName, customerContact, confirmedAt, now);
    }

    public Booking expire(Instant now) {
        BookingStatus effectiveStatus = getEffectiveStatus(now);
        if (status == BookingStatus.HOLD && effectiveStatus == BookingStatus.EXPIRED) {
            return new Booking(id, slot, BookingStatus.EXPIRED, createdAt, null, null, null, null);
        }
        return this;
    }

    private static String normalizeCustomerField(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " не может быть пустым.");
        }
        String normalizedValue = value.strip();
        if (normalizedValue.codePointCount(0, normalizedValue.length()) > maxLength) {
            throw new IllegalArgumentException(field + " не может содержать больше " + maxLength + " символов.");
        }
        return normalizedValue;
    }

    public UUID getId() {
        return id;
    }

    public long getSlotId() {
        return slot.getId();
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }
}
