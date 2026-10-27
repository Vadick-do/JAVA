package lab5;

import java.util.Scanner;

public class lab5 {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleView view = new ConsoleView(scanner);
            CameraController controller = new CameraController(view);
            controller.run();
        }
    }
}
