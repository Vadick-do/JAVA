package lab2;

import java.util.ArrayList;
import java.util.Scanner;

public class lab2 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<WebCamera> cams = new ArrayList<>();
        
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

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 0:
                    System.out.println("Выход из программы.");
                    isRunning = false;
                    break;
                case 1:
                    printCameras(cams);
                    break;
                case 2:
                    addCamera(cams, scanner);
                    break;
                case 3:
                    editCamera(cams, scanner);
                    break;
                case 4:
                    deleteCamera(cams, scanner);
                    break;
                case 5:
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
        int bestScore = bestCam.countCharacteristics();
        for (int i = 1; i < cams.size(); i++) {
            int score = cams.get(i).countCharacteristics();
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
            System.out.println("Модель: " + cam.getModel() + ", Ширина: " + cam.getWigth() + ", FPS: " + cam.getFPS());
        }
    }

    public static void addCamera(ArrayList<WebCamera> cams, Scanner scanner) {
        System.out.print("Введите модель: ");
        String model = scanner.nextLine();
        System.out.print("Введите ширину разрешения: ");
        int width = scanner.nextInt();
        System.out.print("Введите FPS: ");
        int fps = scanner.nextInt();
        cams.add(new WebCamera(model, width, fps));
        System.out.println("Камера успешно добавлена!");
    }

    public static void editCamera(ArrayList<WebCamera> cams, Scanner scanner) {
        System.out.print("Введите номер камеры для изменения: ");
        int index = scanner.nextInt() - 1;
        if (index >= 0 && index < cams.size()) {
            scanner.nextLine(); 
            WebCamera cam = cams.get(index);
            System.out.print("Введите новую модель (текущая: " + cam.getModel() + "): ");
            cam.setModel(scanner.nextLine());
            System.out.print("Введите новую ширину (текущая: " + cam.getWigth() + "): ");
            cam.setWidth(scanner.nextInt());
            System.out.print("Введите новый FPS (текущий: " + cam.getFPS() + "): ");
            cam.setFPS(scanner.nextInt());
            System.out.println("Свойства обновлены!");
        } else {
            System.out.println("Такого нет или не может быть");
        }
    }

    public static void deleteCamera(ArrayList<WebCamera> cams, Scanner scanner) {
        System.out.print("Введите номер камеры для удаления: ");
        int index = scanner.nextInt() - 1;
        if (index >= 0 && index < cams.size()) {
            cams.remove(index);
            System.out.println("Камера удалена!");
        } else {
            System.out.println("Такого нет или не может быть");
        }
    }
}
