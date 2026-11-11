package lab6.model;

import java.util.Random;

public class WebCamera extends Camera {
    public static final String TYPE = "WebCamera";
    public static final double MIN_WIDTH = 320.0;
    public static final double MAX_WIDTH = 3840.0;
    public static final double DEFAULT_WIDTH = 1280.0;
    public static final double MIN_FPS = 1.0;
    public static final double MAX_FPS = 120.0;
    public static final double DEFAULT_FPS = 30.0;
    public static final double MIN_ZOOM = 1.0;
    public static final double MAX_ZOOM = 4.0;

    private double width;
    private double fps;
    private boolean microActive;

    public WebCamera() {
        this("Стандартная камера", DEFAULT_WIDTH, DEFAULT_FPS, true, DEFAULT_ZOOM);
    }

    public WebCamera(String model, double width, double fps) {
        this(model, width, fps, true, DEFAULT_ZOOM);
    }

    public WebCamera(String model, double width, double fps, boolean microphoneActive, double currentZoom) {
        this(model, width, fps, microphoneActive, currentZoom, 0);
    }

    public WebCamera(String model, double width, double fps, boolean microphoneActive, double currentZoom, int id) {
        super(model, id);
        if (!Double.isFinite(width) || width < MIN_WIDTH || width > MAX_WIDTH) {
            this.width = DEFAULT_WIDTH;
        } else {
            this.width = width;
        }
        if (!Double.isFinite(fps) || fps < MIN_FPS || fps > MAX_FPS) {
            this.fps = DEFAULT_FPS;
        } else {
            this.fps = fps;
        }
        this.microActive = microphoneActive;
        if (Double.isFinite(currentZoom) && currentZoom >= MIN_ZOOM && currentZoom <= MAX_ZOOM) {
            this.currentZoom = currentZoom;
        } else {
            this.currentZoom = DEFAULT_ZOOM;
        }
    }

    @Override
    public double getMinZoom() {
        return MIN_ZOOM;
    }

    @Override
    public double getMaxZoom() {
        return MAX_ZOOM;
    }

    public double getWidth() {
        return width;
    }

    public double getFPS() {
        return fps;
    }

    public boolean isMicrophoneEnabled() {
        return microActive;
    }

    public boolean setWidth(double width) {
        if (!Double.isFinite(width) || width < MIN_WIDTH || width > MAX_WIDTH) {
            throw new IllegalArgumentException("Ошибка: ширина экрана должна быть в диапазоне от 320 до 3840 пикселей!");
        }
        this.width = width;
        return true;
    }

    public boolean setFPS(double fps) {
        if (!Double.isFinite(fps) || fps < MIN_FPS || fps > MAX_FPS) {
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
        if (!PHOTO_MODE.equals(getMode())) {
            throw new IllegalStateException("Ошибка съемки: веб-камера сейчас в режиме видео!");
        }
        String fileName = "webcam_snap_" + (getImages().size() + 1) + ".png";
        String resolution = (int) width + "x" + (int) ((width * 9) / 16);
        Image image = new Image(fileName, resolution, 2.0, "PNG");
        addImage(image);
    }

    @Override
    public void recordVideo(int seconds) {
        if (!VIDEO_MODE.equals(getMode())) {
            throw new IllegalStateException("Ошибка записи: веб-камера сейчас в режиме фото!");
        }
        checkVideoDuration(seconds);
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
