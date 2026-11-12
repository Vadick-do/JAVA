package ru.coursework.booking.features.resources.transport.http;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import ru.coursework.booking.core.domain.Resource;
import ru.coursework.booking.features.resources.service.ResourceService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ResourceHandler {
    private final ResourceService service;

    public ResourceHandler(ResourceService service) {
        this.service = service;
    }

    public void findAll(Context context) throws SQLException {
        List<ResourceResponse> response = new ArrayList<>();
        for (Resource resource : service.findAll()) {
            if (resource.isActive()) {
                response.add(ResourceResponse.from(resource));
            }
        }
        context.json(response);
    }

    public void findById(Context context) throws SQLException {
        long id;
        try {
            id = Long.parseLong(context.pathParam("id"));
        } catch (NumberFormatException exception) {
            throw new BadRequestResponse("ID ресурса должен быть положительным целым числом.");
        }
        if (id <= 0) {
            throw new BadRequestResponse("ID ресурса должен быть положительным целым числом.");
        }
        Resource resource = service.findById(id)
                .filter(Resource::isActive)
                .orElseThrow(() -> new NotFoundResponse("Ресурс не найден."));
        context.json(ResourceResponse.from(resource));
    }
}
