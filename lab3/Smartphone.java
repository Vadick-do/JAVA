package lab3;

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
        if (megapixels < 2.0 || megapixels > 200.0) {
            System.out.println("Ошибка: мегапиксели должны быть от 2 до 200. Установлено 48.");
            this.megapixels = 48;
        } else {
            this.megapixels = megapixels;
        }

        if (storage < 0) {
            System.out.println("Ошибка: свободная память не может быть отрицательной. Установлено 8192 МБ.");
            this.storage = 8192.0;
        } else {
            this.storage = storage;
        }

        this.nightMode = nightMode;
        if (currentZoom < 0.5 || currentZoom > 50.0) {
            System.out.println("Ошибка: зум смартфона должен быть в диапазоне от 0.5 до 50.0, установлено 1.0");
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
        System.out.println("Память смартфона [" + getModel() + "] очищена (свободно 8192.0 МБ).");
    }

    @Override
    public void takePhoto() {
        if (!"фото".equalsIgnoreCase(getMode())) {
            System.out.println("Ошибка съемки: камера смартфона сейчас в режиме видео!");
            return;
        }
        if (storage < 10) {
            System.out.println("Ошибка: недостаточно памяти в смартфоне для сохранения фото!");
            return;
        }

        storage = Math.round((storage - 5.0) * 10.0) / 10.0; 
        String fileName = "phone_photo_" + (getImages().size() + 1) + ".jpg";
        addImage(fileName);
        System.out.println("Смартфон [" + model + "] сохранил фото " + megapixels + " Мп."
                + " Ночной режим: " + (nightMode ? "вкл" : "выкл")
                + ", Остаток памяти: " + storage + " МБ. Файл: " + fileName);
    }

    @Override
    public void recordVideo(int seconds) {
        if (!"видео".equalsIgnoreCase(getMode())) {
            System.out.println("Ошибка записи: камера смартфона сейчас в режиме фото!");
            return;
        }
        if (seconds <= 0) {
            System.out.println("Ошибка: время видео должно быть больше 0 сек!");
            return;
        }
        if (seconds > 86400) {
            System.out.println("Ошибка: длительность записи не может превышать 86400 сек!");
            return;
        }

        long requiredMb = (long) seconds * 4; 
        if (storage < requiredMb) {
            System.out.println("Ошибка: для записи " + seconds + " сек. видео требуется " + requiredMb + " МБ! Не хватает памяти.");
            return;
        }

        storage = Math.round((storage - requiredMb) * 10.0) / 10.0;
        System.out.println("Смартфон записал видео (" + seconds + " сек). Свободно памяти: " + storage + " МБ");
    }

    @Override
    public void zoom(double factor) {
        if (factor < getMinZoom() || factor > getMaxZoom()) {
            System.out.println("Ошибка: зум смартфона поддерживает значения от " 
                    + getMinZoom() + "x (ультраширик) до " + getMaxZoom() + "x!");
            return;
        }
        this.currentZoom = factor;
        System.out.println("Камера смартфона переключена на зум " + this.currentZoom + "x");
    }

    public void randomizeStorage() {
        Random random = new Random();
        double randomVal = 500.0 + random.nextDouble() * 7500.0;
        this.storage = Math.round(randomVal * 10.0) / 10.0;
        System.out.println("Память смартфона случайно изменена: " + this.storage + " МБ");
    }

    public void toggleNightMode() {
        this.nightMode = !this.nightMode;
        System.out.println("Ночной режим смартфона: " + (nightMode ? "вкл" : "выкл"));
    }

    public double getMegapixels() {
        return megapixels;
    }

    public boolean setMegapixels(double megapixels) {
        if (megapixels < 2 || megapixels > 200) {
            System.out.println("Ошибка: мегапиксели должны быть от 2 до 200!");
            return false;
        }
        this.megapixels = megapixels;
        return true;
    }

    public double getStorage() {
        return storage;
    }

    public boolean setStorage(double storage) {
        if (storage < 0) {
            System.out.println("Ошибка: свободная память не может быть меньше 0!");
            return false;
        }
        this.storage = storage;
        return true;
    }

    public boolean isNightMode() {
        return nightMode;
    }
}
