package lab7;

import lab7.client.CameraClient;
import lab7.client.controller.CameraController;
import lab7.client.view.ConsoleView;

import java.util.Scanner;

public class lab7 {

    public static void main(String[] args) {
        int exitCode;
        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleView view = new ConsoleView(scanner);
            CameraController controller = new CameraController(view, new CameraClient());
            exitCode = controller.run(args);
        }
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
