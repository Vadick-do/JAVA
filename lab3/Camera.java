package lab3;

import java.util.ArrayList;
import java.util.List;

public abstract class Camera {
    private static int nextId = 1;
    private final int id;
    protected String model;
    private String mode;
    protected double currentZoom;
    private final List<String> images;

    public Camera() {
        this("Камера");
    }

    public Camera(String model) {
        this.id = nextId++;
        this.model = model;
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
        System.out.println("Режим изменен на: " + this.mode);
    }

    public boolean setMode(String mode) {
        if (mode != null && ("фото".equalsIgnoreCase(mode) || "видео".equalsIgnoreCase(mode))) {
            this.mode = mode.toLowerCase();
            return true;
        }
        System.out.println("Ошибка: допустимы только режимы 'фото' или 'видео'!");
        return false;
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
        this.model = model;
    }

    public double getCurrentZoom() {
        return currentZoom;
    }

    public String getMode() {
        return mode;
    }

    public List<String> getImages() {
        return images;
    }

    public void printImages() {
        if (images.isEmpty()) {
            System.out.println("Список снимков пуст.");
            return;
        }
        System.out.println("Список сохраненных снимков (" + images.size() + " шт.):");
        for (int i = 0; i < images.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + images.get(i));
        }
    }

    protected void addImage(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.images.add(name);
        }
    }

    protected void addImages(String name) {
        addImage(name);
    }
}