package lab5;

import java.util.ArrayList;
import java.util.List;

public abstract class Camera {
    private static int nextId = 1;
    private final int id;
    protected String model;
    private String mode;
    protected double currentZoom;
    private final List<Image> images;

    public Camera() {
        this("Камера");
    }

    public Camera(String model) {
        this.id = nextId++;
        this.model = model == null || model.trim().isEmpty() ? "Камера" : model.trim();
        this.mode = "фото";
        this.images = new ArrayList<>();
        this.currentZoom = 1.0;
    }

    public void switchMode() {
        if ("фото".equalsIgnoreCase(this.mode)) {
            this.mode = "видео";
        } else {
            this.mode = "фото";
        }
    }

    public boolean setMode(String mode) {
        if (mode != null && ("фото".equalsIgnoreCase(mode) || "видео".equalsIgnoreCase(mode))) {
            this.mode = mode.toLowerCase();
            return true;
        }
        throw new IllegalArgumentException("Ошибка: допустимы только режимы 'фото' или 'видео'!");
    }

    public abstract void takePhoto();
    public abstract void recordVideo(int seconds);
    public abstract void zoom(double factor);

    public abstract double getMinZoom();
    public abstract double getMaxZoom();

    public int getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Ошибка: модель не может быть пустой!");
        }
        this.model = model.trim();
    }

    public double getCurrentZoom() {
        return currentZoom;
    }

    public String getMode() {
        return mode;
    }

    public List<Image> getImages() {
        return images;
    }

    protected void addImage(Image image) {
        if (image != null) {
            this.images.add(image);
        }
    }

    protected void addImage(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.images.add(new Image(name));
        }
    }
}
