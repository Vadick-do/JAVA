package lab6.server;

import lab6.net.NetworkSettings;
import lab6.net.TcpProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer {
    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket()) {
            server.bind(NetworkSettings.loadAddress());
            System.out.println("Сервер запущен: " + server.getLocalSocketAddress());

            while (true) {
                Socket socket = server.accept();
                try (socket) {
                    handleRequest(socket);
                } catch (EOFException e) {
                    System.err.println("Клиент отключился до конца сообщения.");
                } catch (IOException e) {
                    System.err.println("Ошибка обмена с клиентом: " + e.getMessage());
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void handleRequest(Socket socket) throws IOException {
        socket.setSoTimeout(TcpProtocol.TIMEOUT_MS);
        DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        String request = TcpProtocol.readMessage(input);
        String response = TcpProtocol.PING.equals(request) ? TcpProtocol.PONG : TcpProtocol.UNKNOWN_COMMAND;
        TcpProtocol.writeMessage(output, response);
    }
}
