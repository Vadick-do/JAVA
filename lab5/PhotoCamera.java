package lab5;

import java.util.Random;

public class PhotoCamera extends Camera {
    private int iso;
    private int battery;
    private boolean flashActive;

    public PhotoCamera() {
        this("Sony ZV-1", 100, 100, false, 1.0);
    }

    public PhotoCamera(String model, int iso, int battery, boolean flashActive, double currentZoom) {
        super(model);
        if (iso < 100 || iso > 6400) {
            this.iso = 100;
        } else {
            this.iso = iso;
        }

        if (battery < 0 || battery > 100) {
            this.battery = 100;
        } else {
            this.battery = battery;
        }

        this.flashActive = flashActive;

        if (!Double.isFinite(currentZoom) || currentZoom < 1.0 || currentZoom > 10.0) {
            this.currentZoom = 1.0;
        } else {
            this.currentZoom = currentZoom;
        }
    }

    @Override
    public double getMinZoom() {
        return 1.0;
    }

    @Override
    public double getMaxZoom() {
        return 10.0;
    }

    public void recharge() {
        this.battery = 100;
    }

    @Override
    public void takePhoto() {
        if (!"фото".equalsIgnoreCase(getMode())) {
            throw new IllegalStateException("Ошибка съемки: зеркалка находится в режиме видео!");
        }
        if (battery < 5) {
            throw new IllegalStateException("Ошибка: батарея зеркалки разряжена для съемки!");
        }

        battery -= 1;
        String fileName = "photo_raw_" + (getImages().size() + 1) + ".cr3";
        Image photo = new Image(fileName, "6000x4000", 25.0, "CR3");
        addImage(photo);
    }

    @Override
    public void recordVideo(int seconds) {
        if (!"видео".equalsIgnoreCase(getMode())) {
            throw new IllegalStateException("Ошибка записи: зеркалка находится в режиме фото!");
        }
        if (seconds <= 0) {
            throw new IllegalStateException("Ошибка: длительность записи должна быть больше 0 сек!");
        }
        if (battery < 10) {
            throw new IllegalStateException("Ошибка: недостаточно заряда для записи видео!");
        }

        battery -= 5;
    }

    @Override
    public void zoom(double factor) {
        if (!Double.isFinite(factor) || factor < getMinZoom() || factor > getMaxZoom()) {
            throw new IllegalArgumentException("Ошибка: оптический объектив поддерживает зум только от "
                    + getMinZoom() + "x до " + getMaxZoom() + "x!");
        }
        this.currentZoom = factor;
    }

    public void randomizeIso() {
        Random random = new Random();
        int randomVal = (random.nextInt(64) + 1) * 100;
        this.iso = randomVal;
    }

    public void toggleFlash() {
        this.flashActive = !this.flashActive;
    }

    public int getIso() {
        return iso;
    }

    public boolean setIso(int iso) {
        if (iso < 100 || iso > 6400) {
            throw new IllegalArgumentException("Ошибка: ISO должно быть от 100 до 6400!");
        }
        this.iso = iso;
        return true;
    }

    public int getBattery() {
        return battery;
    }

    public boolean setBattery(int battery) {
        if (battery < 0 || battery > 100) {
            throw new IllegalArgumentException("Ошибка: уровень заряда должен быть от 0 до 100%!");
        }
        this.battery = battery;
        return true;
    }

    public boolean isFlashActive() {
        return flashActive;
    }
}
