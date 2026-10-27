package lab5;

import java.util.Random;

public class Smartphone extends Camera {
    private double megapixels;
    private double storage;
    private boolean nightMode;

    public Smartphone() {
        this("Iphone 11", 48.0, 8192.0, false, 1.0);
    }

    public Smartphone(String model, double megapixels, double storage, boolean nightMode, double currentZoom) {
        super(model);
        if (!Double.isFinite(megapixels) || megapixels < 2.0 || megapixels > 200.0) {
            this.megapixels = 48;
        } else {
            this.megapixels = megapixels;
        }

        if (!Double.isFinite(storage) || storage < 0) {
            this.storage = 8192.0;
        } else {
            this.storage = storage;
        }

        this.nightMode = nightMode;
        if (!Double.isFinite(currentZoom) || currentZoom < 0.5 || currentZoom > 50.0) {
            this.currentZoom = 1.0;
        } else {
            this.currentZoom = currentZoom;
        }
    }

    @Override
    public double getMinZoom() {
        return 0.5;
    }

    @Override
    public double getMaxZoom() {
        return 50.0;
    }

    public void clearStorage() {
        this.storage = 8192.0;
    }

    @Override
    public void takePhoto() {
        if (!"фото".equalsIgnoreCase(getMode())) {
            throw new IllegalStateException("Ошибка съемки: камера смартфона сейчас в режиме видео!");
        }
        if (!Double.isFinite(storage) || storage < 10) {
            throw new IllegalStateException("Ошибка: недостаточно памяти в смартфоне для сохранения фото!");
        }

        storage = Math.round((storage - 5.0) * 10.0) / 10.0;
        String fileName = "phone_photo_" + (getImages().size() + 1) + ".jpg";
        Image image = new Image(fileName, megapixels + " Мп", 5.0, "JPG");
        addImage(image);
    }

    @Override
    public void recordVideo(int seconds) {
        if (!"видео".equalsIgnoreCase(getMode())) {
            throw new IllegalStateException("Ошибка записи: камера смартфона сейчас в режиме фото!");
        }
        if (seconds <= 0) {
            throw new IllegalStateException("Ошибка: время видео должно быть больше 0 сек!");
        }
        if (seconds > 86400) {
            throw new IllegalStateException("Ошибка: длительность записи не может превышать 86400 сек!");
        }

        long requiredMb = (long) seconds * 4;
        if (!Double.isFinite(storage) || storage < requiredMb) {
            throw new IllegalStateException("Ошибка: для записи " + seconds + " сек. видео требуется " + requiredMb + " МБ! Не хватает памяти.");
        }

        storage = Math.round((storage - requiredMb) * 10.0) / 10.0;
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
        if (!Double.isFinite(megapixels) || megapixels < 2 || megapixels > 200) {
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
