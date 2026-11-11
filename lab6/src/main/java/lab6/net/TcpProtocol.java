package lab6.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TcpProtocol {
    public static final String PING = "PING";
    public static final String LIST = "LIST";
    public static final String GET = "GET";
    public static final String READ = "READ";
    public static final String LOAD = "LOAD";
    public static final String REPLACE = "REPLACE";
    public static final String ADD = "ADD";
    public static final String EDIT = "EDIT";
    public static final String DELETE = "DELETE";
    public static final String PHOTO = "PHOTO";
    public static final String VIDEO = "VIDEO";
    public static final String ZOOM = "ZOOM";
    public static final String MODE = "MODE";
    public static final String RANDOMIZE = "RANDOMIZE";
    public static final String TOGGLE = "TOGGLE";
    public static final String RESET = "RESET";
    public static final String DEFAULT_FILE = "devices.txt";
    public static final String OK = "OK";
    public static final String ERROR = "ERROR ";
    public static final String PONG = OK + " PONG";
    public static final String UNKNOWN_COMMAND = ERROR + "Неизвестная команда.";
    public static final int TIMEOUT_MS = 5000;
    public static final int MAX_MESSAGE_BYTES = 1024 * 1024;

    public static String readMessage(DataInputStream input) throws IOException {
        int length = input.readInt();
        checkLength(length);
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }

    public static void writeMessage(DataOutputStream output, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        checkLength(bytes.length);
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    private static void checkLength(int length) throws IOException {
        if (length < 1 || length > MAX_MESSAGE_BYTES) {
            throw new IOException("Недопустимая длина сообщения: " + length);
        }
    }
}
