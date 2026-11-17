package ru.coursework.booking.features.bookings.service;

public final class ResourceInactiveException extends IllegalStateException {
    public ResourceInactiveException() {
        super("Ресурс неактивен.");
    }
}
