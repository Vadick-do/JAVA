package lab6.controller;

import lab6.io.CamerasCsvWriter;
import lab6.io.CamerasFileManager;
import lab6.model.Camera;
import lab6.model.FileReadResult;
import lab6.model.PhotoCamera;
import lab6.model.Smartphone;
import lab6.model.WebCamera;
import lab6.view.CommandLineOptions;
import lab6.view.ConsoleView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class CameraController {
    private final List<Camera> cams;
    private final ConsoleView view;
    private final CamerasFileManager fileManager;
    private String dataFileName;

    public CameraController(ConsoleView view) {
        this.view = view;
        this.cams = new ArrayList<>();
        this.fileManager = new CamerasFileManager();
        this.dataFileName = fileManager.getDefaultFileName();
    }

    public int run(String[] args) {
        try {
            CommandLineOptions options = view.readArguments(args, dataFileName);
            switch (options.getAction()) {
                case CommandLineOptions.HELP:
                    view.showHelp(dataFileName);
                    break;
                case CommandLineOptions.INTERACTIVE:
                    dataFileName = options.getFileName();
                    runInteractive();
                    break;
                case CommandLineOptions.SHOW:
                    showCamerasFromFile(options.getFileName());
                    break;
                case CommandLineOptions.WATCH:
                    watchCamerasFromFile(options.getFileName(), options.getInterval());
                    break;
                case CommandLineOptions.LOG:
                    logCamerasToCsv(options.getFileName(), options.getCsvFileName(), options.getInterval());
                    break;
                case CommandLineOptions.REPLACE:
                    fileManager.replaceFile(options.getSourceName(), options.getTargetName());
                    view.showMessage("Файл '" + options.getTargetName() + "' заменен содержимым файла '" + options.getSourceName() + "'.");
                    break;
            }
            return 0;
        } catch (IllegalArgumentException e) {
            view.showMessage("Ошибка: " + e.getMessage());
            view.showHelp(dataFileName);
            return 2;
        } catch (IOException e) {
            view.showMessage("Ошибка ввода-вывода: " + e.getMessage());
            return 1;
        }
    }

    private void runInteractive() {
        cams.add(new WebCamera("Logitech C270", 1280.0, 30.0));
        cams.add(new PhotoCamera("Canon EOS R6", 400, 90, false, 2.0));
        cams.add(new Smartphone("IPhone 15 Pro", 48.0, 5120.0, false, Camera.DEFAULT_ZOOM));

        try {
            runMenu();
        } catch (NoSuchElementException e) {
            view.showMessage("Ввод завершен. Выход из программы.");
        }
    }

    private void runMenu() {
        boolean isRunning = true;
        while (isRunning) {
            view.showMenu(dataFileName);
            String choice = view.readLine();

            switch (choice) {
                case "0":
                    view.showMessage("Выход из программы.");
                    isRunning = false;
                    break;
                case "1":
                    view.printCameras(cams);
                    break;
                case "2":
                    addCamera();
                    break;
                case "3":
                    editCamera();
                    break;
                case "4":
                    deleteCamera();
                    break;
                case "5":
                    workWithCamera();
                    break;
                case "6":
                    captureAll();
                    break;
                case "7":
                    loadCamerasFromFile(dataFileName);
                    break;
                case "8":
                    changeFileName();
                    break;
                default:
                    view.showMessage("Такого пункта в меню нет!");
            }
        }
    }

    public void addCamera() {
        int typeChoice = view.readCameraType();

        String model = view.readString("Введите название модели: ");

        switch (typeChoice) {
            case 1:
                double width = view.readDouble("Введите ширину разрешения (320 - 3840): ", WebCamera.MIN_WIDTH, WebCamera.MAX_WIDTH);
                double fps = view.readDouble("Введите FPS (1 - 120): ", WebCamera.MIN_FPS, WebCamera.MAX_FPS);
                cams.add(new WebCamera(model, width, fps));
                view.showMessage("Веб-камера успешно добавлена!");
                break;

            case 2:
                int iso = view.readInt("Введите ISO (100 - 6400): ", PhotoCamera.MIN_ISO, PhotoCamera.MAX_ISO);
                int battery = view.readInt("Введите заряд батареи % (0 - 100): ", PhotoCamera.MIN_BATTERY, PhotoCamera.MAX_BATTERY);
                cams.add(new PhotoCamera(model, iso, battery, false, Camera.DEFAULT_ZOOM));
                view.showMessage("Зеркалка успешно добавлена!");
                break;

            case 3:
                double mp = view.readDouble("Введите мегапиксели (2 - 200): ", Smartphone.MIN_MEGAPIXELS, Smartphone.MAX_MEGAPIXELS);
                double storage = view.readDouble("Введите объем свободной памяти МБ (от 0, можно дробное): ", 0, Double.MAX_VALUE);
                cams.add(new Smartphone(model, mp, storage, false, Camera.DEFAULT_ZOOM));
                view.showMessage("Смартфон успешно добавлен!");
                break;
        }
    }

    public void editCamera() {
        if (cams.isEmpty()) {
            view.showMessage("Список пуст.");
            return;
        }

        int index = view.readCameraIndex(cams, "\nВведите номер устройства для редактирования: ");

        Camera cam = cams.get(index);
        view.showMessage("(Нажмите Enter без ввода, чтобы оставить текущее значение)");

        if (cam instanceof WebCamera wc) {
            wc.setModel(view.readString("Новая модель [" + wc.getModel() + "]: ", wc.getModel()));
            wc.setWidth(view.readDouble("Новая ширина (320 - 3840) [" + wc.getWidth() + "]: ", WebCamera.MIN_WIDTH, WebCamera.MAX_WIDTH, wc.getWidth()));
            wc.setFPS(view.readDouble("Новый FPS (1 - 120) [" + wc.getFPS() + "]: ", WebCamera.MIN_FPS, WebCamera.MAX_FPS, wc.getFPS()));
        } else if (cam instanceof PhotoCamera pc) {
            pc.setModel(view.readString("Новая модель [" + pc.getModel() + "]: ", pc.getModel()));
            pc.setIso(view.readInt("Новое ISO (100 - 6400) [" + pc.getIso() + "]: ", PhotoCamera.MIN_ISO, PhotoCamera.MAX_ISO, pc.getIso()));
            pc.setBattery(view.readInt("Новый заряд батареи % (0 - 100) [" + pc.getBattery() + "]: ", PhotoCamera.MIN_BATTERY, PhotoCamera.MAX_BATTERY, pc.getBattery()));
        } else if (cam instanceof Smartphone sp) {
            sp.setModel(view.readString("Новая модель [" + sp.getModel() + "]: ", sp.getModel()));
            sp.setMegapixels(view.readDouble("Новые Мп (2 - 200) [" + sp.getMegapixels() + "]: ", Smartphone.MIN_MEGAPIXELS, Smartphone.MAX_MEGAPIXELS, sp.getMegapixels()));
            sp.setStorage(view.readDouble("Новая память МБ [" + sp.getStorage() + "]: ", 0, Double.MAX_VALUE, sp.getStorage()));
        }
        view.showMessage("Параметры успешно обновлены!");
    }

    public void deleteCamera() {
        if (cams.isEmpty()) {
            view.showMessage("Список пуст. Удалять нечего.");
            return;
        }

        int index = view.readCameraIndex(cams, "\nВведите номер устройства для удаления: ");
        cams.remove(index);
        view.showMessage("Устройство успешно удалено!");
    }

    public void workWithCamera() {
        if (cams.isEmpty()) {
            view.showMessage("Список устройств пуст.");
            return;
        }

        int index = view.readCameraIndex(cams, "\nВыберите номер устройства для работы: ");

        Camera cam = cams.get(index);
        boolean inDeviceMenu = true;

        while (inDeviceMenu) {
            view.showDeviceMenu(cam);

            String action = view.readLine();
            try {
                switch (action) {
                    case "0":
                        inDeviceMenu = false;
                        break;
                    case "1":
                        cam.takePhoto();
                        view.showPhotoTaken(cam);
                        break;
                    case "2":
                        int sec = view.readInt("Введите длительность видео (сек): ", 1, Camera.MAX_VIDEO_SECONDS);
                        cam.recordVideo(sec);
                        view.showVideoRecorded(cam, sec);
                        break;
                    case "3":
                        double factor = view.readDouble("Введите коэффициент зума (" + cam.getMinZoom() + " - " + cam.getMaxZoom() + "): ", cam.getMinZoom(), cam.getMaxZoom());
                        cam.zoom(factor);
                        view.showZoomChanged(cam);
                        break;
                    case "4":
                        cam.switchMode();
                        view.showModeChanged(cam);
                        break;
                    case "5":
                        if (cam instanceof WebCamera wc) {
                            wc.randomizeFps();
                        } else if (cam instanceof PhotoCamera pc) {
                            pc.randomizeIso();
                        } else if (cam instanceof Smartphone sp) {
                            sp.randomizeStorage();
                        }
                        view.showRandomCharacteristic(cam);
                        break;
                    case "6":
                        if (cam instanceof WebCamera wc) {
                            wc.toggleMicrophone();
                        } else if (cam instanceof PhotoCamera pc) {
                            pc.toggleFlash();
                        } else if (cam instanceof Smartphone sp) {
                            sp.toggleNightMode();
                        }
                        view.showSpecialAction(cam);
                        break;
                    case "7":
                        view.printImages(cam);
                        break;
                    case "8":
                        if (cam instanceof PhotoCamera pc) {
                            pc.recharge();
                            view.showDeviceReset(cam);
                        } else if (cam instanceof Smartphone sp) {
                            sp.clearStorage();
                            view.showDeviceReset(cam);
                        } else if (cam instanceof WebCamera) {
                            view.showDeviceReset(cam);
                        } else {
                            view.showMessage("Неверное действие!");
                        }
                        break;
                    default:
                        view.showMessage("Неверное действие!");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showMessage(e.getMessage());
            }
        }
    }

    public void captureAll() {
        if (cams.isEmpty()) {
            view.showMessage("Нет подключенных устройств.");
            return;
        }

        view.showMessage("\nСинхронная съемка со всех устройств:");
        for (Camera cam : cams) {
            if (!Camera.PHOTO_MODE.equals(cam.getMode())) {
                cam.switchMode();
                view.showModeChanged(cam);
            }
            try {
                cam.takePhoto();
                view.showPhotoTaken(cam);
            } catch (IllegalStateException e) {
                view.showMessage(e.getMessage());
            }
        }
    }

    private void loadCamerasFromFile(String fileName) {
        view.showMessage("\nПопытка чтения данных из файла: " + fileName);

        try {
            FileReadResult result = fileManager.readFile(fileName);

            if (!result.getCams().isEmpty()) {
                cams.addAll(result.getCams());
                view.showMessage("Успешно добавлено устройств в программу: " + result.getCams().size());
            } else {
                view.showMessage("Объектов для добавления не найдено.");
            }

            view.printReport(result);
        } catch (IOException e) {
            view.showMessage("Ошибка ввода-вывода при чтении файла: " + e.getMessage());
        }
    }

    private void showCamerasFromFile(String fileName) throws IOException {
        FileReadResult result = fileManager.readFile(fileName);
        cams.clear();
        cams.addAll(result.getCams());
        view.printCameras(cams);
        view.printReport(result);
    }

    private void watchCamerasFromFile(String fileName, int interval) {
        view.showWatchStart(fileName, interval);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                showCamerasFromFile(fileName);
            } catch (IOException e) {
                view.showMessage("Ошибка ввода-вывода при чтении файла: " + e.getMessage());
            }

            try {
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void logCamerasToCsv(String fileName, String csvFileName, int interval) throws IOException {
        fileManager.checkLogFile(fileName, csvFileName);
        try (CamerasCsvWriter csvWriter = new CamerasCsvWriter(csvFileName)) {
            view.showLogStart(fileName, csvFileName, interval);
            while (!Thread.currentThread().isInterrupted()) {
                FileReadResult result = null;
                try {
                    fileManager.checkLogFile(fileName, csvFileName);
                    result = fileManager.readFile(fileName);
                } catch (IOException e) {
                    view.showMessage("Ошибка ввода-вывода при чтении файла: " + e.getMessage());
                }

                if (result != null) {
                    int changes = csvWriter.writeChanges(result.getCams());
                    view.printReport(result);
                    view.showMessage("Записано значений в CSV: " + changes);
                }

                try {
                    Thread.sleep(interval * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void changeFileName() {
        view.showMessage("\nТекущее имя файла: " + dataFileName);
        view.showPrompt("Введите новое имя файла (или оставьте пустым для отмены): ");
        String newName = view.readLine();
        if (newName.isEmpty()) {
            view.showMessage("Имя файла не изменено.");
            return;
        }
        view.showMessage("Имя файла изменено на: " + newName);
        dataFileName = newName;
    }

}
