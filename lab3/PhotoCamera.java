package lab3;

import java.util.concurrent.ThreadLocalRandom;

public class PhotoCamera extends Camera{
    private String model;
    private int iso;
    private int battery;
    private boolean flashActive;
    private double currentZoom;

    public PhotoCamera() {
        this.model = "Sony ZV-1";
        this.iso = 100;
        this.battery = 100;
        this.flashActive = false;
        this.currentZoom = 1.0;
    }

    public PhotoCamera(String model, int iso, int battery, boolean flashActive, double currentZoom) {
        this.model = model;
        if (iso < 100 || iso > 6400) {
            System.out.println("Ошибка: ISO должно быть от 100 до 6400. Установлено 100.");
            this.iso = 100;
        } else {
            this.iso = iso;
        }

        if (battery < 0 || battery > 100) {
            System.out.println("Ошибка: заряд батареи должен быть от 0 до 100%. Установлено 100%.");
            this.battery = 100;
        } else {
            this.battery = battery;
        }

        this.flashActive = flashActive;

        if (currentZoom < 1.0 || currentZoom > 10.0) {
            System.out.println("Ошибка: зум фотокамеры должен быть в диапазоне от 1.0 до 10.0, установлено 1.0");
            this.currentZoom = 1.0;
        } else {
            this.currentZoom = currentZoom;
        }
    }

    @Override
    public void takePhoto() {
        if (!"фото".equalsIgnoreCase(getMode())) {
            System.out.println("Ошибка съемки: зеркалка находится в режиме видео!");
            return;
        }
        if (battery < 5) {
            System.out.println("Ошибка: батарея зеркалки разряжена для съемки!");
            return;
        }

        battery -= 1; 
        String fileName = "photo_raw_" + (getImages().size() + 1) + ".cr3";
        addImages(fileName);
        System.out.println("Зеркалка [" + model + "] щелкнула затвором! ISO: " + iso 
                + ", Вспышка: " + (flashActive ? "сработала" : "выкл") 
                + ", Батарея: " + battery + "%. Сохранено: " + fileName);
    }

    @Override
    public void recordVideo(int seconds) {
        if (!"видео".equalsIgnoreCase(getMode())) {
            System.out.println("Ошибка записи: зеркалка находится в режиме фото!");
            return;
        }
        if (seconds <= 0) {
            System.out.println("Ошибка: длительность записи должна быть больше 0 сек!");
            return;
        }
        if (battery < 10) {
            System.out.println("Ошибка: недостаточно заряда для записи видео!");
            return;
        }

        battery -= 5; 
        System.out.println("Зеркалка записала видео " + seconds + " сек. Батарея: " + battery + "%");
    }

    @Override
    public void zoom(double factor) {
        if (factor < 1.0 || factor > 10.0) {
            System.out.println("Ошибка: оптический объектив поддерживает зум только от 1.0x до 10.0x!");
            return;
        }
        this.currentZoom = factor;
        System.out.println("Кольцо объектива повернуто. Оптический зум: " + this.currentZoom + "x");
    }

    public void randomizeIso() {
        int randomVal = ThreadLocalRandom.current().nextInt(1, 33) * 100;
        this.iso = randomVal;
        System.out.println("Новое случайное ISO: " + this.iso);
    }

    public void toggleFlash() {
        this.flashActive = !this.flashActive;
        System.out.println("Вспышка зеркалки теперь: " + (flashActive ? "вкл" : "выкл"));
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getIso() {
        return iso;
    }

    public boolean setIso(int iso) {
        if (iso < 100 || iso > 6400) {
            System.out.println("Ошибка: ISO должно быть от 100 до 6400!");
            return false;
        }
        this.iso = iso;
        return true;
    }

    public int getBattery() {
        return battery;
    }

    public boolean setBattery(int battery) {
        if (battery < 0 || battery > 100) {
            System.out.println("Ошибка: уровень заряда должен быть от 0 до 100%!");
            return false;
        }
        this.battery = battery;
        return true;
    }

    public boolean isFlashActive() {
        return flashActive;
    }

    public double getCurrentZoom() {
        return currentZoom;
    }
}
