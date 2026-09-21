package lab2;

public class lab2 {

    public static void main(String[] args) {
        WebCamera defaultCamera = new WebCamera();
        WebCamera cam = new WebCamera("Logitech C270 / C310", 640, 90);
        WebCamera[] cams = new WebCamera[]{defaultCamera, cam};
        WebCamera bestCam = getBestCamera(cams);
        String model = bestCam.getModel();
        System.out.println(model);
    }

    public static WebCamera getBestCamera(WebCamera[] cams) {
        WebCamera bestCam = cams[0];
        int bestScore = bestCam.countCharacteristics();
        for (int i = 1; i < cams.length; i++) {
            int score = cams[i].countCharacteristics();
            if (score > bestScore) {
                bestScore = score;
                bestCam = cams[i];
            }
        }
        
        return bestCam;
    }
}
