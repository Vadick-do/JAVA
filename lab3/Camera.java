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

    public abstract void takePhoto();
    public abstract void recordVideo(int seconds);
    public abstract void zoom(double factor);

    public String getMode() {
        return mode;
    }

    public List<String> getImages() {
        return images;
    }
}