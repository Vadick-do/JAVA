package ru.coursework.booking.features.resources.transport.http;

import ru.coursework.booking.core.domain.Resource;
import ru.coursework.booking.core.domain.ResourceType;

public record ResourceResponse(long id, String name, ResourceType type, int capacity, boolean active) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(resource.getId(), resource.getName(), resource.getType(),
                resource.getCapacity(), resource.isActive());
    }
}
