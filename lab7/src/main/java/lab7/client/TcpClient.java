package lab7.client;

import lab7.client.view.ConsoleView;
import lab7.model.Camera;
import lab7.model.FileReadResult;
import lab7.net.CameraRequest;
import lab7.net.CameraResponse;
import lab7.net.LogResult;
import lab7.net.NetworkSettings;
import lab7.net.TcpProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class TcpClient {
    private final InetSocketAddress address;

    public TcpClient(InetSocketAddress address) {
        this.address = address;
    }

    public CameraResponse request(CameraRequest request) throws IOException {
        byte[] message = TcpProtocol.serialize(request);
        try (Socket socket = new Socket()) {
            socket.connect(address, TcpProtocol.TIMEOUT_MS);
            socket.setSoTimeout(TcpProtocol.TIMEOUT_MS);
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            TcpProtocol.writeMessage(output, message);
            return TcpProtocol.readMessage(input, CameraResponse.class);
        }
    }

    public static void main(String[] args) {
        try {
            TcpClient client = new TcpClient(NetworkSettings.loadAddress());
            CameraRequest request = args.length == 0 ? new CameraRequest(TcpProtocol.PING)
                    : new CameraRequest(args[0], Arrays.copyOfRange(args, 1, args.length));
            Serializable response = client.request(request).valueAs(Serializable.class);
            if (TcpProtocol.PING.equals(request.command()) && !TcpProtocol.PONG.equals(response)) {
                throw new IOException("Неожиданный ответ сервера: " + response);
            }
            printResponse(response);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Ошибка клиента: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printResponse(Serializable response) throws IOException {
        if (response instanceof String status) {
            System.out.println(status);
            return;
        }

        System.out.println(TcpProtocol.OK);
        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleView view = new ConsoleView(scanner);
            if (response instanceof Camera[] cameras) {
                view.printCameras(List.of(cameras));
            } else if (response instanceof Camera camera) {
                view.printCameras(List.of(camera));
            } else if (response instanceof FileReadResult report) {
                view.printCameras(report.getCams());
                view.printReport(report);
            } else if (response instanceof LogResult log) {
                view.showLogResult(log);
            } else {
                throw new IOException("Некорректный ответ сервера.");
            }
        }
    }
}
