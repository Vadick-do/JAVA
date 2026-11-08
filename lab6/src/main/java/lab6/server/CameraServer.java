package lab6.server;

import lab6.model.Camera;
import lab6.model.FileReadResult;
import lab6.model.PhotoCamera;
import lab6.model.Smartphone;
import lab6.model.WebCamera;
import lab6.server.io.CamerasFileManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraServer {
    private final List<Camera> cams = new ArrayList<>();
    private final CamerasFileManager fileManager = new CamerasFileManager();

    public List<Camera> getCameras() {
        return List.copyOf(cams);
    }

    public void addDemoCameras() {
        cams.add(new WebCamera("Logitech C270", 1280.0, 30.0));
        cams.add(new PhotoCamera("Canon EOS R6", 400, 90, false, 2.0));
        cams.add(new Smartphone("IPhone 15 Pro", 48.0, 5120.0, false, Camera.DEFAULT_ZOOM));
    }

    public void addWebCamera(String model, double width, double fps) {
        cams.add(new WebCamera(model, width, fps));
    }

    public void addPhotoCamera(String model, int iso, int battery) {
        cams.add(new PhotoCamera(model, iso, battery, false, Camera.DEFAULT_ZOOM));
    }

    public void addSmartphone(String model, double megapixels, double storage) {
        cams.add(new Smartphone(model, megapixels, storage, false, Camera.DEFAULT_ZOOM));
    }

    public void editWebCamera(int index, String model, double width, double fps) {
        WebCamera camera = (WebCamera) cams.get(index);
        camera.setModel(model);
        camera.setWidth(width);
        camera.setFPS(fps);
    }

    public void editPhotoCamera(int index, String model, int iso, int battery) {
        PhotoCamera camera = (PhotoCamera) cams.get(index);
        camera.setModel(model);
        camera.setIso(iso);
        camera.setBattery(battery);
    }

    public void editSmartphone(int index, String model, double megapixels, double storage) {
        Smartphone camera = (Smartphone) cams.get(index);
        camera.setModel(model);
        camera.setMegapixels(megapixels);
        camera.setStorage(storage);
    }

    public void deleteCamera(int index) {
        cams.remove(index);
    }

    public void takePhoto(int index) {
        cams.get(index).takePhoto();
    }

    public void recordVideo(int index, int seconds) {
        cams.get(index).recordVideo(seconds);
    }

    public void zoom(int index, double factor) {
        cams.get(index).zoom(factor);
    }

    public void switchMode(int index) {
        cams.get(index).switchMode();
    }

    public void randomizeCharacteristic(int index) {
        Camera cam = cams.get(index);
        if (cam instanceof WebCamera wc) {
            wc.randomizeFps();
        } else if (cam instanceof PhotoCamera pc) {
            pc.randomizeIso();
        } else if (cam instanceof Smartphone sp) {
            sp.randomizeStorage();
        }
    }

    public void toggleSpecialAction(int index) {
        Camera cam = cams.get(index);
        if (cam instanceof WebCamera wc) {
            wc.toggleMicrophone();
        } else if (cam instanceof PhotoCamera pc) {
            pc.toggleFlash();
        } else if (cam instanceof Smartphone sp) {
            sp.toggleNightMode();
        }
    }

    public void resetCamera(int index) {
        Camera cam = cams.get(index);
        if (cam instanceof PhotoCamera pc) {
            pc.recharge();
        } else if (cam instanceof Smartphone sp) {
            sp.clearStorage();
        }
    }

    public String getDefaultFileName() {
        return fileManager.getDefaultFileName();
    }

    public FileReadResult readCameras(String fileName) throws IOException {
        return fileManager.readFile(fileName);
    }

    public FileReadResult loadCameras(String fileName) throws IOException {
        FileReadResult result = readCameras(fileName);
        cams.addAll(result.getCams());
        return result;
    }

    public void checkLogFile(String fileName, String csvFileName) throws IOException {
        fileManager.checkLogFile(fileName, csvFileName);
    }

    public void replaceFile(String sourceName, String targetName) throws IOException {
        fileManager.replaceFile(sourceName, targetName);
    }
}
