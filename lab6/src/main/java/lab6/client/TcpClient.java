package lab6.client;

import lab6.net.NetworkSettings;
import lab6.net.TcpProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpClient {
    private final InetSocketAddress address;

    public TcpClient(InetSocketAddress address) {
        this.address = address;
    }

    public String request(String message) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(address, TcpProtocol.TIMEOUT_MS);
            socket.setSoTimeout(TcpProtocol.TIMEOUT_MS);
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            TcpProtocol.writeMessage(output, message);
            return TcpProtocol.readMessage(input);
        }
    }

    public static void main(String[] args) {
        try {
            TcpClient client = new TcpClient(NetworkSettings.loadAddress());
            String response = client.request(TcpProtocol.PING);
            if (!TcpProtocol.PONG.equals(response)) {
                throw new IOException("Неожиданный ответ сервера: " + response);
            }
            System.out.println(response);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Ошибка клиента: " + e.getMessage());
            System.exit(1);
        }
    }
}
