package lab6.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Camera {
    public static final String PHOTO_MODE = "фото";
    public static final String VIDEO_MODE = "видео";
    public static final int MAX_VIDEO_SECONDS = 86400;
    public static final double DEFAULT_ZOOM = 1.0;

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
        this(model, 0);
    }

    protected Camera(String model, int id) {
        this.id = id == 0 ? nextId++ : id;
        this.model = model == null || model.trim().isEmpty() ? "Камера" : model.trim();
        this.mode = PHOTO_MODE;
        this.images = new ArrayList<>();
        this.currentZoom = DEFAULT_ZOOM;
    }

    public void switchMode() {
        if (PHOTO_MODE.equals(this.mode)) {
            this.mode = VIDEO_MODE;
        } else {
            this.mode = PHOTO_MODE;
        }
    }

    public boolean setMode(String mode) {
        if (PHOTO_MODE.equalsIgnoreCase(mode)) {
            this.mode = PHOTO_MODE;
            return true;
        }
        if (VIDEO_MODE.equalsIgnoreCase(mode)) {
            this.mode = VIDEO_MODE;
            return true;
        }
        throw new IllegalArgumentException("Ошибка: допустимы только режимы 'фото' или 'видео'!");
    }

    public abstract void takePhoto();
    public abstract void recordVideo(int seconds);
    public abstract void zoom(double factor);

    protected void checkVideoDuration(int seconds) {
        if (seconds <= 0 || seconds > MAX_VIDEO_SECONDS) {
            throw new IllegalArgumentException("Ошибка: длительность записи должна быть от 1 до " + MAX_VIDEO_SECONDS + " сек!");
        }
    }

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
