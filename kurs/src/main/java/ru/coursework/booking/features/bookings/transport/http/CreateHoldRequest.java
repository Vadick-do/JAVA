package ru.coursework.booking.features.bookings.transport.http;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.BadRequestResponse;

public record CreateHoldRequest(long slotId) {
    public static CreateHoldRequest from(JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 1 || !body.has("slotId")) {
            throw new BadRequestResponse("Укажите только slotId.");
        }
        JsonNode value = body.get("slotId");
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0) {
            throw new BadRequestResponse("ID слота должен быть положительным целым числом.");
        }
        return new CreateHoldRequest(value.longValue());
    }
}
