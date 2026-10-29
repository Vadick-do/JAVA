package lab5;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public class CameraController {
    private final ArrayList<Camera> cams;
    private final ConsoleView view;
    private final CamerasFileManager fileManager;
    private String dataFileName;

    public CameraController(ConsoleView view) {
        this.view = view;
        this.cams = new ArrayList<>();
        this.fileManager = new CamerasFileManager();
        this.dataFileName = new File("lab5/devices.txt").exists() ? "lab5/devices.txt" : "devices.txt";
    }

    public int run(String[] args) {
        String action = null;
        String fileName = dataFileName;
        String sourceName = null;
        String targetName = null;
        boolean fileSpecified = false;

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help":
                    case "-h":
                    case "--interactive":
                    case "--show":
                    case "--replace":
                        if (action != null) {
                            throw new IllegalArgumentException("Нужно выбрать только один режим работы.");
                        }
                        action = args[i].equals("-h") ? "--help" : args[i];
                        if (action.equals("--replace")) {
                            sourceName = getArgument(args, i + 1, "--replace");
                            targetName = getArgument(args, i + 2, "--replace");
                            i += 2;
                        }
                        break;
                    case "--file":
                        if (fileSpecified) {
                            throw new IllegalArgumentException("Имя файла можно указать только один раз.");
                        }
                        fileName = getArgument(args, i + 1, "--file");
                        fileSpecified = true;
                        i++;
                        break;
                    default:
                        throw new IllegalArgumentException("Неизвестный ключ: " + args[i]);
                }
            }

            if (action == null) {
                if (fileSpecified) {
                    throw new IllegalArgumentException("Для ключа --file нужно выбрать --show или --interactive.");
                }
                view.showHelp(dataFileName);
                return 0;
            }
            if (fileSpecified && !action.equals("--show") && !action.equals("--interactive")) {
                throw new IllegalArgumentException("Ключ --file используется только с --show или --interactive.");
            }

            switch (action) {
                case "--help":
                    view.showHelp(dataFileName);
                    break;
                case "--interactive":
                    dataFileName = fileName;
                    runInteractive();
                    break;
                case "--show":
                    showCamerasFromFile(fileName);
                    break;
                case "--replace":
                    fileManager.replaceFile(sourceName, targetName);
                    view.showMessage("Файл '" + targetName + "' заменен содержимым файла '" + sourceName + "'.");
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

    private String getArgument(String[] args, int index, String key) {
        if (index >= args.length || args[index].isBlank() || args[index].startsWith("--") || args[index].equals("-h")) {
            throw new IllegalArgumentException("Не указан файл для ключа " + key + ".");
        }
        return args[index];
    }

    private void runInteractive() {
        cams.add(new WebCamera("Logitech C270", 1280.0, 30.0));
        cams.add(new PhotoCamera("Canon EOS R6", 400, 90, false, 2.0));
        cams.add(new Smartphone("IPhone 15 Pro", 48.0, 5120.0, false, 1.0));

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
        view.showMessage("\nВыберите тип создаваемого устройства:");
        view.showMessage("1. Веб-камера");
        view.showMessage("2. Фотокамера (зеркалка)");
        view.showMessage("3. Смартфон");
        view.showPrompt("Ваш выбор: ");
        String typeChoice = view.readLine();

        if (!typeChoice.equals("1") && !typeChoice.equals("2") && !typeChoice.equals("3")) {
            view.showMessage("Неверный тип устройства!");
            return;
        }

        String model = view.readString("Введите название модели: ");

        switch (typeChoice) {
            case "1":
                double width = view.readDouble("Введите ширину разрешения (320 - 3840): ", 320, 3840);
                double fps = view.readDouble("Введите FPS (1 - 120): ", 1, 120);
                cams.add(new WebCamera(model, width, fps));
                view.showMessage("Веб-камера успешно добавлена!");
                break;

            case "2":
                int iso = view.readInt("Введите ISO (100 - 6400): ", 100, 6400);
                int battery = view.readInt("Введите заряд батареи % (0 - 100): ", 0, 100);
                cams.add(new PhotoCamera(model, iso, battery, false, 1.0));
                view.showMessage("Зеркалка успешно добавлена!");
                break;

            case "3":
                double mp = view.readDouble("Введите мегапиксели (2 - 200): ", 2, 200);
                double storage = view.readDouble("Введите объем свободной памяти МБ (от 0, можно дробное): ", 0, 1_000_000);
                cams.add(new Smartphone(model, mp, storage, false, 1.0));
                view.showMessage("Смартфон успешно добавлен!");
                break;
        }
    }

    public void editCamera() {
        if (cams.isEmpty()) {
            view.showMessage("Список пуст.");
            return;
        }

        view.printCameras(cams);
        view.showPrompt("\nВведите номер устройства для редактирования: ");
        String indexStr = view.readLine();
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            view.showMessage("Некорректный ввод!");
            return;
        }

        if (index < 0 || index >= cams.size()) {
            view.showMessage("Устройства с таким номером нет!");
            return;
        }

        Camera cam = cams.get(index);
        view.showMessage("(Нажмите Enter без ввода, чтобы оставить текущее значение)");

        if (cam instanceof WebCamera wc) {
            wc.setModel(view.readString("Новая модель [" + wc.getModel() + "]: ", wc.getModel()));
            wc.setWidth(view.readDouble("Новая ширина (320 - 3840) [" + wc.getWidth() + "]: ", 320, 3840, wc.getWidth()));
            wc.setFPS(view.readDouble("Новый FPS (1 - 120) [" + wc.getFPS() + "]: ", 1, 120, wc.getFPS()));
        } else if (cam instanceof PhotoCamera pc) {
            pc.setModel(view.readString("Новая модель [" + pc.getModel() + "]: ", pc.getModel()));
            pc.setIso(view.readInt("Новое ISO (100 - 6400) [" + pc.getIso() + "]: ", 100, 6400, pc.getIso()));
            pc.setBattery(view.readInt("Новый заряд батареи % (0 - 100) [" + pc.getBattery() + "]: ", 0, 100, pc.getBattery()));
        } else if (cam instanceof Smartphone sp) {
            sp.setModel(view.readString("Новая модель [" + sp.getModel() + "]: ", sp.getModel()));
            sp.setMegapixels(view.readDouble("Новые Мп (2 - 200) [" + sp.getMegapixels() + "]: ", 2, 200, sp.getMegapixels()));
            sp.setStorage(view.readDouble("Новая память МБ [" + sp.getStorage() + "]: ", 0, 1_000_000, sp.getStorage()));
        }
        view.showMessage("Параметры успешно обновлены!");
    }

    public void deleteCamera() {
        if (cams.isEmpty()) {
            view.showMessage("Список пуст. Удалять нечего.");
            return;
        }

        view.printCameras(cams);
        view.showPrompt("\nВведите номер устройства для удаления: ");
        String indexStr = view.readLine();
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            view.showMessage("Некорректный ввод!");
            return;
        }

        if (index >= 0 && index < cams.size()) {
            cams.remove(index);
            view.showMessage("Устройство успешно удалено!");
        } else {
            view.showMessage("Устройства с таким номером нет!");
        }
    }

    public void workWithCamera() {
        if (cams.isEmpty()) {
            view.showMessage("Список устройств пуст.");
            return;
        }

        view.printCameras(cams);
        view.showPrompt("\nВыберите номер устройства для работы: ");
        String indexStr = view.readLine();
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            view.showMessage("Некорректный номер!");
            return;
        }

        if (index < 0 || index >= cams.size()) {
            view.showMessage("Устройства с таким номером нет!");
            return;
        }

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
                        int sec = view.readInt("Введите длительность видео (сек): ", 1, 86400);
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
            if (!"фото".equalsIgnoreCase(cam.getMode())) {
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
