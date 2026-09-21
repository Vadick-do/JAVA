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
                case 1:
                    //printCameras(cams);
                    break;
                case 2:
                    //addCamera(cams, scanner);
                    break;
                case 3:
                    //editCamera(cams, scanner);
                    break;
                case 4:
                    //deleteCamera(cams, scanner);
                    break;
                case 5:
                    //showBestCamera(cams);
                    break;
                case 0:
                    System.out.println("Выход из программы.");
                    isRunning = false;
                    break;
                default:
                    System.out.println("Такого в меню нет!");
            }
        }
    
        scanner.close();
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
