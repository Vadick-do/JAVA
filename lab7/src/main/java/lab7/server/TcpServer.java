package lab7.server;

import lab7.net.CameraRequest;
import lab7.net.NetworkSettings;
import lab7.net.TcpProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class TcpServer {
    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket()) {
            server.bind(NetworkSettings.loadAddress());
            Path dataDirectory = Files.isDirectory(Path.of("lab7")) ? Path.of("lab7") : Path.of(".");
            CameraRequests requests = new CameraRequests(dataDirectory);
            try {
                requests.loadDefaultFile();
            } catch (IOException | IllegalArgumentException e) {
                System.err.println("Ошибка загрузки данных: " + e.getMessage());
            }
            System.out.println("Сервер запущен: " + server.getLocalSocketAddress());

            while (true) {
                Socket socket = server.accept();
                Thread worker = new Thread(() -> handleClient(socket, requests));
                worker.start();
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void handleClient(Socket socket, CameraRequests requests) {
        try (socket) {
            handleRequest(socket, requests);
        } catch (EOFException e) {
            System.err.println("Клиент отключился до конца сообщения.");
        } catch (IOException e) {
            System.err.println("Ошибка обмена с клиентом: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket socket, CameraRequests requests) throws IOException {
        socket.setSoTimeout(TcpProtocol.TIMEOUT_MS);
        DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        CameraRequest request = TcpProtocol.readMessage(input, CameraRequest.class);
        TcpProtocol.writeMessage(output, requests.handle(request));
    }
}
