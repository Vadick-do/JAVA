package lab5;

import java.util.Random;

public class WebCamera extends Camera {
    private double width;
    private double fps;
    private boolean microActive;

    public WebCamera() {
        this("Стандартная камера", 1280.0, 30.0, true, 1.0);
    }

    public WebCamera(String model, double width, double fps) {
        this(model, width, fps, true, 1.0);
    }

    public WebCamera(String model, double width, double fps, boolean microphoneActive, double currentZoom) {
        super(model);
        if (!Double.isFinite(width) || width < 320 || width > 3840) {
            this.width = 1280.0;
        } else {
            this.width = width;
        }
        if (!Double.isFinite(fps) || fps < 1 || fps > 120) {
            this.fps = 30.0;
        } else {
            this.fps = fps;
        }
        this.microActive = microphoneActive;
        if (currentZoom >= 1.0 && currentZoom <= 4.0) {
            this.currentZoom = currentZoom;
        } else {
            this.currentZoom = 1.0;
        }
    }

    @Override
    public double getMinZoom() {
        return 1.0;
    }

    @Override
    public double getMaxZoom() {
        return 4.0;
    }

    public double getWidth() {
        return width;
    }

    public double getWigth() {
        return width;
    }

    public double getFPS() {
        return fps;
    }

    public boolean isMicrophoneEnabled() {
        return microActive;
    }

    public boolean setWidth(double width) {
        if (!Double.isFinite(width) || width < 320.0 || width > 3840.0) {
            throw new IllegalArgumentException("Ошибка: ширина экрана должна быть в диапазоне от 320 до 3840 пикселей!");
        }
        this.width = width;
        return true;
    }

    public boolean setFPS(double fps) {
        if (!Double.isFinite(fps) || fps < 1.0 || fps > 120.0) {
            throw new IllegalArgumentException("Ошибка: FPS должен быть в диапазоне от 1 до 120!");
        }
        this.fps = fps;
        return true;
    }

    public double countCharacteristics() {
        return this.width * this.fps;
    }

    public double calculateMegapixels() {
        double height = (this.width * 9) / 16;
        return (this.width * height) / 1_000_000.0;
    }

    @Override
    public void takePhoto() {
        if (!"фото".equalsIgnoreCase(getMode())) {
            throw new IllegalStateException("Ошибка съемки: веб-камера сейчас в режиме видео!");
        }
        String fileName = "webcam_snap_" + (getImages().size() + 1) + ".png";
        String resolution = (int) width + "x" + (int) ((width * 9) / 16);
        Image image = new Image(fileName, resolution, 2.0, "PNG");
        addImage(image);
    }

    @Override
    public void recordVideo(int seconds) {
        if (!"видео".equalsIgnoreCase(getMode())) {
            throw new IllegalStateException("Ошибка записи: веб-камера сейчас в режиме фото!");
        }
        if (seconds <= 0) {
            throw new IllegalStateException("Ошибка: длительность записи видео должна быть больше 0 секунд!");
        }
    }

    @Override
    public void zoom(double factor) {
        if (!Double.isFinite(factor) || factor < getMinZoom() || factor > getMaxZoom()) {
            throw new IllegalArgumentException("Ошибка: цифровой зум веб-камеры поддерживает значения только от "
                    + getMinZoom() + "x до " + getMaxZoom() + "x!");
        }
        this.currentZoom = factor;
    }

    public void randomizeFps() {
        Random random = new Random();
        double randomValue = 15.0 + random.nextDouble() * 45.0;
        this.fps = Math.round(randomValue * 10.0) / 10.0;
    }

    public void toggleMicrophone() {
        this.microActive = !this.microActive;
    }
}
