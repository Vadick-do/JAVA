package lab6;

import lab6.client.controller.CameraController;
import lab6.client.view.ConsoleView;
import lab6.server.CameraServer;

import java.util.Scanner;

public class lab6 {

    public static void main(String[] args) {
        int exitCode;
        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleView view = new ConsoleView(scanner);
            CameraServer server = new CameraServer();
            CameraController controller = new CameraController(view, server);
            exitCode = controller.run(args);
        }
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
