package lab6.model;

import java.util.Random;

public class PhotoCamera extends Camera {
    public static final String TYPE = "PhotoCamera";
    public static final int MIN_ISO = 100;
    public static final int MAX_ISO = 6400;
    public static final int MIN_BATTERY = 0;
    public static final int MAX_BATTERY = 100;
    public static final double MIN_ZOOM = 1.0;
    public static final double MAX_ZOOM = 10.0;
    private static final int PHOTO_BATTERY_COST = 1;
    private static final int VIDEO_BATTERY_COST = 5;

    private int iso;
    private int battery;
    private boolean flashActive;

    public PhotoCamera() {
        this("Sony ZV-1", MIN_ISO, MAX_BATTERY, false, DEFAULT_ZOOM);
    }

    public PhotoCamera(String model, int iso, int battery, boolean flashActive, double currentZoom) {
        super(model);
        if (iso < MIN_ISO || iso > MAX_ISO) {
            this.iso = MIN_ISO;
        } else {
            this.iso = iso;
        }

        if (battery < MIN_BATTERY || battery > MAX_BATTERY) {
            this.battery = MAX_BATTERY;
        } else {
            this.battery = battery;
        }

        this.flashActive = flashActive;

        if (!Double.isFinite(currentZoom) || currentZoom < MIN_ZOOM || currentZoom > MAX_ZOOM) {
            this.currentZoom = DEFAULT_ZOOM;
        } else {
            this.currentZoom = currentZoom;
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

    public void recharge() {
        this.battery = MAX_BATTERY;
    }

    @Override
    public void takePhoto() {
        if (!PHOTO_MODE.equals(getMode())) {
            throw new IllegalStateException("Ошибка съемки: зеркалка находится в режиме видео!");
        }
        if (battery < PHOTO_BATTERY_COST) {
            throw new IllegalStateException("Ошибка: батарея зеркалки разряжена для съемки!");
        }

        battery -= PHOTO_BATTERY_COST;
        String fileName = "photo_raw_" + (getImages().size() + 1) + ".cr3";
        Image photo = new Image(fileName, "6000x4000", 25.0, "CR3");
        addImage(photo);
    }

    @Override
    public void recordVideo(int seconds) {
        if (!VIDEO_MODE.equals(getMode())) {
            throw new IllegalStateException("Ошибка записи: зеркалка находится в режиме фото!");
        }
        checkVideoDuration(seconds);
        if (battery < VIDEO_BATTERY_COST) {
            throw new IllegalStateException("Ошибка: недостаточно заряда для записи видео!");
        }

        battery -= VIDEO_BATTERY_COST;
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
        int randomVal = (random.nextInt(MAX_ISO / MIN_ISO) + 1) * MIN_ISO;
        this.iso = randomVal;
    }

    public void toggleFlash() {
        this.flashActive = !this.flashActive;
    }

    public int getIso() {
        return iso;
    }

    public boolean setIso(int iso) {
        if (iso < MIN_ISO || iso > MAX_ISO) {
            throw new IllegalArgumentException("Ошибка: ISO должно быть от 100 до 6400!");
        }
        this.iso = iso;
        return true;
    }

    public int getBattery() {
        return battery;
    }

    public boolean setBattery(int battery) {
        if (battery < MIN_BATTERY || battery > MAX_BATTERY) {
            throw new IllegalArgumentException("Ошибка: уровень заряда должен быть от 0 до 100%!");
        }
        this.battery = battery;
        return true;
    }

    public boolean isFlashActive() {
        return flashActive;
    }
}
