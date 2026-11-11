package lab6.client.controller;

import lab6.client.CameraClient;
import lab6.client.io.CamerasCsvWriter;
import lab6.client.view.CommandLineOptions;
import lab6.client.view.ConsoleView;
import lab6.model.Camera;
import lab6.model.FileReadResult;
import lab6.model.PhotoCamera;
import lab6.model.Smartphone;
import lab6.model.WebCamera;
import lab6.net.TcpProtocol;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

public class CameraController {
    private final ConsoleView view;
    private final CameraClient client;
    private String dataFileName;

    public CameraController(ConsoleView view, CameraClient client) {
        this.view = view;
        this.client = client;
        this.dataFileName = TcpProtocol.DEFAULT_FILE;
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
                    client.execute(TcpProtocol.REPLACE, options.getSourceName(), options.getTargetName());
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

            try {
                switch (choice) {
                    case "0":
                        view.showMessage("Выход из программы.");
                        isRunning = false;
                        break;
                    case "1":
                        view.printCameras(client.getCameras());
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
            } catch (IOException e) {
                view.showMessage("Ошибка ввода-вывода: " + e.getMessage());
            }
        }
    }

    public void addCamera() throws IOException {
        int typeChoice = view.readCameraType();

        String model = view.readString("Введите название модели: ");

        switch (typeChoice) {
            case 1:
                double width = view.readDouble("Введите ширину разрешения (320 - 3840): ", WebCamera.MIN_WIDTH, WebCamera.MAX_WIDTH);
                double fps = view.readDouble("Введите FPS (1 - 120): ", WebCamera.MIN_FPS, WebCamera.MAX_FPS);
                client.execute(TcpProtocol.ADD, WebCamera.TYPE, model, width, fps);
                view.showMessage("Веб-камера успешно добавлена!");
                break;

            case 2:
                int iso = view.readInt("Введите ISO (100 - 6400): ", PhotoCamera.MIN_ISO, PhotoCamera.MAX_ISO);
                int battery = view.readInt("Введите заряд батареи % (0 - 100): ", PhotoCamera.MIN_BATTERY, PhotoCamera.MAX_BATTERY);
                client.execute(TcpProtocol.ADD, PhotoCamera.TYPE, model, iso, battery);
                view.showMessage("Зеркалка успешно добавлена!");
                break;

            case 3:
                double mp = view.readDouble("Введите мегапиксели (2 - 200): ", Smartphone.MIN_MEGAPIXELS, Smartphone.MAX_MEGAPIXELS);
                double storage = view.readDouble("Введите объем свободной памяти МБ (от 0, можно дробное): ", 0, Double.MAX_VALUE);
                client.execute(TcpProtocol.ADD, Smartphone.TYPE, model, mp, storage);
                view.showMessage("Смартфон успешно добавлен!");
                break;
        }
    }

    public void editCamera() throws IOException {
        List<Camera> cams = client.getCameras();
        if (cams.isEmpty()) {
            view.showMessage("Список пуст.");
            return;
        }

        int index = view.readCameraIndex(cams, "\nВведите номер устройства для редактирования: ");

        Camera cam = cams.get(index);
        view.showMessage("(Нажмите Enter без ввода, чтобы оставить текущее значение)");

        if (cam instanceof WebCamera wc) {
            String model = view.readString("Новая модель [" + wc.getModel() + "]: ", wc.getModel());
            double width = view.readDouble("Новая ширина (320 - 3840) [" + wc.getWidth() + "]: ", WebCamera.MIN_WIDTH, WebCamera.MAX_WIDTH, wc.getWidth());
            double fps = view.readDouble("Новый FPS (1 - 120) [" + wc.getFPS() + "]: ", WebCamera.MIN_FPS, WebCamera.MAX_FPS, wc.getFPS());
            client.execute(TcpProtocol.EDIT, cam.getId(), model, width, fps);
        } else if (cam instanceof PhotoCamera pc) {
            String model = view.readString("Новая модель [" + pc.getModel() + "]: ", pc.getModel());
            int iso = view.readInt("Новое ISO (100 - 6400) [" + pc.getIso() + "]: ", PhotoCamera.MIN_ISO, PhotoCamera.MAX_ISO, pc.getIso());
            int battery = view.readInt("Новый заряд батареи % (0 - 100) [" + pc.getBattery() + "]: ", PhotoCamera.MIN_BATTERY, PhotoCamera.MAX_BATTERY, pc.getBattery());
            client.execute(TcpProtocol.EDIT, cam.getId(), model, iso, battery);
        } else if (cam instanceof Smartphone sp) {
            String model = view.readString("Новая модель [" + sp.getModel() + "]: ", sp.getModel());
            double megapixels = view.readDouble("Новые Мп (2 - 200) [" + sp.getMegapixels() + "]: ", Smartphone.MIN_MEGAPIXELS, Smartphone.MAX_MEGAPIXELS, sp.getMegapixels());
            double storage = view.readDouble("Новая память МБ [" + sp.getStorage() + "]: ", 0, Double.MAX_VALUE, sp.getStorage());
            client.execute(TcpProtocol.EDIT, cam.getId(), model, megapixels, storage);
        }
        view.showMessage("Параметры успешно обновлены!");
    }

