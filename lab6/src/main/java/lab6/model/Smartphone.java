package lab6.model;

import java.util.Random;

public class Smartphone extends Camera {
    public static final String TYPE = "Smartphone";
    public static final double MIN_MEGAPIXELS = 2.0;
    public static final double MAX_MEGAPIXELS = 200.0;
    public static final double DEFAULT_MEGAPIXELS = 48.0;
    public static final double DEFAULT_STORAGE = 8192.0;
    public static final double MIN_ZOOM = 0.5;
    public static final double MAX_ZOOM = 50.0;
    private static final double PHOTO_SIZE_MB = 5.0;
    private static final int VIDEO_MB_PER_SECOND = 4;

    private double megapixels;
    private double storage;
    private boolean nightMode;

    public Smartphone() {
        this("Iphone 11", DEFAULT_MEGAPIXELS, DEFAULT_STORAGE, false, DEFAULT_ZOOM);
    }

    public Smartphone(String model, double megapixels, double storage, boolean nightMode, double currentZoom) {
        super(model);
        if (!Double.isFinite(megapixels) || megapixels < MIN_MEGAPIXELS || megapixels > MAX_MEGAPIXELS) {
            this.megapixels = DEFAULT_MEGAPIXELS;
        } else {
            this.megapixels = megapixels;
        }

        if (!Double.isFinite(storage) || storage < 0) {
            this.storage = DEFAULT_STORAGE;
        } else {
            this.storage = storage;
        }

        this.nightMode = nightMode;
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

    public void clearStorage() {
        this.storage = DEFAULT_STORAGE;
    }

    @Override
    public void takePhoto() {
        if (!PHOTO_MODE.equals(getMode())) {
            throw new IllegalStateException("Ошибка съемки: камера смартфона сейчас в режиме видео!");
        }
        if (!Double.isFinite(storage) || storage < PHOTO_SIZE_MB) {
            throw new IllegalStateException("Ошибка: недостаточно памяти в смартфоне для сохранения фото!");
        }

        storage -= PHOTO_SIZE_MB;
        String fileName = "phone_photo_" + (getImages().size() + 1) + ".jpg";
        Image image = new Image(fileName, megapixels + " Мп", PHOTO_SIZE_MB, "JPG");
        addImage(image);
    }

    @Override
    public void recordVideo(int seconds) {
        if (!VIDEO_MODE.equals(getMode())) {
            throw new IllegalStateException("Ошибка записи: камера смартфона сейчас в режиме фото!");
        }
        checkVideoDuration(seconds);

        long requiredMb = (long) seconds * VIDEO_MB_PER_SECOND;
        if (!Double.isFinite(storage) || storage < requiredMb) {
            throw new IllegalStateException("Ошибка: для записи " + seconds + " сек. видео требуется " + requiredMb + " МБ! Не хватает памяти.");
        }

        storage -= requiredMb;
    }

    @Override
    public void zoom(double factor) {
        if (!Double.isFinite(factor) || factor < getMinZoom() || factor > getMaxZoom()) {
            throw new IllegalArgumentException("Ошибка: зум смартфона поддерживает значения от "
                    + getMinZoom() + "x (ультраширик) до " + getMaxZoom() + "x!");
        }
        this.currentZoom = factor;
    }

    public void randomizeStorage() {
        Random random = new Random();
        double randomVal = 500.0 + random.nextDouble() * 7500.0;
        this.storage = Math.round(randomVal * 10.0) / 10.0;
    }

    public void toggleNightMode() {
        this.nightMode = !this.nightMode;
    }

    public double getMegapixels() {
        return megapixels;
    }

    public boolean setMegapixels(double megapixels) {
        if (!Double.isFinite(megapixels) || megapixels < MIN_MEGAPIXELS || megapixels > MAX_MEGAPIXELS) {
            throw new IllegalArgumentException("Ошибка: мегапиксели должны быть от 2 до 200!");
        }
        this.megapixels = megapixels;
        return true;
    }

    public double getStorage() {
        return storage;
    }

    public boolean setStorage(double storage) {
        if (!Double.isFinite(storage) || storage < 0) {
            throw new IllegalArgumentException("Ошибка: свободная память не может быть меньше 0!");
        }
        this.storage = storage;
        return true;
    }

    public boolean isNightMode() {
        return nightMode;
    }
}
