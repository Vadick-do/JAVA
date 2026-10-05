package lab3;

import java.util.ArrayList;
import java.util.Scanner;

public class lab3 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<Camera> cams = new ArrayList<>();

        cams.add(new WebCamera("Logitech C270", 1280.0, 30.0));
        cams.add(new PhotoCamera("Canon EOS R6", 400, 90, false, 2.0));
        cams.add(new Smartphone("IPhone 15 Pro", 48.0, 5120.0, false, 1.0));

        boolean isRunning = true;
        while (isRunning) {
            System.out.println("\n--- МЕНЮ ---");
            System.out.println("0. Выход");
            System.out.println("1. Просмотр всех устройств");
            System.out.println("2. Добавить устройство");
            System.out.println("3. Изменить свойства устройства");
            System.out.println("4. Удалить устройство");
            System.out.println("5. Функциональная работа с устройством");
            System.out.println("6. Сделать фото на все устройства сразу");
            System.out.print("Выберите действие: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "0":
                    System.out.println("Выход из программы.");
                    isRunning = false;
                    break;
                case "1":
                    printCameras(cams);
                    break;
                case "2":
                    addCamera(cams, scanner);
                    break;
                case "3":
                    editCamera(cams, scanner);
                    break;
                case "4":
                    deleteCamera(cams, scanner);
                    break;
                case "5":
                    workWithCamera(cams, scanner);
                    break;
                case "6":
                    captureAll(cams);
                    break;
                default:
                    System.out.println("Такого пункта в меню нет!");
            }
        }

        scanner.close();
    }

    public static void printCameras(ArrayList<Camera> cams) {
        if (cams.isEmpty()) {
            System.out.println("Список устройств пуст.");
            return;
        }

        System.out.println("\nСписок зарегистрированных устройств:");
        for (int i = 0; i < cams.size(); i++) {
            Camera cam = cams.get(i);
            System.out.print((i + 1) + ". [ID: " + cam.getId() + "] ");

            if (cam instanceof WebCamera wc) {
                System.out.println("[Веб-камера] " + wc.getModel() 
                        + " | Разрешение: " + wc.getWidth() + " px | FPS: " + wc.getFPS() 
                        + " | Режим: " + wc.getMode() + " | Снимков: " + wc.getImages().size());
            } else if (cam instanceof PhotoCamera pc) {
                System.out.println("[Зеркалка] " + pc.getModel() 
                        + " | ISO: " + pc.getIso() + " | Батарея: " + pc.getBattery() + "%" 
                        + " | Режим: " + pc.getMode() + " | Снимков: " + pc.getImages().size());
            } else if (cam instanceof Smartphone sp) {
                System.out.println("[Смартфон] " + sp.getModel() 
                        + " | Матрица: " + sp.getMegapixels() + " Мп | Память: " + sp.getStorage() + " МБ" 
                        + " | Режим: " + sp.getMode() + " | Снимков: " + sp.getImages().size());
            }
        }
    }

    public static void addCamera(ArrayList<Camera> cams, Scanner scanner) {
        System.out.println("\nВыберите тип создаваемого устройства:");
        System.out.println("1. Веб-камера");
        System.out.println("2. Фотокамера (зеркалка)");
        System.out.println("3. Смартфон");
        System.out.print("Ваш выбор: ");
        String typeChoice = scanner.nextLine().trim();

        if (!typeChoice.equals("1") && !typeChoice.equals("2") && !typeChoice.equals("3")) {
            System.out.println("Неверный тип устройства!");
            return;
        }

        String model = readString(scanner, "Введите название модели: ");

        switch (typeChoice) {
            case "1":
                double width = readDouble(scanner, "Введите ширину разрешения (320 - 3840): ", 320, 3840);
                double fps = readDouble(scanner, "Введите FPS (1 - 120): ", 1, 120);
                cams.add(new WebCamera(model, width, fps));
                System.out.println("Веб-камера успешно добавлена!");
                break;

            case "2":
                int iso = readInt(scanner, "Введите ISO (100 - 6400): ", 100, 6400);
                int battery = readInt(scanner, "Введите заряд батареи % (0 - 100): ", 0, 100);
                cams.add(new PhotoCamera(model, iso, battery, false, 1.0));
                System.out.println("Зеркалка успешно добавлена!");
                break;

            case "3":
                double mp = readDouble(scanner, "Введите мегапиксели (2 - 200): ", 2, 200);
                double storage = readDouble(scanner, "Введите объем свободной памяти МБ (от 0, можно дробное): ", 0, 1_000_000);
                cams.add(new Smartphone(model, mp, storage, false, 1.0));
                System.out.println("Смартфон успешно добавлен!");
                break;
        }
    }

    public static void editCamera(ArrayList<Camera> cams, Scanner scanner) {
        if (cams.isEmpty()) {
            System.out.println("Список пуст.");
            return;
        }

        printCameras(cams);
        System.out.print("\nВведите номер устройства для редактирования: ");
        String indexStr = scanner.nextLine().trim();
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Некорректный ввод!");
            return;
        }

        if (index < 0 || index >= cams.size()) {
            System.out.println("Устройства с таким номером нет!");
            return;
        }

        Camera cam = cams.get(index);

        if (cam instanceof WebCamera wc) {
            wc.setModel(readString(scanner, "Новая модель (текущая: " + wc.getModel() + "): "));
            wc.setWidth(readDouble(scanner, "Новая ширина (320 - 3840): ", 320, 3840));
            wc.setFPS(readDouble(scanner, "Новый FPS (1 - 120): ", 1, 120));
        } else if (cam instanceof PhotoCamera pc) {
            pc.setModel(readString(scanner, "Новая модель (текущая: " + pc.getModel() + "): "));
            pc.setIso(readInt(scanner, "Новое ISO (100 - 6400): ", 100, 6400));
            pc.setBattery(readInt(scanner, "Новый заряд батареи % (0 - 100): ", 0, 100));
        } else if (cam instanceof Smartphone sp) {
            sp.setModel(readString(scanner, "Новая модель (текущая: " + sp.getModel() + "): "));
            sp.setMegapixels(readDouble(scanner, "Новые Мп (2 - 200): ", 2, 200));
            sp.setStorage(readDouble(scanner, "Новая память МБ (можно дробное): ", 0, 1_000_000));
        }
        System.out.println("Параметры успешно обновлены!");
    }

    public static void deleteCamera(ArrayList<Camera> cams, Scanner scanner) {
        if (cams.isEmpty()) {
            System.out.println("Список пуст. Удалять нечего.");
            return;
        }

        printCameras(cams);
        System.out.print("\nВведите номер устройства для удаления: ");
        String indexStr = scanner.nextLine().trim();
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Некорректный ввод!");
            return;
        }

        if (index >= 0 && index < cams.size()) {
            cams.remove(index);
            System.out.println("Устройство успешно удалено!");
        } else {
            System.out.println("Устройства с таким номером нет!");
        }
    }

    public static void workWithCamera(ArrayList<Camera> cams, Scanner scanner) {
        if (cams.isEmpty()) {
            System.out.println("Список устройств пуст.");
            return;
        }

        printCameras(cams);
        System.out.print("\nВыберите номер устройства для работы: ");
        String indexStr = scanner.nextLine().trim();
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Некорректный номер!");
            return;
        }

        if (index < 0 || index >= cams.size()) {
            System.out.println("Устройства с таким номером нет!");
            return;
        }

        Camera cam = cams.get(index);

        String randomCharText = "Изменить случайную характеристику";
        String specialActionText = "Специальное действие устройства";

        if (cam instanceof WebCamera wc) {
            randomCharText = "Изменить случайную характеристику (FPS)";
            specialActionText = "Включить/выключить микрофон (сейчас: " + (wc.isMicrophoneEnabled() ? "вкл" : "выкл") + ")";
        } else if (cam instanceof PhotoCamera pc) {
            randomCharText = "Изменить случайную характеристику (ISO)";
            specialActionText = "Включить/выключить вспышку (сейчас: " + (pc.isFlashActive() ? "вкл" : "выкл") + ")";
        } else if (cam instanceof Smartphone sp) {
            randomCharText = "Изменить случайную характеристику (свободная память)";
            specialActionText = "Включить/выключить ночной режим (сейчас: " + (sp.isNightMode() ? "вкл" : "выкл") + ")";
        }

        System.out.println("\n--- Выберите действие с устройством ---");
        System.out.println("1. Сделать фото");
        System.out.println("2. Записать видео");
        System.out.println("3. Изменить зум");
        System.out.println("4. Переключить режим (фото <-> видео) [текущий: " + cam.getMode() + "]");
        System.out.println("5. " + randomCharText);
        System.out.println("6. " + specialActionText);
        System.out.print("Ваш выбор: ");

        String action = scanner.nextLine().trim();
        switch (action) {
            case "1":
                cam.takePhoto();
                break;
            case "2":
                int sec = readInt(scanner, "Введите длительность видео (сек): ", 1, 86400);
                cam.recordVideo(sec);
                break;
            case "3":
                double factor = readDouble(scanner, "Введите коэффициент зума: ", 0.1, 100.0);
                cam.zoom(factor);
                break;
            case "4":
                cam.switchMode();
                break;
            case "5":
                if (cam instanceof WebCamera wc) {
                    wc.randomizeFps();
                } else if (cam instanceof PhotoCamera pc) {
                    pc.randomizeIso();
                } else if (cam instanceof Smartphone sp) {
                    sp.randomizeStorage();
                }
                break;
            case "6":
                if (cam instanceof WebCamera wc) {
                    wc.toggleMicrophone();
                } else if (cam instanceof PhotoCamera pc) {
                    pc.toggleFlash();
                } else if (cam instanceof Smartphone sp) {
                    sp.toggleNightMode();
                }
                break;
            default:
                System.out.println("Неверное действие!");
        }
    }

    public static void captureAll(ArrayList<Camera> cams) {
        if (cams.isEmpty()) {
            System.out.println("Нет подключенных устройств.");
            return;
        }

        System.out.println("\n[Синхронная съемка со всех устройств]:");
        for (Camera cam : cams) {
            if (!"фото".equalsIgnoreCase(cam.getMode())) {
                cam.switchMode();
            }
            cam.takePhoto(); 
        }
    }

    private static String readString(Scanner scanner, String msg) {
        while (true) {
            System.out.print(msg);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Ошибка! Поле не может быть пустым.");
        }
    }

    private static int readInt(Scanner scanner, String msg, int min, int max) {
        while (true) {
            System.out.print(msg);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }
            if (input.contains(".") || input.contains(",")) {
                System.out.println("Ошибка! Дробные числа не допускаются, введите целое число.");
                continue;
            }
            try {
                int val = Integer.parseInt(input);
                if (val < min) {
                    System.out.println("Ошибка! Значение не может быть меньше " + min + ".");
                } else if (val > max) {
                    System.out.println("Ошибка! Значение не может быть больше " + max + ".");
                } else {
                    return val;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка! Введите корректное целое число.");
            }
        }
    }

    private static double readDouble(Scanner scanner, String msg, double min, double max) {
        while (true) {
            System.out.print(msg);
            String input = scanner.nextLine().trim().replace(',', '.');
            if (input.isEmpty()) {
                continue;
            }
            try {
                double val = Double.parseDouble(input);
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    System.out.println("Ошибка! Введено недопустимое число.");
                } else if (val < min) {
                    System.out.println("Ошибка! Значение не может быть меньше " + min + ".");
                } else if (val > max) {
                    System.out.println("Ошибка! Значение не может быть больше " + max + ".");
                } else {
                    return val;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка! Введите корректное число.");
            }
        }
    }
}