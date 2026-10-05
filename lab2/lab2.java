package lab2;

import java.util.ArrayList;
import java.util.Scanner;

public class lab2 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<WebCamera> cams = new ArrayList<>();

        final int num = 3; 

        cams.add(new WebCamera());
        cams.add(new WebCamera("Logitech C270 / C310", 640, 90));
       
        boolean isRunning = true;
        while (isRunning) {
            System.out.println("\n--- МЕНЮ ---");
            System.out.println("0. Выход");
            System.out.println("1. Просмотр всех веб-камер");
            System.out.println("2. Добавить веб-камеру");
            System.out.println("3. Изменить свойства камеры по номеру");
            System.out.println("4. Удалить камеру по номеру");
            System.out.println("5. Найти камеру с наилучшими характеристиками");
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
                    getBestCamera(cams);
                    break;
                default:
                    System.out.println("Такого в меню нет!");
            }
        }
    
        scanner.close();
    }

    public static void getBestCamera(ArrayList<WebCamera> cams) {
        if (cams.isEmpty()) {
            System.out.println("Список камер пуст.");
            return;
        }

        WebCamera bestCam = cams.get(0);
        double bestScore = bestCam.countCharacteristics();
        for (int i = 1; i < cams.size(); i++) {
            double score = cams.get(i).countCharacteristics();
            if (score > bestScore) {
                bestScore = score;
                bestCam = cams.get(i);
            }
        }
        System.out.println("Лучшая камера: " + bestCam.getModel());
    }

    public static void printCameras(ArrayList<WebCamera> cams) {
        if (cams.isEmpty()) {
            System.out.println("Список камер пуст.");
            return;
        }
        
        for (int i = 0; i < cams.size(); i++) {
            WebCamera cam = cams.get(i);
            System.out.println("Модель: " + cam.getModel() + ", Ширина экрана: " + cam.getWigth() + " пикселей" + ", FPS: " + cam.getFPS());
        }
    }

    private static double readDouble(Scanner scanner, String msg, double min, double max, String errorMsg) {
        double val;
        do {
            System.out.print(msg);
            while (!scanner.hasNextDouble()) {
                System.out.println("Ошибка! Введите число.");
                scanner.next();
            }
            val = scanner.nextDouble();
            if (val < min || val > max) {
                System.out.println(errorMsg);
            }
        } while (val < min || val > max);
        return val;
    }

    public static void addCamera(ArrayList<WebCamera> cams, Scanner scanner) {
        System.out.print("Введите модель: ");
        String model = scanner.nextLine();
        double width = readDouble(scanner, "Введите ширину разрешения в пикселях: ", 320, 3840, "Минимальное значение для ширины экрана 320 пикселей, а максимальное - 3840");
        double fps = readDouble(scanner, "Введите FPS: ", 1, 120, "Минимальное значение для fps 1, а максимальное - 120");
        scanner.nextLine();
        cams.add(new WebCamera(model, width, fps));
        System.out.println("Камера успешно добавлена!");
    }

    public static void editCamera(ArrayList<WebCamera> cams, Scanner scanner) {
        System.out.print("Введите номер камеры для изменения: ");
        if (!scanner.hasNextInt()) {
            System.out.println("Такого нет или не может быть");
            scanner.nextLine();
            return;
        }
        int index = scanner.nextInt() - 1;
        scanner.nextLine();

        if (index >= 0 && index < cams.size()) {
            WebCamera cam = cams.get(index);
            System.out.print("Введите новую модель (текущая: " + cam.getModel() + "): ");
            cam.setModel(scanner.nextLine());
            cam.setWidth(readDouble(scanner, "Введите новую ширину экрана в пикселях (текущая: " + cam.getWigth() + "): ", 320, 3840, "Минимальное значение для ширины экрана 320, а максимальное - 3840"));
            cam.setFPS(readDouble(scanner, "Введите новый FPS (текущий: " + cam.getFPS() + "): ", 1, 120, "Минимальное значение для fps 1, а максимальное - 120"));
            scanner.nextLine();
            System.out.println("Свойства обновлены!");
        } else {
            System.out.println("Такого нет или не может быть");
        }
    }

    public static void deleteCamera(ArrayList<WebCamera> cams, Scanner scanner) {
    if (cams.isEmpty()) {
        System.out.println("Список камер пуст. Удалять нечего.");
        return;
    }

    System.out.print("Введите номер камеры для удаления: ");
    if (!scanner.hasNextInt()) {
        System.out.println("Такого нет или не может быть");
        scanner.nextLine();
        return;
    }
    int index = scanner.nextInt() - 1;
    scanner.nextLine();

    if (index >= 0 && index < cams.size()) {
        cams.remove(index);
        System.out.println("Камера удалена!");
    } else {
        System.out.println("Такого нет или не может быть");
    }
}
}
