package lab5;

import java.util.Scanner;

public class lab5 {

    public static void main(String[] args) {
        int exitCode;
        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleView view = new ConsoleView(scanner);
            CameraController controller = new CameraController(view);
            exitCode = controller.run(args);
        }
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
