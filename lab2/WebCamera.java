package lab2;

public class WebCamera {
    private String model;
    private int width;
    private int fps;

    public WebCamera() {
        this.model = "Стандартная камера";
        this.width = 1280;
        this.fps = 30;
    }

    public WebCamera(String model, int width, int fps) {
        this.model = model;
        if (width < 320 || width > 3840) {
            System.out.println("Минимальное значение для ширины экрана 320, а максимальное - 3840");
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

    public int getWigth() {
        return width;
    }

    public int getFPS() {
        return fps;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setFPS(int fps) {
        this.fps = fps;
    }

    public int countCharacteristics() {
        return this.width * this.fps;
    }

    public double calculateMegapixels() {
        int height = (this.width * 9) / 16;
        return (this.width * height) / 1_000_000.0;
}
}