    public void deleteCamera() throws IOException {
        List<Camera> cams = client.getCameras();
        if (cams.isEmpty()) {
            view.showMessage("Список пуст. Удалять нечего.");
            return;
        }

        int index = view.readCameraIndex(cams, "\nВведите номер устройства для удаления: ");
        client.execute(TcpProtocol.DELETE, cams.get(index).getId());
        view.showMessage("Устройство успешно удалено!");
    }

    public void workWithCamera() throws IOException {
        List<Camera> cams = client.getCameras();
        if (cams.isEmpty()) {
            view.showMessage("Список устройств пуст.");
            return;
        }

        int index = view.readCameraIndex(cams, "\nВыберите номер устройства для работы: ");

        Camera cam = cams.get(index);
        int id = cam.getId();
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
                        client.execute(TcpProtocol.PHOTO, id);
                        cam = client.getCamera(id);
                        view.showPhotoTaken(cam);
                        break;
                    case "2":
                        int sec = view.readInt("Введите длительность видео (сек): ", 1, Camera.MAX_VIDEO_SECONDS);
                        client.execute(TcpProtocol.VIDEO, id, sec);
                        cam = client.getCamera(id);
                        view.showVideoRecorded(cam, sec);
                        break;
                    case "3":
                        double factor = view.readDouble("Введите коэффициент зума (" + cam.getMinZoom() + " - " + cam.getMaxZoom() + "): ", cam.getMinZoom(), cam.getMaxZoom());
                        client.execute(TcpProtocol.ZOOM, id, factor);
                        cam = client.getCamera(id);
                        view.showZoomChanged(cam);
                        break;
                    case "4":
                        client.execute(TcpProtocol.MODE, id);
                        cam = client.getCamera(id);
                        view.showModeChanged(cam);
                        break;
                    case "5":
                        client.execute(TcpProtocol.RANDOMIZE, id);
                        cam = client.getCamera(id);
                        view.showRandomCharacteristic(cam);
                        break;
                    case "6":
                        client.execute(TcpProtocol.TOGGLE, id);
                        cam = client.getCamera(id);
                        view.showSpecialAction(cam);
                        break;
                    case "7":
                        cam = client.getCamera(id);
                        view.printImages(cam);
                        break;
                    case "8":
                        client.execute(TcpProtocol.RESET, id);
                        cam = client.getCamera(id);
                        view.showDeviceReset(cam);
                        break;
                    default:
                        view.showMessage("Неверное действие!");
                }
            } catch (IOException e) {
                view.showMessage(e.getMessage());
            }
        }
    }

    public void captureAll() throws IOException {
        List<Camera> cams = client.getCameras();
        if (cams.isEmpty()) {
            view.showMessage("Нет подключенных устройств.");
            return;
        }

        view.showMessage("\nСинхронная съемка со всех устройств:");
        for (Camera cam : cams) {
            int id = cam.getId();
            try {
                if (!Camera.PHOTO_MODE.equals(cam.getMode())) {
                    client.execute(TcpProtocol.MODE, id);
                    view.showModeChanged(client.getCamera(id));
                }
                client.execute(TcpProtocol.PHOTO, id);
                view.showPhotoTaken(client.getCamera(id));
            } catch (IOException e) {
                view.showMessage(e.getMessage());
            }
        }
    }

    private void loadCamerasFromFile(String fileName) {
        view.showMessage("\nПопытка чтения данных из файла: " + fileName);

        try {
            FileReadResult result = client.loadCameras(fileName);

            if (!result.getCams().isEmpty()) {
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
        FileReadResult result = client.readCameras(fileName);
        view.printCameras(result.getCams());
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
        CamerasCsvWriter.checkDataFile(fileName, csvFileName);
        try (CamerasCsvWriter csvWriter = new CamerasCsvWriter(csvFileName)) {
            view.showLogStart(fileName, csvFileName, interval);
            while (!Thread.currentThread().isInterrupted()) {
                FileReadResult result = null;
                try {
                    CamerasCsvWriter.checkDataFile(fileName, csvFileName);
                    result = client.readCameras(fileName);
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
