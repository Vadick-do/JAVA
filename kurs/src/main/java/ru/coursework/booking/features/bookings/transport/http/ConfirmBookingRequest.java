package ru.coursework.booking.features.bookings.transport.http;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.BadRequestResponse;
import ru.coursework.booking.core.domain.Booking;

public record ConfirmBookingRequest(String customerName, String customerContact) {
    public static ConfirmBookingRequest from(JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 2
                || !body.path("customerName").isTextual() || !body.path("customerContact").isTextual()) {
            throw new BadRequestResponse("Укажите имя и контакт клиента строками.");
        }
        String name = body.get("customerName").textValue();
        String contact = body.get("customerContact").textValue();
        try {
            Booking.validateCustomerDetails(name, contact);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestResponse(exception.getMessage());
        }
        return new ConfirmBookingRequest(name, contact);
    }
}
