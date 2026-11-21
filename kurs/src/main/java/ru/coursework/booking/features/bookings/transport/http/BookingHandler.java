package ru.coursework.booking.features.bookings.transport.http;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import ru.coursework.booking.core.domain.Booking;
import ru.coursework.booking.core.transport.http.ErrorResponse;
import ru.coursework.booking.features.bookings.service.BookingService;
import ru.coursework.booking.features.bookings.service.ResourceInactiveException;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.UUID;

public final class BookingHandler {
    private final BookingService service;

    public BookingHandler(BookingService service) {
        this.service = service;
    }

    public void createHold(Context context) throws SQLException {
        CreateHoldRequest request = CreateHoldRequest.from(context.bodyAsClass(JsonNode.class));
        Booking booking;
        try {
            booking = service.createHold(request.slotId());
        } catch (NoSuchElementException exception) {
            throw new NotFoundResponse("Слот не найден.");
        } catch (ResourceInactiveException exception) {
            context.status(409).json(new ErrorResponse("RESOURCE_INACTIVE", "Ресурс неактивен."));
            return;
        } catch (IllegalStateException exception) {
            throw new ConflictResponse("Создание холда невозможно.");
        }
        context.status(201).json(BookingResponse.from(booking));
    }

    public void confirmBooking(Context context) throws SQLException {
        UUID id = parseBookingId(context.pathParam("id"));
        ConfirmBookingRequest request = ConfirmBookingRequest.from(context.bodyAsClass(JsonNode.class));
        Booking booking;
        try {
            booking = service.confirmBooking(id, request.customerName(), request.customerContact());
        } catch (NoSuchElementException exception) {
            throw new NotFoundResponse("Бронь не найдена.");
        } catch (IllegalStateException exception) {
            context.status(409).json(new ErrorResponse("BOOKING_CONFLICT", "Подтверждение брони невозможно."));
            return;
        }
        context.json(BookingResponse.from(booking));
    }

    public void cancelBooking(Context context) throws SQLException {
        UUID id = parseBookingId(context.pathParam("id"));
        Booking booking;
        try {
            booking = service.cancelBooking(id);
        } catch (NoSuchElementException exception) {
            throw new NotFoundResponse("Бронь не найдена.");
        }
        context.json(BookingResponse.from(booking));
    }

    public void findById(Context context) throws SQLException {
        UUID id = parseBookingId(context.pathParam("id"));
        Booking booking;
        try {
            booking = service.findById(id);
        } catch (NoSuchElementException exception) {
            throw new NotFoundResponse("Бронь не найдена.");
        }
        context.json(BookingResponse.from(booking));
    }

    private UUID parseBookingId(String value) {
        try {
            UUID id = UUID.fromString(value);
            if (!id.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException("Неканонический UUID.");
            }
            return id;
        } catch (IllegalArgumentException exception) {
            throw new BadRequestResponse("ID брони должен быть UUID.");
        }
    }
}
