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

    public static WebCamera getBestCamera(WebCamera[] cams) {
        WebCamera bestCam = cams[0];
        int bestScore = bestCam.width * bestCam.fps;
        for (int i = 1; i < cams.length; i++) {
            int score = cams[i].width * cams[i].fps;
            if (score > bestScore) {
                bestScore = score;
                bestCam = cams[i];
            }
        }
        
        return bestCam;
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
}
