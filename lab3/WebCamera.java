package lab3;

import java.util.concurrent.ThreadLocalRandom;

public class WebCamera extends Camera{
    private String model;
    private double width;
    private double fps;
    private boolean microActive;
    private double currentZoom;

    public WebCamera() {
        this.model = "Стандартная камера";
        this.width = 1280.0;
        this.fps = 30.0;
        this.microActive = true;
        this.currentZoom = 1.0;
    }

    public WebCamera(String model, double width, double fps) {
        this.model = model;
        if (width < 320 || width > 3840) {
            System.out.println("Минимальное значение для ширины экрана 320 пикселей, а максимальное - 3840 пикселей");
            this.width = 1280.0;
        } else {
            this.width = width;
        }
        if (fps < 1 || fps > 120) {
            System.out.println("Минимальное значение для fps 1, а максимальное - 120");
            this.fps = 30.0;
        } else {
            this.fps = fps;
        }
        this.microActive = true;
        this.currentZoom = 1.0;
    }

    public WebCamera(String model, double width, double fps, boolean microphoneActive, double currentZoom) {
        this(model, width, fps); 
        this.microActive = microphoneActive;
        if (currentZoom >= 1.0 && currentZoom <= 4.0) {
            this.currentZoom = currentZoom;
        } else {
            this.currentZoom = 1.0;
        }
    }

    public String getModel() {
        return model;
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

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isMicrophoneEnabled() {
        return microActive;
    }

    public double getCurrentZoom() {
        return currentZoom;
    }

   public boolean setWidth(double width) {
        if (width < 320.0 || width > 3840.0) {
            System.out.println("Ошибка: ширина экрана должна быть в диапазоне от 320 до 3840 пикселей!");
            return false;
        }
        this.width = width;
        return true;
    }

    public boolean setFPS(double fps) {
        if (fps < 1.0 || fps > 120.0) {
            System.out.println("Ошибка: FPS должен быть в диапазоне от 1 до 120!");
            return false;
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
            System.out.println("Ошибка съемки: веб-камера сейчас в режиме видео!");
            return;
        }
        String fileName = "webcam_snap_" + (getImages().size() + 1) + ".png";
        addImages(fileName);
        System.out.println("Веб-камера [" + model + "] сделала снимок " 
                + (int) width + "x" + (int) ((width * 9) / 16) 
                + " (зум: " + currentZoom + "x). Сохранено в: " + fileName);
    }
    
    @Override 
    public void recordVideo(int seconds) {
        if (!"видео".equalsIgnoreCase(getMode())) {
            System.out.println("Ошибка записи: веб-камера сейчас в режиме фото!");
            return;
        }
        if (seconds <= 0) {
            System.out.println("Ошибка: длительность записи видео должна быть больше 0 секунд!");
            return;
        }
        System.out.println("Веб-камера ведет запись: " + seconds + " сек. при " + fps + " FPS."
                + " Микрофон: " + (microActive ? "включен" : "выключен"));
    }

    @Override 
    public void zoom(double factor) {
        if (factor < 1.0 || factor > 4.0) {
            System.out.println("Ошибка: цифровой зум веб-камеры поддерживает значения только от 1.0x до 4.0x!");
            return;
        }
        this.currentZoom = factor;
        System.out.println("Цифровой зум веб-камеры установлен на " + this.currentZoom + "x");
    }

    public void randomizeFps() {
        double randomValue = ThreadLocalRandom.current().nextDouble(15.0, 60.0);
        this.fps = Math.round(randomValue * 10.0) / 10.0;
        System.out.println("FPS веб-камеры случайно изменен: " + this.fps);
    }

    public void toggleMicrophone() {
        this.microActive = !this.microActive;
        System.out.println("Микрофон веб-камеры теперь: " + (microActive ? "включен" : "выключен"));
    }
}
