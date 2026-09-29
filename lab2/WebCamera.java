package lab2;

public class WebCamera {
    private String model;
    private double width;
    private double fps;

    public WebCamera() {
        this.model = "Стандартная камера";
        this.width = 1280.0;
        this.fps = 30.0;
    }

    public WebCamera(String model, double width, double fps) {
        this.model = model;
        if (width < 320 || width > 3840) {
            System.out.println("Минимальное значение для ширины экрана 320 пикселей, а максимальное - 3840 пикселей");
        } else {
            this.width = width;
        }
        if (fps < 1 || fps > 120) {
            System.out.println("Минимальное значение для fps 1, а максимальное - 120");
        } else {
            this.fps = fps;
        }
    }

    public String getModel() {
        return model;
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
}
