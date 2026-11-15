package ru.coursework.booking.features.slots.transport.http;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import ru.coursework.booking.core.domain.Slot;
import ru.coursework.booking.features.slots.service.SlotService;

import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public final class SlotHandler {
    private final SlotService service;

    public SlotHandler(SlotService service) {
        this.service = service;
    }

    public void findAvailable(Context context) throws SQLException {
        long resourceId;
        try {
            resourceId = Long.parseLong(context.pathParam("id"));
        } catch (NumberFormatException exception) {
            throw new BadRequestResponse("ID ресурса должен быть положительным целым числом.");
        }
        Instant from = readBoundary(context, "from");
        Instant to = readBoundary(context, "to");
        List<Slot> slots;
        try {
            slots = service.findAvailable(resourceId, from, to);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestResponse(exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw new NotFoundResponse("Ресурс не найден.");
        }
        List<SlotResponse> response = new ArrayList<>();
        for (Slot slot : slots) {
            response.add(SlotResponse.from(slot));
        }
        context.json(response);
    }

    private Instant readBoundary(Context context, String name) {
        List<String> values = context.queryParams(name);
        if (values.size() != 1) {
            throw new BadRequestResponse("Укажите ровно один параметр " + name + ".");
        }
        try {
            return OffsetDateTime.parse(values.get(0)).toInstant();
        } catch (DateTimeParseException exception) {
            throw new BadRequestResponse("Параметр " + name + " должен содержать дату и время с offset.");
        }
    }
}
