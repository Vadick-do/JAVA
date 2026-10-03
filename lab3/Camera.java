package lab3;

import java.util.ArrayList;
import java.util.List;

public abstract class Camera {
    private String mode;
    private final List<String> images;

    public Camera() {
        this.mode = "фото";
        this.images = new ArrayList<>();
    }

    public void switchMode() {
        if ("фото".equalsIgnoreCase(this.mode)) {
            this.mode = "видео";
        } else {
            this.mode = "фото";
        }
        System.out.println("Режим изменен на: " + this.mode);
    }

    public void setMode(String mode) {
        if (mode == null || (!"фото".equalsIgnoreCase(mode) && !"видео".equalsIgnoreCase(mode))) {
            throw new IllegalArgumentException(
                "Недопустимый режим: " + mode + ". Допустимы только фото или видео."
            );
        }
        this.mode = mode.toLowerCase();
    }

    public abstract void takePhoto();
    public abstract void recordVideo(int seconds);
    public abstract void zoom(double factor);

    public String getMode() {
        return mode;
    }

    public List<String> getImages() {
        return images;
    }

    protected void addImages(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя файла не может быть пустым или null");
        }
        this.images.add(name);
    }
}