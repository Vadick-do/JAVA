package ru.coursework.booking.core.domain;

public final class Resource {
    private final long id;
    private final String name;
    private final ResourceType type;
    private final int capacity;
    private final boolean active;

    public Resource(long id, String name, ResourceType type, int capacity, boolean active) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID ресурса должен быть положительным.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название ресурса не должно быть пустым.");
        }
        String normalizedName = name.strip();
        if (normalizedName.codePointCount(0, normalizedName.length()) > 100) {
            throw new IllegalArgumentException("Название ресурса должно содержать не более 100 символов.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Тип ресурса должен быть указан.");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Вместимость ресурса должна быть положительной.");
        }
        this.id = id;
        this.name = normalizedName;
        this.type = type;
        this.capacity = capacity;
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ResourceType getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isActive() {
        return active;
    }
}
